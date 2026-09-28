package net.erutobusiness.erutosmobs.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * 大きなしぶき。バニラのしぶきの絵（splash_0〜3）を、幅 0.7〜1.4 ブロックで出す。
 * 渡された速さのまま飛び、重さ 1.0（1 tick に 0.04 ずつ落ちる）で弧を描いて、最後の 8 tick で薄れる。
 * ⚠ 上へ 0.3〜0.45 で出すと 2〜2.5 ブロック上がる（翼幅 12 の鳥の翼端が水を切る高さ。実物 20 cm × 11）。
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
        this.lifetime = 18 + this.random.nextInt(12);
        this.quadSize = 0.35f + this.random.nextFloat() * 0.35f;
        float c = 0.88f + this.random.nextFloat() * 0.12f;
        this.rCol = c * 0.92f;
        this.gCol = c * 0.97f;
        this.bCol = c;
        this.alpha = 0.9f;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.age > this.lifetime - 8) {
            this.alpha = Math.max(0.0f, 0.9f * (this.lifetime - this.age) / 8.0f);
        }
        if (this.onGround) {
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
