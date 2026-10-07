package net.erutobusiness.erutosmobs.entity;

import net.erutobusiness.erutosmobs.advancement.ShearwaterTrigger;
import net.erutobusiness.erutosmobs.registry.ModEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.EnumSet;

/**
 * 船に付いて飛ぶ（2026-09-29）。実物のミズナギドリは船、とくに漁船について飛ぶ（捨てられた魚を拾う）。
 *
 * 海でボートに乗った人が 48 ブロック以内にいると、しばらく（45〜90 秒）船の横に並んで滑空する。
 *   並ぶ所  … 船の左か右 11〜15 ブロック（いまいる側。5 秒ごとに付け直す）・船の 0〜3.5 前・水面の 2.5〜5.5 上。
 *              ゆっくり波打つように位置を変える
 *   寄せ方  … 横のずれは、並ぶ線の上で鳥の 10 ブロック先の点を狙って浅い角度で寄せる。前後のずれは速さで詰める
 *              （遅れていれば速く、出すぎていれば遅く）。船が遅くて 14 ブロックより前へ出たら、外側へ回って引き返す
 *   波を切る … 12〜20 秒ごとに 3〜4 秒、水面すれすれへ降りて船の横を蛇行する。蛇行で左右へ傾くので、
 *              下がった翼端が波を切り、しぶきと泡の線が船の横に残る（{@link ShearwaterEffects}）
 *   回る    … 船がほぼ止まっていたら、船のまわりを半径 12 で、傾いたまま回る（翼端がずっと波を切る。{@link ShearPassGoal} と同じ回り方）
 * ふつうは見つけるたび（1 秒ごと）に 1/8 で付く。魚を食べさせた人（{@link FishLureGoal}）の船には、64 ブロック先からでも
 * 必ず付き、長く（最長 2 分）、近く（9〜13）並ぶ。
 * 並ぶ間、その人に嵐渡り（{@link net.erutobusiness.erutosmobs.effect.StormCrossingEffect}）を分ける。
 * 信用している人の船に終わりまで並べたら、風切羽を 1 枚くれる（{@link ShearwaterEntity#giveFeather}）。
 * 終わったら 2〜4 分（信用している人なら 1〜2 分）は付いてこない。船が陸へ上がる・人が降りる・64 より離れる・
 * その人に殴られる、でもやめる。嵐の怒りを買っている人の船には付かない。
 * ⚠ 人が漕ぐ船は、動きを漕ぐ人の画面の側が決めてサーバへ位置を送る。サーバの船の速度は当てにならないので、
 *   位置の差から速さを出す。
 */
class FollowBoatGoal extends Goal {
    private static final double SEARCH = 48.0;
    /** 信用している人（魚をくれた人）の船は、もっと遠くから見つける */
    private static final double TRUSTED_SEARCH = 64.0;
    private static final double GIVE_UP = 64.0;
    /** 横のずれを寄せるときに狙う、鳥の前の距離（波を切る間は蛇行を鋭くするため短く） */
    private static final double LOOK = 10.0;
    private static final double SKIM_LOOK = 6.0;
    private static final double ORBIT_RADIUS = 12.0;
    private static final double ORBIT_LEAD = 0.45;
    private static final double SKIM = 0.15;
    private static final double SHEAR_SPEED = 0.30;

    private final ShearwaterEntity bird;
    @Nullable
    private Player player;
    private boolean trusted;
    private int side;
    private int time;
    private int duration;
    private int nextSkim;
    private int skimLeft;
    private int stillTicks;
    private int orbitTurn;
    private boolean turningBack;
    private int searchDelay;
    @Nullable
    private Vec3 lastBoatPos;
    private Vec3 boatVel = Vec3.ZERO;

