package net.erutobusiness.erutosmobs.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * 火花。バニラの光の点（glow）を幅 0.3〜0.6 ブロックで、暗くても明るく（光の強さ最大で）描く。
 * 3〜6 tick だけ光り、細かく震えて消える。帯電中の稲妻は、これを翼の後縁に沿って折れ線に並べて描く
 * （{@code ShearwaterEffects}）。⚠ バニラの電気の火花（幅 0.15〜0.3・周りの明るさで暗くなる）は、
 * 翼幅 12 の鳥の上では見分けられなかった（2026-09-28 に撮った絵）。
 */
public class SparkParticle extends TextureSheetParticle {
    protected SparkParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                            SpriteSet sprites) {
        super(level, x, y, z);
        this.pickSprite(sprites);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.gravity = 0.0f;
        this.friction = 0.7f;
        this.hasPhysics = false;
        this.lifetime = 3 + this.random.nextInt(4);
        this.quadSize = 0.15f + this.random.nextFloat() * 0.15f;
        // 嵐の火花: 青白く、ときどき琥珀（体の光る縁と同じ色）
        if (this.random.nextInt(4) == 0) {
            this.rCol = 1.0f;
            this.gCol = 0.78f;
            this.bCol = 0.35f;
        } else {
            this.rCol = 0.75f;
            this.gCol = 0.92f;
            this.bCol = 1.0f;
        }
    }

    @Override
    public void tick() {
        super.tick();
        this.quadSize *= 0.85f;
    }

    @Override
    public int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
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
            return new SparkParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}
