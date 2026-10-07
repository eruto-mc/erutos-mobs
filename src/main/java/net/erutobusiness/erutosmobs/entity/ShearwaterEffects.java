package net.erutobusiness.erutosmobs.entity;

import net.erutobusiness.erutosmobs.client.ParticleBudget;
import net.erutobusiness.erutosmobs.client.ShearwaterSounds;
import net.erutobusiness.erutosmobs.registry.ModParticles;
import net.erutobusiness.erutosmobs.registry.ModSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

/**
 * クライアントで出す粒子と音（2026-09-28）。サーバは状態だけを持ち、見た目はここで各自が作る
 * （タイヨウチョウも羽根はクライアント側で毎 tick 出している）。
 *
 * ⚠⚠ 大きさは「実物 × 11」（この鳥は翼幅 12 ブロックで、実物 1.1 m の約 11 倍。1 ブロック ＝ 1 m）。
 *   最初はバニラの粒子（幅 0.2〜0.4）をそのまま出していて、翼幅の 2〜3% の粉にしかならなかった
 *   （2026-09-28・ユーザー「パーティクルが体に対して小さすぎる」）。
 * ⚠⚠ 出どころは描いた翼の骨から取る（{@code ShearwaterRenderer} が毎コマ写す。描かれていない間だけ、キットが計算した
 *   滑空の姿勢の点）。前は滑空の姿勢の決まった点だけで、羽ばたいている間は火花が翼から離れた空中に出ていた。
 *
 * 出すもの（設計は README の「光・粒子・雷」）:
 *   風の筋     … 速く滑空しているとき、水面から 1 ブロックより上にある翼端から。前の tick の翼端から今の翼端まで並べる
 *   波を切る   … 翼端が水面に触れたら、2〜3 ブロック噴き上がるしぶき（実物 20〜30 cm × 11）と、水面に残る泡の線。
 *                ⚠ 水に触れている翼端からは風の筋を出さない（2026-09-28・ユーザー「風の筋か波切のしぶきか区別がつかない」。
 *                どちらも白い点で、同じ翼端から重なって出ていた）
 *   嵐の火花   … 帯電中は、後縁の稲妻（1 tick に 1〜2 本）・翼端から翼の外の空へ 1.5〜2.5 ブロック走る放電（3 tick に 1 本）・
 *                前縁を肩から翼端まで走る長い稲妻（10 tick に 1 本）。雷雨だけなら後縁の稲妻をときどき。
 *                どれも白い芯の帯を折れ線につなぎ、折れ目に光の点を置く（2〜4 tick で消え、次の tick に別の所へ出る）
 *   足の水しぶき … 水面からの離陸で、足が水を蹴る拍に（実物 20 cm × 11 → 幅 2 ブロックほど）
 *   水滴       … 水から飛び立った後の 5 秒、翼の後縁から落ちる。雨の中を飛ぶ間も、少なめに落ちる
 * 粒子の量は各自の設定 `particleAmount` とゲームの「パーティクル」の設定で間引く（{@link ParticleBudget}）。
 *   航跡       … 水面を漕いで進むとき、胴の後ろへ八の字に開く泡（実物の幅 0.5〜1 m × 11 → 6〜8 ブロック）
 *   着水       … 急降下から水面に入った瞬間の大きなしぶきと泡の輪（半径 1〜3.5 ブロック）
 * 鳴らす音（2026-10-08。音は手元の音の道具 wavs が作る）:
 *   波を切る水の音（翼端が水に触れている間 0.7 秒おき）・帯電のパチパチ（帯電中 0.6〜1.6 秒おき）・
 *   近くを飛び過ぎる風切り（{@link ShearwaterSounds#flyby}。この画面の人を見て決める）
 */
final class ShearwaterEffects {
    private final ShearwaterEntity bird;
    private int lastState = -1;
    private int stateTicks;
    private int wetTicks;
    /** この tick に粒子を出す割合（0〜1。{@link ParticleBudget}） */
    private float budget = 1.0f;
    private int shearSoundCooldown;
    private int crackleCooldown;
    private int flybyCooldown;
    private boolean takeoffFromWater;
    /** 前の tick の翼端（世界の座標）。風の筋と泡の線を途切れさせないため。飛んでいない間は null */
    private double[] lastTipL;
    private double[] lastTipR;

    ShearwaterEffects(ShearwaterEntity bird) {
        this.bird = bird;
    }

