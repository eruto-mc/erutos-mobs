package net.erutobusiness.erutosmobs.entity;

import net.erutobusiness.erutosmobs.registry.ModParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;

/**
 * クライアントで出す粒子と音（2026-09-28）。サーバは状態だけを持ち、見た目はここで各自が作る
 * （タイヨウチョウも羽根はクライアント側で毎 tick 出している）。
 *
 * ⚠⚠ 大きさは「実物 × 11」（この鳥は翼幅 12 ブロックで、実物 1.1 m の約 11 倍。1 ブロック ＝ 1 m）。
 *   最初はバニラの粒子（幅 0.2〜0.4）をそのまま出していて、翼幅の 2〜3% の粉にしかならなかった
 *   （2026-09-28・ユーザー「パーティクルが体に対して小さすぎる」）。
 *
 * 出すもの（設計は README の「光・粒子・雷」）:
 *   風の筋     … 速く滑空しているとき、両方の翼端から。前の tick の翼端から今の翼端まで並べて、途切れない帯にする
 *   波を切る   … 下がった翼端が水面に触れたら、2〜2.5 ブロック上がるしぶき（実物 20 cm × 11）と、切った線の泡
 *   嵐の火花   … 帯電中は翼の後縁に沿った稲妻の折れ線（長さ 1〜3 ブロック）。雷雨だけならときどき
 *   足の水しぶき … 水面からの離陸で、足が水を蹴る拍に（実物 20 cm × 11 → 幅 2 ブロックほど）
 *   水滴       … 水から飛び立った後の 5 秒、翼の後縁から落ちる
 *   航跡       … 水面を漕いで進むとき、胴の後ろへ八の字に開く（実物の幅 0.5〜1 m × 11 → 6〜8 ブロック）
 *   着水       … 急降下から水面に入った瞬間の大きなしぶき（半径 1〜3.5 ブロック）
 * 位置は {@link ShearwaterLocators}（キットが模型から計算して書いた値）を体の向きへ回して使う。
 */
final class ShearwaterEffects {
    private final ShearwaterEntity bird;
    private int lastState = -1;
    private int stateTicks;
    private int wetTicks;
    private int splashSoundCooldown;
    private boolean takeoffFromWater;
    /** 前の tick の翼端（世界の座標）。風の筋を途切れさせないため。滑空していない間は null */
    private double[] lastTipL;
    private double[] lastTipR;

    ShearwaterEffects(ShearwaterEntity bird) {
        this.bird = bird;
    }