    FollowBoatGoal(ShearwaterEntity bird) {
        this.bird = bird;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    private static boolean flying(int st) {
        return st == ShearwaterEntity.FLY || st == ShearwaterEntity.GLIDE || st == ShearwaterEntity.HOVER;
    }

    private static boolean onSea(Boat boat) {
        double s = ShearwaterEntity.seaSurfaceAt(boat.level(), boat.getBlockX(), boat.getBlockZ());
        return !Double.isNaN(s) && Math.abs(boat.getY() - s) < 1.5;
    }

    private double horizontalDistSqr(Vec3 p) {
        double dx = p.x - this.bird.getX();
        double dz = p.z - this.bird.getZ();
        return dx * dx + dz * dz;
    }

    @Override
    public boolean canUse() {
        if (--this.searchDelay > 0) {
            return false;
        }
        this.searchDelay = 20;
        if (!flying(this.bird.getState()) || this.bird.followCooldown > 0) {
            return false;
        }
        Player best = null;
        double bestD = Double.MAX_VALUE;
        for (Player p : this.bird.level().players()) {
            if (p.isSpectator() || ShearwaterEntity.wrathful(p) || !(p.getVehicle() instanceof Boat boat) || !onSea(boat)) {
                continue;
            }
            double d = horizontalDistSqr(boat.position());
            double reach = this.bird.trusts(p) ? TRUSTED_SEARCH : SEARCH;
            if (d < reach * reach && d < bestD) {
                best = p;
                bestD = d;
            }
        }
        // ⚠ 信用している人の船には必ず付く。1/2 の籤にしていたら、飛び立った直後の 1 回を外したあいだに
        //   海を巡る目標で 50 ブロック先へ行ってしまい、付いてこなかった（2026-10-07 に試した）
        if (best == null || (!this.bird.trusts(best) && this.bird.getRandom().nextInt(8) != 0)) {
            return false;
        }
        this.player = best;
        return true;
    }

    @Override
    public void start() {
        Player p = this.player;
        if (p == null || !(p.getVehicle() instanceof Boat boat)) {
            return;
        }
        this.trusted = this.bird.trusts(p);
        this.time = 0;
        this.duration = this.trusted ? 1800 + this.bird.getRandom().nextInt(601) : 900 + this.bird.getRandom().nextInt(901);
        this.nextSkim = 240 + this.bird.getRandom().nextInt(161);
        this.skimLeft = 0;
        this.stillTicks = 0;
        this.turningBack = false;
        this.lastBoatPos = null;
        this.boatVel = Vec3.ZERO;
        // 寄っていた側に並ぶ（船の右が +1）
        Vec3 fwd = Vec3.directionFromRotation(0.0f, boat.getYRot());
        Vec3 right = new Vec3(-fwd.z, 0.0, fwd.x);
        Vec3 rel = this.bird.position().subtract(boat.position());
        this.side = rel.dot(right) >= 0.0 ? 1 : -1;
        if (this.bird.getState() == ShearwaterEntity.HOVER) {
            this.bird.setState(ShearwaterEntity.FLY);
        }
        this.bird.getNavigation().stop();
    }

    @Override
    public boolean canContinueToUse() {
        Player p = this.player;
        if (p == null || !p.isAlive() || ShearwaterEntity.wrathful(p) || !(p.getVehicle() instanceof Boat boat) || !onSea(boat)) {
            return false;
        }
        if (this.time >= this.duration || !flying(this.bird.getState())
                || horizontalDistSqr(boat.position()) > GIVE_UP * GIVE_UP) {
            return false;
        }
        // その人に殴られたらやめる
        return !(this.bird.getLastHurtByMob() == p && this.bird.tickCount - this.bird.getLastHurtByMobTimestamp() < 200);
    }

    @Override
    public void stop() {
        this.bird.followCooldown = this.trusted ? 1200 + this.bird.getRandom().nextInt(1201)
                : 2400 + this.bird.getRandom().nextInt(2401);
        this.bird.holdGlide(0);
        // 信用している人の船に終わりまで並べたら、風切羽を 1 枚くれる
        Player p = this.player;
        if (this.trusted && this.time >= this.duration && p != null && p.isAlive() && p.getVehicle() instanceof Boat
                && this.bird.trusts(p)) {
            this.bird.giveFeather(p);
        }
        this.player = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        Player p = this.player;
        if (p == null || !(p.getVehicle() instanceof Boat boat)) {
            return;
        }
        this.time++;
        // 並んで飛ぶ間は嵐渡り（雷に打たれない・泳ぎとボートが速い）を分ける。5 秒ごとに 15 秒ぶん足し直す
        if (this.time % 100 == 1) {
            p.addEffect(new MobEffectInstance(ModEffects.STORM_CROSSING.get(), 300, 0, true, true, true));
        }
        if (this.time == 600 && p instanceof ServerPlayer sp) {
            ShearwaterTrigger.INSTANCE.trigger(sp, ShearwaterTrigger.FOLLOWED);
        }
        Level level = this.bird.level();
        // 船の速さ（位置の差をならす）
        Vec3 pos = boat.position();
        Vec3 inst = this.lastBoatPos == null ? Vec3.ZERO : pos.subtract(this.lastBoatPos);
        this.lastBoatPos = pos;
        this.boatVel = this.boatVel.scale(0.8).add(inst.scale(0.2));
        double speed = this.boatVel.horizontalDistance();
        Vec3 fwd = speed > 0.03 ? new Vec3(this.boatVel.x, 0.0, this.boatVel.z).normalize()
                : Vec3.directionFromRotation(0.0f, boat.getYRot());
        Vec3 right = new Vec3(-fwd.z, 0.0, fwd.x);
        double surface = ShearwaterEntity.seaSurfaceAt(level, boat.getBlockX(), boat.getBlockZ());
        if (Double.isNaN(surface)) {
            return;
        }
        double waterY = surface - 0.11;                      // 水源の水面はブロックの上面より 1/9 低い
        this.stillTicks = speed < 0.04 ? this.stillTicks + 1 : 0;

        // 船がほぼ止まっていたら、船のまわりを傾いたまま回る
        if (this.stillTicks > 60) {
            if (this.stillTicks == 61) {
                Vec3 rel = this.bird.position().subtract(pos);
                Vec3 v = this.bird.getDeltaMovement();
                this.orbitTurn = rel.x * v.z - rel.z * v.x >= 0.0 ? 1 : -1;
            }
            double phi = Math.atan2(this.bird.getZ() - pos.z, this.bird.getX() - pos.x) + this.orbitTurn * ORBIT_LEAD;
            this.bird.getMoveControl().setWantedPosition(pos.x + ORBIT_RADIUS * Math.cos(phi), waterY + SKIM,
                    pos.z + ORBIT_RADIUS * Math.sin(phi), SHEAR_SPEED / ShearwaterMoveControl.CRUISE_GLIDE);
            this.bird.holdGlide(20);
            return;
        }

        // ⚠ 並ぶ側は 5 秒ごとに「いまいる側」へ付け直す。始めに決めた側のままだと、船が向きを変えるたびに
        //   反対側の並ぶ所へ回り込もうとして、船の真上を何度も横切った（2026-10-07 に試した。船を小さく回したとき）
        Vec3 rel = this.bird.position().subtract(pos);
        double relF = rel.x * fwd.x + rel.z * fwd.z;
        double relL = rel.x * right.x + rel.z * right.z;
        if (this.time % 100 == 0) {
            this.side = relL >= 0.0 ? 1 : -1;
        }
        // 並ぶ所。信用している人には近く。⚠ 翼の半分が 6 ブロックあるので、蛇行のいちばん近い所でも翼端が船に掛からず、
        //   下の「7 ブロック以内なら上を通る」にも掛からない幅（近い側を 6.5 にしていたら、波切りが毎回打ち切られた）
        double near = this.trusted ? 9.0 : 11.0;
        double lateral;
        double y;
        if (this.skimLeft > 0) {
            // 水面すれすれで船の横を蛇行する（左右へ傾いて翼端が波を切る）
            this.skimLeft--;
            lateral = near + 1.5 + 2.0 * Math.sin(this.time * 0.15);
            y = waterY + SKIM;
            this.bird.holdGlide(this.skimLeft + 1);
            if (this.skimLeft == 0) {
                this.nextSkim = 240 + this.bird.getRandom().nextInt(161);
            }
        } else {
            lateral = near + 2.0 + 2.0 * Math.sin(this.time * 0.04);
            y = surface + 4.0 + 1.5 * Math.sin(this.time * 0.05);
            if (--this.nextSkim <= 0) {
                this.skimLeft = 60 + this.bird.getRandom().nextInt(21);
            }
        }
        // 並ぶ所（船から見て前へ slotF・横へ slotL）
        double slotF = 1.5 + 2.0 * Math.sin(this.time * 0.023);
        double slotL = this.side * lateral;
        double errF = slotF - relF;
        // 狙う点: 並ぶ線の上で、鳥の LOOK 先。横のずれは浅い角度で寄せ、前後のずれは速さで詰める
        // ⚠ 並ぶ所そのものを狙うと、近づくほど向きが大きく振れ、曲がる半径（6）より小さく回れずに
        //   並ぶ所のまわりを回り続けて、船の真上を何度も横切った（2026-10-07 に試した）
        double look = this.skimLeft > 0 ? SKIM_LOOK : LOOK;
        if (this.turningBack ? errF < -4.0 : errF < -14.0) {
            // 船より大きく前へ出た（船が遅い）: 外側へ回って引き返す
            this.turningBack = true;
            look = -LOOK;
            slotL += this.side * 8.0;
        } else {
            this.turningBack = false;
        }
        Vec3 aim = pos.add(fwd.scale(relF + look)).add(right.scale(slotL));
        // 船の 7 ブロック以内に入ったら、水面の 9 上を通る（翼幅 12 の翼が人の頭のすぐ上を通らないように）
        if (rel.horizontalDistance() < 7.0) {
            y = Math.max(y, surface + 9.0);
            this.skimLeft = 0;
        }
        // 船の速さに合わせ、遅れていれば速く、出すぎていれば遅く飛ぶ
        double want = speed + Mth.clamp(errF * 0.03, -0.1, 0.2);
        double cruise = this.bird.getState() == ShearwaterEntity.GLIDE ? ShearwaterMoveControl.CRUISE_GLIDE : ShearwaterMoveControl.CRUISE_FLY;
        double mod = Mth.clamp(want / cruise, 0.6, 2.2);
        this.bird.getMoveControl().setWantedPosition(aim.x, y, aim.z, mod);
    }
}