    void tick() {
        Level level = this.bird.level();
        RandomSource r = this.bird.getRandom();
        this.budget = ParticleBudget.amount();
        int st = this.bird.getState();
        if (st != this.lastState) {
            onChange(level, r, this.lastState, st);
            this.lastState = st;
            this.stateTicks = 0;
        } else {
            this.stateTicks++;
        }
        if (this.shearSoundCooldown > 0) {
            this.shearSoundCooldown--;
        }
        // 近くを飛び過ぎる風切り（聞き手を見て決めるので画面の側のクラスで。鳴らしたら 3 秒は鳴らさない）
        if (this.flybyCooldown > 0) {
            this.flybyCooldown--;
        } else if ((st == ShearwaterEntity.FLY || st == ShearwaterEntity.GLIDE) && ShearwaterSounds.flyby(this.bird)) {
            this.flybyCooldown = 60;
        }
        float yaw = this.bird.yBodyRot;
        double dx = this.bird.getX() - this.bird.xo;
        double dz = this.bird.getZ() - this.bird.zo;
        double speed = Math.sqrt(dx * dx + dz * dz);
        int bank = this.bird.getBank();

        // 翼端: 水に触れていれば波を切り、離れていて速く滑空していれば風の筋
        if (st == ShearwaterEntity.FLY || st == ShearwaterEntity.GLIDE || st == ShearwaterEntity.HOVER) {
            double[] tipL = tip(0, bank, yaw);
            double[] tipR = tip(1, bank, yaw);
            wingTip(level, r, st, speed, dx, dz, tipL, this.lastTipL);
            wingTip(level, r, st, speed, dx, dz, tipR, this.lastTipR);
            this.lastTipL = tipL;
            this.lastTipR = tipR;
        } else {
            this.lastTipL = null;
            this.lastTipR = null;
        }

        // 嵐の火花（間引くときは稲妻 1 本ずつ。帯を途中で欠けさせない）
        if (this.bird.isCharged()) {
            int n = 1 + r.nextInt(2);
            for (int i = 0; i < n; i++) {
                if (roll()) {
                    edgeArc(level, r, yaw);
                }
            }
            if (r.nextInt(3) == 0 && roll()) {
                discharge(level, r, yaw);
            }
            if (r.nextInt(10) == 0 && roll()) {
                leadingArc(level, r, yaw);
            }
            // 帯電のパチパチ（0.8 秒の音を 0.6〜1.6 秒おきに）
            if (--this.crackleCooldown <= 0) {
                level.playLocalSound(this.bird.getX(), this.bird.getY() + 1.0, this.bird.getZ(), ModSounds.SHEARWATER_CRACKLE.get(),
                        SoundSource.NEUTRAL, 1.0f, 0.85f + r.nextFloat() * 0.35f, false);
                this.crackleCooldown = 12 + r.nextInt(20);
            }
        } else if (level.isThundering() && r.nextInt(12) == 0 && roll()) {
            edgeArc(level, r, yaw);
        }

        // 雨の滴: 雨の中を飛ぶ間、翼の後縁から細い滴が落ち、ときどき大きな滴が混ざる（飛び立った後の水滴より少なめ）
        if ((st == ShearwaterEntity.FLY || st == ShearwaterEntity.GLIDE || st == ShearwaterEntity.HOVER) && this.wetTicks == 0
                && level.isRainingAt(BlockPos.containing(this.bird.getX(), this.bird.getY() + 2.0, this.bird.getZ()))) {
            for (int i = 0; i < 2; i++) {
                double[] w = edge(r.nextInt(2), r.nextInt(ShearwaterLocators.EDGE_PER_SIDE), yaw);
                add(level, ParticleTypes.FALLING_WATER, w[0], w[1] - 0.05, w[2], 0.0, 0.0, 0.0);
            }
            if (r.nextInt(8) == 0) {
                double[] w = edge(r.nextInt(2), r.nextInt(ShearwaterLocators.EDGE_PER_SIDE), yaw);
                add(level, ModParticles.SPRAY.get(), w[0], w[1] - 0.1, w[2], 0.0, -0.05, 0.0);
            }
        }

        // 足の水しぶき（離陸の走り。動きは 2 秒で 3 周＝6 歩 → 約 6.7 tick ごと）
        if (st == ShearwaterEntity.TAKEOFF && this.takeoffFromWater && this.stateTicks < 26 && this.stateTicks % 7 == 3) {
            double[] foot = at((this.stateTicks / 7) % 2 == 0 ? ShearwaterLocators.FOOT_L : ShearwaterLocators.FOOT_R, yaw);
            double surface = surfaceNear(level, foot);
            if (!Double.isNaN(surface)) {
                burst(level, r, foot[0], surface, foot[2], 4, 12, 1.2, 0.26);
                level.playLocalSound(foot[0], surface, foot[2], SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL,
                        0.7f, 0.9f + r.nextFloat() * 0.2f, false);
            }
        }

        // 水滴（水から飛び立った後）。細い滴に、ときどき落ちる大きな塊を混ぜる
        if (this.wetTicks > 0) {
            this.wetTicks--;
            for (int i = 0; i < 4; i++) {
                double[] w = edge(r.nextInt(2), r.nextInt(ShearwaterLocators.EDGE_PER_SIDE), yaw);
                add(level, ParticleTypes.FALLING_WATER, w[0], w[1] - 0.05, w[2], 0.0, 0.0, 0.0);
            }
            if (r.nextInt(4) == 0) {
                double[] w = edge(r.nextInt(2), r.nextInt(ShearwaterLocators.EDGE_PER_SIDE), yaw);
                add(level, ModParticles.SPRAY.get(), w[0], w[1] - 0.1, w[2], 0.0, -0.05, 0.0);
            }
        }

        // 航跡（水面を漕いで進む）: 胴の後ろへ八の字に開く 2 本の泡の線
        if (st == ShearwaterEntity.PADDLE && speed > 0.01 && this.stateTicks % 3 == 0) {
            for (int side = -1; side <= 1; side += 2) {
                double back = 1.5 + r.nextDouble() * 4.0;
                double[] p = at(new double[] {side * (0.8 + back * 0.55), 0.3, -back}, yaw);
                double surface = surfaceNear(level, p);
                if (!Double.isNaN(surface)) {
                    add(level, ModParticles.FOAM.get(), p[0], surface + 0.02, p[2], 0.0, 0.0, 0.0);
                }
            }
            if (speed > 0.04 && r.nextInt(2) == 0) {
                double[] bow = at(new double[] {0.0, 0.3, 2.2}, yaw);
                double surface = surfaceNear(level, bow);
                if (!Double.isNaN(surface)) {
                    add(level, ModParticles.SPRAY.get(), bow[0], surface + 0.05, bow[2],
                            (r.nextDouble() - 0.5) * 0.1, 0.2, (r.nextDouble() - 0.5) * 0.1);
                }
            }
        }
    }