    void tick() {
        Level level = this.bird.level();
        RandomSource r = this.bird.getRandom();
        int st = this.bird.getState();
        if (st != this.lastState) {
            onChange(level, r, this.lastState, st);
            this.lastState = st;
            this.stateTicks = 0;
        } else {
            this.stateTicks++;
        }
        if (this.splashSoundCooldown > 0) {
            this.splashSoundCooldown--;
        }
        float yaw = this.bird.yBodyRot;
        double dx = this.bird.getX() - this.bird.xo;
        double dz = this.bird.getZ() - this.bird.zo;
        double speed = Math.sqrt(dx * dx + dz * dz);
        int bank = this.bird.getBank();

        // 風の筋（前の翼端から今の翼端まで 3 つ並べる）
        if (st == ShearwaterEntity.GLIDE && speed > 0.12) {
            double[] tipL = at(bank > 0 ? ShearwaterLocators.BANK_R_TIP_L
                    : bank < 0 ? ShearwaterLocators.BANK_L_TIP_L : ShearwaterLocators.GLIDE_TIP_L, yaw);
            double[] tipR = at(bank > 0 ? ShearwaterLocators.BANK_R_TIP_R
                    : bank < 0 ? ShearwaterLocators.BANK_L_TIP_R : ShearwaterLocators.GLIDE_TIP_R, yaw);
            trail(level, this.lastTipL, tipL);
            trail(level, this.lastTipR, tipR);
            this.lastTipL = tipL;
            this.lastTipR = tipR;
        } else {
            this.lastTipL = null;
            this.lastTipR = null;
        }

        // 波を切る（下がった側の翼端だけ）
        if (st == ShearwaterEntity.GLIDE && bank != 0) {
            double[] low = at(bank > 0 ? ShearwaterLocators.BANK_R_TIP_R : ShearwaterLocators.BANK_L_TIP_L, yaw);
            double surface = surfaceNear(level, low);
            if (!Double.isNaN(surface) && low[1] <= surface + 0.3) {
                // 翼の外へ向かって跳ね上がる塊 3 つ・細かな滴 6 つ・切った線の泡 2 つ
                double ox = low[0] - this.bird.getX();
                double oz = low[2] - this.bird.getZ();
                double on = Math.max(Math.sqrt(ox * ox + oz * oz), 1.0E-3);
                for (int i = 0; i < 3; i++) {
                    level.addParticle(ModParticles.SPRAY.get(), low[0], surface + 0.1, low[2],
                            ox / on * (0.06 + r.nextDouble() * 0.08) - dx * 0.3,
                            0.28 + r.nextDouble() * 0.14,
                            oz / on * (0.06 + r.nextDouble() * 0.08) - dz * 0.3);
                }
                for (int i = 0; i < 6; i++) {
                    level.addParticle(ParticleTypes.SPLASH, low[0] + (r.nextDouble() - 0.5) * 1.5, surface + 0.05,
                            low[2] + (r.nextDouble() - 0.5) * 1.5, 0.0, 0.2, 0.0);
                }
                for (int i = 0; i < 2; i++) {
                    level.addParticle(ParticleTypes.FISHING, low[0] + (r.nextDouble() - 0.5) * 0.8, surface + 0.02,
                            low[2] + (r.nextDouble() - 0.5) * 0.8, 0.0, 0.0, 0.0);
                }
                if (this.splashSoundCooldown == 0) {
                    level.playLocalSound(low[0], surface, low[2], SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL,
                            0.6f, 1.1f + r.nextFloat() * 0.3f, false);
                    this.splashSoundCooldown = 8;
                }
            }
        }

        // 嵐の火花（帯電中は稲妻の折れ線、雷雨だけならときどき）
        boolean charged = this.bird.isCharged();
        if (charged || level.isThundering()) {
            int arcs = charged ? (r.nextInt(3) == 0 ? 2 : 1) : (r.nextInt(12) == 0 ? 1 : 0);
            for (int i = 0; i < arcs; i++) {
                arc(level, r, yaw);
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
                double[] e = ShearwaterLocators.TRAILING_EDGE[r.nextInt(ShearwaterLocators.TRAILING_EDGE.length)];
                double[] w = at(e, yaw);
                level.addParticle(ParticleTypes.FALLING_WATER, w[0], w[1] - 0.05, w[2], 0.0, 0.0, 0.0);
            }
            if (r.nextInt(4) == 0) {
                double[] e = ShearwaterLocators.TRAILING_EDGE[r.nextInt(ShearwaterLocators.TRAILING_EDGE.length)];
                double[] w = at(e, yaw);
                level.addParticle(ModParticles.SPRAY.get(), w[0], w[1] - 0.1, w[2], 0.0, -0.05, 0.0);
            }
        }

        // 航跡（水面を漕いで進む）: 胴の後ろへ八の字に開く 2 本の線
        if (st == ShearwaterEntity.PADDLE && speed > 0.01 && this.stateTicks % 2 == 0) {
            for (int side = -1; side <= 1; side += 2) {
                double back = 1.5 + r.nextDouble() * 4.0;
                double[] p = at(new double[] {side * (0.8 + back * 0.55), 0.3, -back}, yaw);
                double surface = surfaceNear(level, p);
                if (!Double.isNaN(surface)) {
                    level.addParticle(ParticleTypes.FISHING, p[0], surface + 0.02, p[2], 0.0, 0.0, 0.0);
                }
            }
            if (speed > 0.04 && r.nextInt(3) == 0) {
                double[] bow = at(new double[] {0.0, 0.3, 2.2}, yaw);
                double surface = surfaceNear(level, bow);
                if (!Double.isNaN(surface)) {
                    level.addParticle(ModParticles.SPRAY.get(), bow[0], surface + 0.05, bow[2],
                            (r.nextDouble() - 0.5) * 0.1, 0.12, (r.nextDouble() - 0.5) * 0.1);
                }
            }
        }
    }

