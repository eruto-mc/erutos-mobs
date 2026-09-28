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
 * 出すもの（設計は README の「表現」）:
 *   風の筋     … 滑空で速いとき、両方の翼端から 1 tick 1 個ずつ
 *   波を切る   … 傾いて低く滑るとき、下がった翼端が水面に触れたら、しぶき（名前の由来 "shearing"）
 *   嵐の火花   … 雷雨と帯電の間、翼の後縁と翼端から
 *   足の水しぶき … 水面からの離陸で、足が水を蹴る拍に
 *   水滴       … 水から飛び立った後の 5 秒、翼の後縁から落ちる
 *   航跡       … 水面を漕いで進むとき、尾の後ろ
 *   着水       … 急降下から水面に入った瞬間の大きなしぶき
 * 位置は {@link ShearwaterLocators}（キットが模型から計算して書いた値）を体の向きへ回して使う。
 */
final class ShearwaterEffects {
    private final ShearwaterEntity bird;
    private int lastState = -1;
    private int stateTicks;
    private int wetTicks;
    private int splashSoundCooldown;
    private boolean takeoffFromWater;

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

        // 風の筋
        if (st == ShearwaterEntity.GLIDE && speed > 0.12) {
            double[][] tips = bank > 0 ? new double[][] {ShearwaterLocators.BANK_R_TIP_L, ShearwaterLocators.BANK_R_TIP_R}
                    : bank < 0 ? new double[][] {ShearwaterLocators.BANK_L_TIP_L, ShearwaterLocators.BANK_L_TIP_R}
                    : new double[][] {ShearwaterLocators.GLIDE_TIP_L, ShearwaterLocators.GLIDE_TIP_R};
            for (double[] tip : tips) {
                double[] w = at(tip, yaw);
                level.addParticle(ModParticles.MIST.get(), w[0], w[1], w[2], -dx * 0.15, 0.0, -dz * 0.15);
            }
        }

        // 波を切る（下がった側の翼端だけ）
        if (st == ShearwaterEntity.GLIDE && bank != 0) {
            double[] low = at(bank > 0 ? ShearwaterLocators.BANK_R_TIP_R : ShearwaterLocators.BANK_L_TIP_L, yaw);
            double surface = surfaceNear(level, low);
            if (!Double.isNaN(surface) && low[1] <= surface + 0.3) {
                for (int i = 0; i < 4; i++) {
                    level.addParticle(ParticleTypes.SPLASH, low[0] + (r.nextDouble() - 0.5) * 0.6, surface + 0.05,
                            low[2] + (r.nextDouble() - 0.5) * 0.6, 0.0, 0.1, 0.0);
                }
                level.addParticle(ParticleTypes.FISHING, low[0], surface + 0.02, low[2], -dx * 0.05, 0.0, -dz * 0.05);
                if (this.splashSoundCooldown == 0) {
                    level.playLocalSound(low[0], surface, low[2], SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL,
                            0.35f, 1.3f + r.nextFloat() * 0.3f, false);
                    this.splashSoundCooldown = 8;
                }
            }
        }

        // 嵐の火花
        boolean charged = this.bird.isCharged();
        if (charged || level.isThundering()) {
            int n = charged ? 2 : (r.nextInt(3) == 0 ? 1 : 0);
            for (int i = 0; i < n; i++) {
                double[] p = pick(r);
                double[] w = at(p, yaw);
                level.addParticle(ParticleTypes.ELECTRIC_SPARK, w[0], w[1], w[2],
                        (r.nextDouble() - 0.5) * 0.2, (r.nextDouble() - 0.5) * 0.2, (r.nextDouble() - 0.5) * 0.2);
            }
        }