    private void onChange(Level level, RandomSource r, int prev, int now) {
        if (prev == ShearwaterEntity.DIVE && now == ShearwaterEntity.PADDLE) {
            // 着水の大きなしぶき（実物 1 m 近い水柱 × 11 は大きすぎるので、半径 1〜3.5・高さ 1〜2.5 ほどに抑える）と泡の輪
            double[] c = at(ShearwaterLocators.BODY_CENTER, this.bird.yBodyRot);
            double surface = surfaceNear(level, c);
            if (!Double.isNaN(surface)) {
                for (int i = 0; i < 24; i++) {
                    double a = r.nextDouble() * Math.PI * 2.0;
                    double rad = 1.0 + r.nextDouble() * 2.5;
                    add(level, ModParticles.SPRAY.get(), c[0] + Math.cos(a) * rad, surface + 0.1,
                            c[2] + Math.sin(a) * rad, Math.cos(a) * 0.12, 0.3 + r.nextDouble() * 0.2,
                            Math.sin(a) * 0.12);
                }
                for (int i = 0; i < 14; i++) {
                    double a = i / 14.0 * Math.PI * 2.0;
                    double rad = 2.5 + r.nextDouble() * 1.0;
                    add(level, ModParticles.FOAM.get(), c[0] + Math.cos(a) * rad, surface + 0.02,
                            c[2] + Math.sin(a) * rad, 0.0, 0.0, 0.0);
                }
                burst(level, r, c[0], surface, c[2], 0, 50, 6.0, 0.3);
                for (int i = 0; i < 20; i++) {
                    add(level, ParticleTypes.BUBBLE, c[0] + (r.nextDouble() - 0.5) * 5.0, surface - 0.6,
                            c[2] + (r.nextDouble() - 0.5) * 5.0, 0.0, 0.05, 0.0);
                }
                level.playLocalSound(c[0], surface, c[2], SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL,
                        1.4f, 0.7f + r.nextFloat() * 0.15f, false);
            }
        }
        if (now == ShearwaterEntity.TAKEOFF) {
            this.takeoffFromWater = prev == ShearwaterEntity.PADDLE || prev == ShearwaterEntity.SLEEP;
            if (this.takeoffFromWater) {
                this.wetTicks = 140;          // 離陸 2 秒＋飛び立った後の 5 秒
            }
        }
    }

