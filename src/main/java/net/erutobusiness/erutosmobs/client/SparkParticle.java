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
 * 稲妻の折れ目の光の点。白い芯に水色の光の輪の絵（キットが描く `shearwater_spark_{0,1}`）を、幅 0.28〜0.4 ブロックで、
 * 暗くても明るく（光の強さ最大で）描く。2〜4 tick だけ光る。{@code ShearwaterEffects.bolt} が稲妻の帯
 * （{@link BoltParticle}）のつなぎ目と先に置く（毎 tick 別の場所に新しい稲妻が出て、ぱちぱち走って見える）。
 * ⚠⚠ 2026-09-28・ユーザー「帯電の火花も見えない」。前はバニラの光の点（glow）を散らしていた。開いて見たら
 *   glow は 8×8 の太さ 1 画素の十字で、大きくしても細い十字が翼の上にいくつか乗るだけだった。
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
        this.lifetime = 2 + this.random.nextInt(3);
        this.quadSize = 0.14f + this.random.nextFloat() * 0.06f;
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