        // 足の水しぶき（離陸の走り。動きは 2 秒で 3 周＝6 歩 → 約 6.7 tick ごと）
        if (st == ShearwaterEntity.TAKEOFF && this.takeoffFromWater && this.stateTicks < 26 && this.stateTicks % 7 == 3) {
            double[] foot = at((this.stateTicks / 7) % 2 == 0 ? ShearwaterLocators.FOOT_L : ShearwaterLocators.FOOT_R, yaw);
            double surface = surfaceNear(level, foot);
            if (!Double.isNaN(surface)) {
                burst(level, r, foot[0], surface, foot[2], 6, 0.35);
                level.playLocalSound(foot[0], surface, foot[2], SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL,
                        0.4f, 1.1f + r.nextFloat() * 0.2f, false);
            }
        }

        // 水滴（水から飛び立った後）
        if (this.wetTicks > 0) {
            this.wetTicks--;
            for (int i = 0; i < 2; i++) {
                double[] e = ShearwaterLocators.TRAILING_EDGE[r.nextInt(ShearwaterLocators.TRAILING_EDGE.length)];
                double[] w = at(e, yaw);
                level.addParticle(ParticleTypes.FALLING_WATER, w[0], w[1] - 0.05, w[2], 0.0, 0.0, 0.0);
            }
        }

        // 航跡（水面を漕いで進む）
        if (st == ShearwaterEntity.PADDLE && speed > 0.01 && this.stateTicks % 3 == 0) {
            double[] tail = at(new double[] {0.0, 0.3, -1.8}, yaw);
            double surface = surfaceNear(level, tail);
            if (!Double.isNaN(surface)) {
                level.addParticle(ParticleTypes.FISHING, tail[0], surface + 0.02, tail[2], -dx * 0.2, 0.0, -dz * 0.2);
            }
        }
    }

    private void onChange(Level level, RandomSource r, int prev, int now) {
        if (prev == ShearwaterEntity.DIVE && now == ShearwaterEntity.PADDLE) {
            // 着水の大きなしぶき
            double[] c = at(ShearwaterLocators.BODY_CENTER, this.bird.yBodyRot);
            double surface = surfaceNear(level, c);
            if (!Double.isNaN(surface)) {
                burst(level, r, c[0], surface, c[2], 40, 2.0);
                for (int i = 0; i < 12; i++) {
                    level.addParticle(ParticleTypes.BUBBLE, c[0] + (r.nextDouble() - 0.5) * 2.0, surface - 0.4,
                            c[2] + (r.nextDouble() - 0.5) * 2.0, 0.0, 0.05, 0.0);
                }
                level.playLocalSound(c[0], surface, c[2], SoundEvents.GENERIC_SPLASH, SoundSource.NEUTRAL,
                        1.0f, 0.8f + r.nextFloat() * 0.15f, false);
            }
        }
        if (now == ShearwaterEntity.TAKEOFF) {
            this.takeoffFromWater = prev == ShearwaterEntity.PADDLE || prev == ShearwaterEntity.SLEEP;
            if (this.takeoffFromWater) {
                this.wetTicks = 140;          // 離陸 2 秒＋飛び立った後の 5 秒
            }
        }
    }

    private static void burst(Level level, RandomSource r, double x, double surface, double z, int n, double spread) {
        for (int i = 0; i < n; i++) {
            level.addParticle(ParticleTypes.SPLASH, x + (r.nextDouble() - 0.5) * spread, surface + 0.05,
                    z + (r.nextDouble() - 0.5) * spread, 0.0, 0.15, 0.0);
        }
        for (int i = 0; i < Math.max(1, n / 6); i++) {
            level.addParticle(ParticleTypes.FISHING, x + (r.nextDouble() - 0.5) * spread, surface + 0.02,
                    z + (r.nextDouble() - 0.5) * spread, 0.0, 0.0, 0.0);
        }
    }

    /** 翼の後縁か翼端のどれか（火花の出どころ） */
    private static double[] pick(RandomSource r) {
        int n = ShearwaterLocators.TRAILING_EDGE.length;
        int k = r.nextInt(n + 2);
        if (k == n) {
            return ShearwaterLocators.GLIDE_TIP_L;
        }
        if (k == n + 1) {
            return ShearwaterLocators.GLIDE_TIP_R;
        }
        return ShearwaterLocators.TRAILING_EDGE[k];
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
