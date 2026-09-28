package net.erutobusiness.erutosmobs.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.tags.FluidTags;

/**
 * 大きなしぶき。水の粒の絵（白い芯・水色の縁・右下が濃い青。キットが描く `shearwater_spray_{0..3}`）を、
 * 幅 0.6〜1.1 ブロックで出す。渡された速さのまま飛び、重さ 1.0（1 tick に 0.04 ずつ落ちる）と空気の抵抗 0.96 で
 * 弧を描き、水へ落ちたら消える。最後の 6 tick で薄れる。
 * 上へ出す速さと上がる高さ（計算）: 0.30 → 0.8・0.45 → 1.8・0.50 → 2.2・0.60 → 3.1 ブロック。
 * ⚠⚠ 2026-09-28・ユーザー「風の筋か波切のしぶきか区別がつかない」。前はバニラのしぶきの絵（splash_0〜3）を借りていた。
 *   開いて見たら 8×8 の下の隅に濃い青の 2〜3 画素しかなく、海の上では見えていなかった。
 */
public class SprayParticle extends TextureSheetParticle {
    protected SprayParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                            SpriteSet sprites) {
        super(level, x, y, z);
        this.pickSprite(sprites);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.gravity = 1.0f;
        this.friction = 0.96f;
        this.lifetime = 20 + this.random.nextInt(14);
        this.quadSize = 0.30f + this.random.nextFloat() * 0.25f;
        this.alpha = 1.0f;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }
        if (this.age > this.lifetime - 6) {
            this.alpha = Math.max(0.0f, (this.lifetime - this.age) / 6.0f);
        }
        // 落ちて水に入ったら消える（水には当たり判定が無いので、放っておくと水の中を沈んでいく）
        if (this.onGround || (this.yd < 0.0
                && this.level.getFluidState(BlockPos.containing(this.x, this.y, this.z)).is(FluidTags.WATER))) {
            this.remove();
        }
    }

    @Override
    public ParticleRenderType getRenderType() {
        return ParticleRenderType.PARTICLE_SHEET_TRANSLUCENT;
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level, double x, double y, double z,
                                       double xd, double yd, double zd) {
            return new SprayParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}