    private void onChange(Level level, RandomSource r, int prev, int now) {
        if (prev == ShearwaterEntity.DIVE && now == ShearwaterEntity.PADDLE) {
            // 着水の大きなしぶき（実物 1 m 近い水柱 × 11 は大きすぎるので、半径 1〜3.5・高さ 3 ほどに抑える）
            double[] c = at(ShearwaterLocators.BODY_CENTER, this.bird.yBodyRot);
            double surface = surfaceNear(level, c);
            if (!Double.isNaN(surface)) {
                for (int i = 0; i < 24; i++) {
                    double a = r.nextDouble() * Math.PI * 2.0;
                    double rad = 1.0 + r.nextDouble() * 2.5;
                    level.addParticle(ModParticles.SPRAY.get(), c[0] + Math.cos(a) * rad, surface + 0.1,
                            c[2] + Math.sin(a) * rad, Math.cos(a) * 0.12, 0.3 + r.nextDouble() * 0.2,
                            Math.sin(a) * 0.12);
                }
                burst(level, r, c[0], surface, c[2], 0, 50, 6.0, 0.3);
                for (int i = 0; i < 20; i++) {
                    level.addParticle(ParticleTypes.BUBBLE, c[0] + (r.nextDouble() - 0.5) * 5.0, surface - 0.6,
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

    /** 風の筋: from から to まで 3 つ並べる（from が無ければ to に 1 つ） */
    private static void trail(Level level, double[] from, double[] to) {
        if (from == null) {
            level.addParticle(ModParticles.MIST.get(), to[0], to[1], to[2], 0.0, 0.0, 0.0);
            return;
        }
        for (int k = 1; k <= 3; k++) {
            double u = k / 3.0;
            level.addParticle(ModParticles.MIST.get(), from[0] + (to[0] - from[0]) * u, from[1] + (to[1] - from[1]) * u,
                    from[2] + (to[2] - from[2]) * u, 0.0, 0.0, 0.0);
        }
    }

    /** しぶき: 大きな塊 big 個（上へ up）と細かな滴 small 個を、幅 spread に散らす */
    private static void burst(Level level, RandomSource r, double x, double surface, double z, int big, int small,
                              double spread, double up) {
        for (int i = 0; i < big; i++) {
            level.addParticle(ModParticles.SPRAY.get(), x + (r.nextDouble() - 0.5) * spread * 0.5, surface + 0.1,
                    z + (r.nextDouble() - 0.5) * spread * 0.5, (r.nextDouble() - 0.5) * 0.12,
                    up + r.nextDouble() * 0.1, (r.nextDouble() - 0.5) * 0.12);
        }
        for (int i = 0; i < small; i++) {
            level.addParticle(ParticleTypes.SPLASH, x + (r.nextDouble() - 0.5) * spread, surface + 0.05,
                    z + (r.nextDouble() - 0.5) * spread, 0.0, 0.2, 0.0);
        }
        for (int i = 0; i < Math.max(2, small / 4); i++) {
            level.addParticle(ParticleTypes.FISHING, x + (r.nextDouble() - 0.5) * spread, surface + 0.02,
                    z + (r.nextDouble() - 0.5) * spread, 0.0, 0.0, 0.0);
        }
    }

    /**
     * 稲妻の折れ線: 翼の後縁の隣り合う 2 点（か、翼端とその隣）を、横へ振れる 7 つの火花で結ぶ。
     * 長さは点の間隔ぶん（1〜3 ブロック）。
     */
    private void arc(Level level, RandomSource r, float yaw) {
        double[][] edge = ShearwaterLocators.TRAILING_EDGE;
        int half = edge.length / 2;
        int side = r.nextInt(2);
        int k = r.nextInt(half);
        double[] a = at(edge[side * half + k], yaw);
        double[] b = k + 1 < half ? at(edge[side * half + k + 1], yaw)
                : at(side == 0 ? ShearwaterLocators.GLIDE_TIP_L : ShearwaterLocators.GLIDE_TIP_R, yaw);
        for (int i = 0; i <= 6; i++) {
            double u = i / 6.0;
            double jag = (i == 0 || i == 6) ? 0.0 : 0.35;
            level.addParticle(ModParticles.SPARK.get(),
                    a[0] + (b[0] - a[0]) * u + (r.nextDouble() - 0.5) * jag,
                    a[1] + (b[1] - a[1]) * u + (r.nextDouble() - 0.5) * jag,
                    a[2] + (b[2] - a[2]) * u + (r.nextDouble() - 0.5) * jag, 0.0, 0.0, 0.0);
        }
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
