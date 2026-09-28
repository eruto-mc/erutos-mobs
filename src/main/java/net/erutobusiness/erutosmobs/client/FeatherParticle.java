package net.erutobusiness.erutosmobs.client;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;

/** 嵐色の羽根。ゆっくり落ちながら左右に揺れて回り、最後の 15 tick で消える。 */
public class FeatherParticle extends TextureSheetParticle {
    private final float spin;

    protected FeatherParticle(ClientLevel level, double x, double y, double z, double xd, double yd, double zd,
                              SpriteSet sprites) {
        // ⚠ 速さ付きの親の作りは速さに乱れを大きく足すので、位置だけの作りを使って速さは自分で入れる
        super(level, x, y, z);
        this.pickSprite(sprites);
        this.xd = xd + (this.random.nextDouble() - 0.5) * 0.08;
        this.yd = yd + this.random.nextDouble() * 0.06;
        this.zd = zd + (this.random.nextDouble() - 0.5) * 0.08;
        this.gravity = 0.025f;
        this.friction = 0.93f;
        this.lifetime = 60 + this.random.nextInt(50);
        // 幅 0.5〜0.8 ブロック。翼幅 12 ブロックの鳥の体の羽根として（実物は翼幅の 3〜5%）。
        // 0.3 前後だと、寄りの昼の絵でも背の色に紛れて見分けられなかった（2026-09-28 に開発用クライアントで撮って確かめた）
        this.quadSize = 0.25f + this.random.nextFloat() * 0.15f;
        this.spin = (this.random.nextFloat() - 0.5f) * 0.25f;
        this.roll = this.random.nextFloat() * 6.2832f;
        this.oRoll = this.roll;
    }

    @Override
    public void tick() {
        super.tick();
        this.oRoll = this.roll;
        this.roll += this.spin;
        this.xd += Math.sin(this.age * 0.25) * 0.004;
        this.zd += Math.cos(this.age * 0.21) * 0.004;
        if (this.age > this.lifetime - 15) {
            this.alpha = Math.max(0.0f, (this.lifetime - this.age) / 15.0f);
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
            return new FeatherParticle(level, x, y, z, xd, yd, zd, this.sprites);
        }
    }
}