    /** 翼端 1 つ。水面から 0.25 以内なら波を切り、1 ブロックより上で速く滑空していれば風の筋 */
    private void wingTip(Level level, RandomSource r, int st, double speed, double dx, double dz, double[] tip,
                         @Nullable double[] last) {
        double surface = surfaceNear(level, tip);
        if (!Double.isNaN(surface) && tip[1] <= surface + 0.25) {
            shear(level, r, dx, dz, tip, last, surface);
        } else if (st == ShearwaterEntity.GLIDE && speed > 0.12 && (Double.isNaN(surface) || tip[1] > surface + 1.0)) {
            trail(level, last, tip);
        }
    }

    /**
     * 波を切る: 翼の外へ 2〜3 ブロック噴き上がるしぶき 5 つ・細かな滴 4 つと、水面に寝かせた泡を前の翼端から今の翼端まで 3 つ
     * （切った線が水面に 3〜4 秒残る）。しぶきの上へ 0.48〜0.60 は、落ちる速さと空気の抵抗（{@code SprayParticle}）で
     * 2.0〜3.1 ブロックの高さになる
     */
    private void shear(Level level, RandomSource r, double dx, double dz, double[] tip, @Nullable double[] last,
                       double surface) {
        double ox = tip[0] - this.bird.getX();
        double oz = tip[2] - this.bird.getZ();
        double on = Math.max(Math.sqrt(ox * ox + oz * oz), 1.0E-3);
        for (int i = 0; i < 5; i++) {
            double out = 0.04 + r.nextDouble() * 0.10;
            add(level, ModParticles.SPRAY.get(), tip[0] + (r.nextDouble() - 0.5) * 0.6, surface + 0.1,
                    tip[2] + (r.nextDouble() - 0.5) * 0.6, ox / on * out + dx * 0.15, 0.48 + r.nextDouble() * 0.12,
                    oz / on * out + dz * 0.15);
        }
        for (int i = 0; i < 4; i++) {
            add(level, ParticleTypes.SPLASH, tip[0] + (r.nextDouble() - 0.5) * 1.5, surface + 0.05,
                    tip[2] + (r.nextDouble() - 0.5) * 1.5, 0.0, 0.2, 0.0);
        }
        double[] from = last != null ? last : tip;
        for (int k = 1; k <= 3; k++) {
            double u = k / 3.0;
            add(level, ModParticles.FOAM.get(), from[0] + (tip[0] - from[0]) * u + (r.nextDouble() - 0.5) * 0.3,
                    surface + 0.02, from[2] + (tip[2] - from[2]) * u + (r.nextDouble() - 0.5) * 0.3, 0.0, 0.0, 0.0);
        }
        // 波を切る水の音（1 秒の音を 0.7 秒おき。両の翼端が水に触れても重ねない）。前はバニラの水しぶきの音だった
        if (this.shearSoundCooldown == 0) {
            level.playLocalSound(tip[0], surface, tip[2], ModSounds.SHEARWATER_SHEAR.get(), SoundSource.NEUTRAL,
                    0.9f, 0.9f + r.nextFloat() * 0.25f, false);
            this.shearSoundCooldown = 14;
        }
    }

    /** 粒子 1 つ。各自の設定とゲームの「パーティクル」の設定で間引く（{@link ParticleBudget}） */
    private void add(Level level, ParticleOptions p, double x, double y, double z, double vx, double vy, double vz) {
        if (roll()) {
            level.addParticle(p, x, y, z, vx, vy, vz);
        }
    }

    /** 出すか（間引かない設定なら必ず出す） */
    private boolean roll() {
        return this.budget >= 1.0f || this.bird.getRandom().nextFloat() < this.budget;
    }

    /** 風の筋: from から to まで 3 つ並べる（from が無ければ to に 1 つ） */
    private void trail(Level level, @Nullable double[] from, double[] to) {
        if (from == null) {
            add(level, ModParticles.MIST.get(), to[0], to[1], to[2], 0.0, 0.0, 0.0);
            return;
        }
        for (int k = 1; k <= 3; k++) {
            double u = k / 3.0;
            add(level, ModParticles.MIST.get(), from[0] + (to[0] - from[0]) * u, from[1] + (to[1] - from[1]) * u,
                    from[2] + (to[2] - from[2]) * u, 0.0, 0.0, 0.0);
        }
    }

