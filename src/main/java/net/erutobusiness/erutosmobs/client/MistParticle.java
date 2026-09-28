package net.erutobusiness.erutosmobs.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * 翼端の風の筋。小さく淡い白で、重さが無く、0.7〜1 秒で薄れて少しふくらむ。
 * 翼端から 1 tick に 1 個ずつ出すと、後ろへ細い線が引かれる。
 */
public class MistParticle extends TextureSheetParticle {
    private final SpriteSet sprites;
    private final float alpha0;

    protected MistParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                           SpriteSet sprites) {
        super(level, x, y, z);
        this.sprites = sprites;
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.gravity = 0.0f;
        this.friction = 0.9f;
        this.hasPhysics = false;
        this.lifetime = 14 + this.random.nextInt(8);
        this.quadSize = 0.10f + this.random.nextFloat() * 0.05f;
        this.rCol = 0.93f;
        this.gCol = 0.97f;
        this.bCol = 1.0f;
        this.alpha0 = 0.5f;
        this.alpha = this.alpha0;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }
        this.setSpriteFromAge(this.sprites);
        float u = (float) this.age / (float) this.lifetime;
        this.alpha = this.alpha0 * (1.0f - u);
        this.quadSize *= 1.03f;
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
            return new MistParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}
