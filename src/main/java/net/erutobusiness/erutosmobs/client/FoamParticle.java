package net.erutobusiness.erutosmobs.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

/**
 * 水面の泡。穴のあいた白い泡の絵（キットが描く `shearwater_foam_{0,1}`）を、幅 0.8〜1.4 ブロックで水面に寝かせて置く。
 * 動かず、2.5〜4 秒かけて少し広がりながら薄れる。翼端が波を切った線・漕いだ航跡・着水の輪に残る。
 * ⚠ 2026-09-28・ユーザー「風の筋か波切のしぶきか区別がつかない」。空中に立つ風の筋と違い、水面に寝ていることで見分けさせる。
 * ⚠ 粒子はふつう画面の方を向く四角（{@code SingleQuadParticle.render}）。ここだけ水平の四角を自分で書く。
 *   面の表裏のどちらが描かれるかは描画の状態しだいなので、表と裏の 2 枚を書く。
 */
public class FoamParticle extends TextureSheetParticle {
    private final float spin;

    protected FoamParticle(ClientLevel level, double x, double y, double z, SpriteSet sprites) {
        super(level, x, y, z);
        this.pickSprite(sprites);
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.gravity = 0.0f;
        this.hasPhysics = false;
        this.lifetime = 50 + this.random.nextInt(30);
        this.quadSize = 0.40f + this.random.nextFloat() * 0.30f;
        this.spin = this.random.nextFloat() * Mth.TWO_PI;
        this.alpha = 0.9f;
        // 画面の外へ出たかの判定に使う箱を、描く四角の大きさに合わせる（既定の 0.2 だと画面の端で先に消える）
        this.setSize(1.6f, 0.1f);
    }

    @Override
    public void tick() {
        super.tick();
        int left = this.lifetime - this.age;
        if (left < 25) {
            this.alpha = 0.9f * Math.max(0, left) / 25.0f;
        }
        this.quadSize *= 1.006f;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 cam = camera.getPosition();
        float x = (float) (Mth.lerp(partialTicks, this.xo, this.x) - cam.x());
        float y = (float) (Mth.lerp(partialTicks, this.yo, this.y) - cam.y());
        float z = (float) (Mth.lerp(partialTicks, this.zo, this.z) - cam.z());
        float s = this.getQuadSize(partialTicks);
        float c = Mth.cos(this.spin) * s;
        float n = Mth.sin(this.spin) * s;
        // 四隅 (-1,-1) (-1,1) (1,1) (1,-1) を水平に置き、spin だけ回す
        float[] cx = {-c + n, -c - n, c - n, c + n};
        float[] cz = {-n - c, -n + c, n + c, n - c};
        float[] us = {this.getU1(), this.getU1(), this.getU0(), this.getU0()};
        float[] vs = {this.getV1(), this.getV0(), this.getV0(), this.getV1()};
        int light = this.getLightColor(partialTicks);
        for (int pass = 0; pass < 2; pass++) {
            for (int k = 0; k < 4; k++) {
                int i = pass == 0 ? k : 3 - k;
                buffer.vertex(x + cx[i], y, z + cz[i]).uv(us[i], vs[i])
                        .color(this.rCol, this.gCol, this.bCol, this.alpha).uv2(light).endVertex();
            }
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
            return new FoamParticle(level, x, y, z, this.sprites);
        }
    }
}
