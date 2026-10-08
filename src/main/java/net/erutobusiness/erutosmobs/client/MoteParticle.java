package net.erutobusiness.erutosmobs.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;

/**
 * 光の粒。飛ぶ間、翼端と後縁の先からこぼれる琥珀の光（絵はキットが描く `shearwater_mote_{0,1}`。翼端の琥珀と同じ色）。
 * 幅 0.28〜0.4 ブロックで、昼も夜も明るさ最大で描く。その場に置いていくので、鳥が進むと後ろへ光の帯が引かれ、
 * ゆっくり沈みながら瞬いて、1.5〜2.5 秒で縮んで消える。{@code ShearwaterEffects} が 1 秒に 3 つほど出す。
 * ⚠⚠ 2026-10-08・ユーザー「なんかあんまスクショに伝説感が無いなぁ」。タイヨウチョウは翼に沿って羽根を
 *   1 秒に 20 個、昼も夜も出している（jar を読んだ値）。こちらは嵐の鳥なので数を絞り、晴れた昼にも出す。
 */
public class MoteParticle extends TextureSheetParticle {
    private final float size0;
    private final float phase;

    protected MoteParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                           SpriteSet sprites) {
        super(level, x, y, z);
        this.pickSprite(sprites);
        this.xd = xd;
        this.yd = yd;
        this.zd = zd;
        this.gravity = 0.05f;              // 1 tick に 0.002 ずつ沈む向きへ（空気の抵抗で 1.5〜2.5 秒に 1 ブロックほど）
        this.friction = 0.96f;
        this.hasPhysics = false;
        this.lifetime = 30 + this.random.nextInt(20);
        this.size0 = 0.14f + this.random.nextFloat() * 0.06f;
        this.quadSize = this.size0;
        this.phase = this.random.nextFloat() * Mth.TWO_PI;
        this.quadSize = 0.0f;
    }

    /**
     * 出てから 4 tick で灯り、大きさを ±12% で揺らして瞬き、終わりの 4 割で縮んで消える。
     * ⚠ 薄くして消さない（2026-10-08 に撮って確かめた）。半透明にすると昼は空の色が透けてくすみ、
     *   光ではなく茶色い縁の輪に見えた。夜は暗い空に重なるので気づきにくい。だから不透明のまま大きさだけを変える。
     */
    @Override
    public void tick() {
        super.tick();
        if (this.removed) {
            return;
        }
        float u = (float) this.age / (float) this.lifetime;
        float in = Math.min(1.0f, this.age / 4.0f);
        float out = u < 0.6f ? 1.0f : (1.0f - u) / 0.4f;
        float twinkle = 1.0f + 0.12f * Mth.sin(this.age * 0.8f + this.phase);
        this.quadSize = this.size0 * in * out * twinkle;
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
            return new MoteParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}