    /** しぶき: 大きな塊 big 個（上へ up）と細かな滴 small 個を幅 spread に散らし、水面に泡を残す */
    private void burst(Level level, RandomSource r, double x, double surface, double z, int big, int small,
                              double spread, double up) {
        for (int i = 0; i < big; i++) {
            add(level, ModParticles.SPRAY.get(), x + (r.nextDouble() - 0.5) * spread * 0.5, surface + 0.1,
                    z + (r.nextDouble() - 0.5) * spread * 0.5, (r.nextDouble() - 0.5) * 0.12,
                    up + r.nextDouble() * 0.1, (r.nextDouble() - 0.5) * 0.12);
        }
        for (int i = 0; i < small; i++) {
            add(level, ParticleTypes.SPLASH, x + (r.nextDouble() - 0.5) * spread, surface + 0.05,
                    z + (r.nextDouble() - 0.5) * spread, 0.0, 0.2, 0.0);
        }
        for (int i = 0; i < Math.max(2, small / 6); i++) {
            add(level, ModParticles.FOAM.get(), x + (r.nextDouble() - 0.5) * spread, surface + 0.02,
                    z + (r.nextDouble() - 0.5) * spread, 0.0, 0.0, 0.0);
        }
    }

    // ---------------------------------------------------------------- 稲妻

    /** 後縁の稲妻: 後縁の点（付け根 → 翼端）のうち、1〜2 つ隣どうしを結ぶ（長さ 0.4〜2.4 ブロック） */
    private void edgeArc(Level level, RandomSource r, float yaw) {
        int side = r.nextInt(2);
        int n = ShearwaterLocators.EDGE_PER_SIDE;
        int k = r.nextInt(n);
        int k2 = Math.min(k + 1 + r.nextInt(2), n);
        double[] a = edge(side, k, yaw);
        double[] b = k2 < n ? edge(side, k2, yaw) : tip(side, 0, yaw);
        bolt(level, r, a, b, 2, 0.45);
    }

    /**
     * 放電: 翼端か後縁の外側から、翼の外へ 1.5〜2.5 ブロック空へ走り（上下へはばらし、前後へは少しだけ）、途中から枝を 1 本出す。
     * ⚠ 前は向きを前後にも大きくばらし長さも 2〜3.5 だったので、後ろから追って撮ると手前へ伸びた帯が画面を横切った（2026-09-28）
     */
    private void discharge(Level level, RandomSource r, float yaw) {
        int side = r.nextInt(2);
        int n = ShearwaterLocators.EDGE_PER_SIDE;
        double[] a = r.nextInt(2) == 0 ? tip(side, 0, yaw) : edge(side, n - 1 - r.nextInt(3), yaw);
        double ox = a[0] - this.bird.getX();
        double oz = a[2] - this.bird.getZ();
        double on = Math.max(Math.sqrt(ox * ox + oz * oz), 1.0E-3);
        double t = Math.toRadians(yaw);
        double fx = -Math.sin(t), fz = Math.cos(t);                 // 体の前（Minecraft の yaw の向き）
        double len = 1.5 + r.nextDouble();
        double out = 0.75, fwd = (r.nextDouble() - 0.5) * 0.5, up = (r.nextDouble() - 0.5) * 0.9;
        double[] b = {a[0] + (ox / on * out + fx * fwd) * len, a[1] + up * len, a[2] + (oz / on * out + fz * fwd) * len};
        List<double[]> kinks = bolt(level, r, a, b, 3, 0.55);
        double[] p = kinks.get(1 + r.nextInt(kinks.size() - 2));
        double bl = 0.6 + r.nextDouble() * 0.5;
        bolt(level, r, p, new double[] {p[0] + ox / on * bl * 0.5 + (r.nextDouble() - 0.5) * bl, p[1] - r.nextDouble() * bl,
                p[2] + oz / on * bl * 0.5 + (r.nextDouble() - 0.5) * bl}, 1, 0.3);
    }

    /** 前縁の長い稲妻: 肩 → 手首 → 腕の先 → 翼端（約 6 ブロック） */
    private void leadingArc(Level level, RandomSource r, float yaw) {
        int side = r.nextInt(2);
        double[] prev = arm(side, 0, yaw);
        for (int k = 1; k <= ShearwaterLocators.ARM_PER_SIDE; k++) {
            double[] next = k < ShearwaterLocators.ARM_PER_SIDE ? arm(side, k, yaw) : tip(side, 0, yaw);
            bolt(level, r, prev, next, 2, 0.35);
            prev = next;
        }
    }

