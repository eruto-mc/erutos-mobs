package net.erutobusiness.erutosmobs.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/**
 * 翼端の風の筋。淡い白で、重さが無く、1.1〜1.6 秒で薄れながらふくらむ。
 * 翼端の前の tick の位置から今の位置まで並べて出すので、途切れずに後ろへ帯が引かれる（{@code ShearwaterEffects}）。
 * ⚠ 幅は 0.6〜0.9 から 1.2〜1.6 まで（2026-09-28。前は 0.2〜0.3 で、翼幅 12 の鳥の翼端では細い糸にしか
 *   見えなかった。大きさの物差しは {@code ModParticles} の注記）。
 * ⚠ 見た目は変えない（2026-09-28・ユーザー「翼も風の筋かっこいいね」）。しぶきと見分けがつかなかった件は、
 *   しぶき・泡の絵を水らしく描き直し、水に触れている翼端からは風の筋を出さないことで分けた。
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
        this.lifetime = 22 + this.random.nextInt(10);
        this.quadSize = 0.30f + this.random.nextFloat() * 0.15f;
        this.rCol = 0.93f;
        this.gCol = 0.97f;
        this.bCol = 1.0f;
        this.alpha0 = 0.42f;
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
        this.quadSize *= 1.025f;
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
