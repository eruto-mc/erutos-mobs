package net.erutobusiness.erutosmobs.entity;

import net.minecraft.util.Mth;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.level.Level;

import java.util.EnumSet;

/**
 * 波を切る — 英名 shearwater（水を切る鳥）の由来の飛び方。
 * 水面すれすれへ降りて、片側へ傾いたまま円を描いて滑り、下がった翼端で水面を切る。
 * しぶきはクライアントの {@link ShearwaterEffects} が、翼端が水面に触れたかで出す（ここは飛び方だけ）。
 *
 * 円は鳥の横に中心を取り、半径 11〜15。円の上を 0.45 rad 先の点へ向かい続けると円に乗る。
 * 高さは足元を水面の 0.15 上へ寄せる。傾いたときの下の翼端は足元の 0.41 下（右へ傾く）・0.05 上（左へ傾く）
 * なので（{@link ShearwaterLocators} の BANK_*）、翼端だけが水に入り、胴（足元の 0.67 上から）は濡れない。
 * 速さは 0.3 ブロック/tick。ふだんの滑空（0.27）・羽ばたき（0.22）より速い——実物もこの飛び方のときがいちばん速い。
 */
class ShearPassGoal extends Goal {
    private static final double SPEED = 0.30;
    private static final double LEAD = 0.45;
    private static final double SKIM = 0.15;

    private final ShearwaterEntity bird;
    private double cx;
    private double cz;
    private double radius;
    private int turn;
    private int time;
    private int duration;
    private double surface;

    ShearPassGoal(ShearwaterEntity bird) {
        this.bird = bird;
        this.setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        int st = this.bird.getState();
        if ((st != ShearwaterEntity.FLY && st != ShearwaterEntity.GLIDE) || this.bird.shearCooldown > 0) {
            return false;
        }
        return plan();
    }

    /** 左右どちらかへ円を取り、円の上 8 点がどれも同じ高さの水面なら採る。 */
    private boolean plan() {
        Level level = this.bird.level();
        double here = ShearwaterEntity.seaSurfaceAt(level, this.bird.getBlockX(), this.bird.getBlockZ());
        if (Double.isNaN(here) || this.bird.getY() - here > 12.0) {
            this.bird.shearCooldown = 200;
            return false;
        }
        float yaw = this.bird.getYRot() * Mth.DEG_TO_RAD;
        double r = 11.0 + this.bird.getRandom().nextDouble() * 4.0;
        int first = this.bird.getRandom().nextBoolean() ? 1 : -1;
        for (int k = 0; k < 2; k++) {
            int t = k == 0 ? first : -first;
            // 右へ曲がる（t = +1）なら中心は鳥の右。鳥の右は (−cos yaw, −sin yaw)、左は (cos yaw, sin yaw)
            double x = this.bird.getX() - t * r * Mth.cos(yaw);
            double z = this.bird.getZ() - t * r * Mth.sin(yaw);
            if (circleOverSea(level, x, z, r, here)) {
                this.cx = x;
                this.cz = z;
                this.radius = r;
                this.turn = t;
                this.surface = here;
                return true;
            }
        }
        this.bird.shearCooldown = 200;
        return false;
    }

    private static boolean circleOverSea(Level level, double x, double z, double r, double surface) {
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4.0;
            double s = ShearwaterEntity.seaSurfaceAt(level, Mth.floor(x + r * Math.cos(a)), Mth.floor(z + r * Math.sin(a)));
            if (Double.isNaN(s) || Math.abs(s - surface) > 0.5) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void start() {
        this.time = 0;
        this.duration = 140 + this.bird.getRandom().nextInt(81);
        this.bird.getNavigation().stop();
        this.bird.holdGlide(this.duration);
    }

    @Override
    public boolean canContinueToUse() {
        return this.time < this.duration && this.bird.getState() == ShearwaterEntity.GLIDE && this.bird.isAlive()
                && !this.bird.horizontalCollision;
    }

    @Override
    public void stop() {
        this.bird.shearCooldown = 900 + this.bird.getRandom().nextInt(901);
        this.bird.holdGlide(0);
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        this.time++;
        double px = this.bird.getX() - this.cx;
        double pz = this.bird.getZ() - this.cz;
        double phi = Math.atan2(pz, px) + this.turn * LEAD;
        double tx = this.cx + this.radius * Math.cos(phi);
        double tz = this.cz + this.radius * Math.sin(phi);
        // 高さ: 足元を水面の少し上へ。最後の 1 秒は上がり始める（速さを高さに換えて抜ける）
        double surfaceHere = ShearwaterEntity.seaSurfaceAt(this.bird.level(), this.bird.getBlockX(), this.bird.getBlockZ());
        if (!Double.isNaN(surfaceHere)) {
            this.surface = surfaceHere;
        }
        double wantY = this.surface - 0.11 + SKIM;       // 水源の水面はブロックの上面より 1/9 低い
        if (this.duration - this.time < 20) {
            wantY += 3.0;
        }
        // 飛び方そのもの（速さ・曲がり・上下）は ShearwaterMoveControl に任せ、ここは円の上の行き先だけを渡す
        this.bird.getMoveControl().setWantedPosition(tx, wantY, tz, SPEED / ShearwaterMoveControl.CRUISE_GLIDE);
        this.bird.holdGlide(this.duration - this.time + 1);
    }
}