    /**
     * 稲妻 1 本: a から b へ折れ目 kinks 個の折れ線を引く。区切りごとに帯（{@code BOLT}。速さの欄に次の点までの向きと長さ）を
     * 1 つ出し、折れ目と先に光の点（{@code SPARK}）を置いて、帯のつなぎ目を隠す。
     * 折れ目は a〜b を等分した点を jag の幅で横へ振る。返すのは折れ線の点（a・折れ目・b）
     */
    private static List<double[]> bolt(Level level, RandomSource r, double[] a, double[] b, int kinks, double jag) {
        List<double[]> pts = new ArrayList<>();
        pts.add(a);
        for (int i = 1; i <= kinks; i++) {
            double u = i / (double) (kinks + 1);
            pts.add(new double[] {a[0] + (b[0] - a[0]) * u + (r.nextDouble() - 0.5) * jag,
                    a[1] + (b[1] - a[1]) * u + (r.nextDouble() - 0.5) * jag,
                    a[2] + (b[2] - a[2]) * u + (r.nextDouble() - 0.5) * jag});
        }
        pts.add(b);
        for (int i = 0; i + 1 < pts.size(); i++) {
            double[] p = pts.get(i);
            double[] q = pts.get(i + 1);
            level.addParticle(ModParticles.BOLT.get(), p[0], p[1], p[2], q[0] - p[0], q[1] - p[1], q[2] - p[2]);
            level.addParticle(ModParticles.SPARK.get(), q[0], q[1], q[2], 0.0, 0.0, 0.0);
        }
        return pts;
    }

    // ---------------------------------------------------------------- 出どころ

    /** 翼端（side 0＝左・1＝右）。描いた骨から取れていなければ、滑空か傾き（bank）の決まった点 */
    private double[] tip(int side, int bank, float yaw) {
        if (this.bird.livePoints() != null) {
            return live(ShearwaterLocators.TIP + side, yaw);
        }
        double[] p = side == 0
                ? (bank > 0 ? ShearwaterLocators.BANK_R_TIP_L : bank < 0 ? ShearwaterLocators.BANK_L_TIP_L : ShearwaterLocators.GLIDE_TIP_L)
                : (bank > 0 ? ShearwaterLocators.BANK_R_TIP_R : bank < 0 ? ShearwaterLocators.BANK_L_TIP_R : ShearwaterLocators.GLIDE_TIP_R);
        return at(p, yaw);
    }

    /** 後縁の k 番目（付け根から先へ） */
    private double[] edge(int side, int k, float yaw) {
        return live(ShearwaterLocators.EDGE + side * ShearwaterLocators.EDGE_PER_SIDE + k, yaw);
    }

    /** 前縁の k 番目（0＝肩・1＝手首・2＝腕の先） */
    private double[] arm(int side, int k, float yaw) {
        return live(ShearwaterLocators.ARM + side * ShearwaterLocators.ARM_PER_SIDE + k, yaw);
    }

    /** 点 i（{@code ShearwaterLocators.LIVE_*} の番号）の世界の位置。描いた骨から取れていなければ滑空の姿勢の点 */
    private double[] live(int i, float yaw) {
        double[][] pts = this.bird.livePoints();
        if (pts != null) {
            double[] p = pts[i];
            return new double[] {this.bird.getX() + p[0], this.bird.getY() + p[1], this.bird.getZ() + p[2]};
        }
        return at(ShearwaterLocators.LIVE_GLIDE[i], yaw);
    }

    private double[] at(double[] local, float yaw) {
        double[] d = ShearwaterLocators.toWorld(local, yaw);
        return new double[] {this.bird.getX() + d[0], this.bird.getY() + d[1], this.bird.getZ() + d[2]};
    }

    /** 点の近く（上 1・下 2 ブロック）にある水面の高さ。無ければ NaN。 */
    static double surfaceNear(Level level, double[] p) {
        BlockPos bp = BlockPos.containing(p[0], p[1], p[2]);
        for (int dy = 1; dy >= -2; dy--) {
            BlockPos q = bp.offset(0, dy, 0);
            FluidState fs = level.getFluidState(q);
            if (fs.is(FluidTags.WATER) && !level.getFluidState(q.above()).is(FluidTags.WATER)) {
                return q.getY() + fs.getHeight(level, q);
            }
        }
        return Double.NaN;
    }
}
