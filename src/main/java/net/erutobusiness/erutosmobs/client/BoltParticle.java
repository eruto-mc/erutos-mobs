package net.erutobusiness.erutosmobs.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.ParticleRenderType;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.client.particle.TextureSheetParticle;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/**
 * 稲妻のひと区切り。出した点から、速さの欄で渡された向きと長さの先まで、幅 0.2〜0.26 ブロックの帯を引く
 * （白い芯・水色の縁。キットが描く `shearwater_bolt_0`）。帯はいつもこちらを向き、暗くても明るく、2〜4 tick で消える。
 * {@code ShearwaterEffects.bolt} が折れ線の区切りごとに 1 つ出し、折れ目に光の点（{@link SparkParticle}）を置く。
 * ⚠⚠ 2026-09-28・ユーザー「帯電の火花も見えない」→ 光の点を 0.12 ブロックごとに並べたら、撮った絵で「数珠」に見えた。
 *   帯なら 1 本の線になる。
 * ⚠ 動かない粒子なので、画面の外かの判定に使う箱は、線の両端を囲む箱にする（既定の 0.2 だと画面の端で先に消える）。
 */
public class BoltParticle extends TextureSheetParticle {
    private final float ex;
    private final float ey;
    private final float ez;

    protected BoltParticle(ClientLevel level, double x, double y, double z, double dx, double dy, double dz, SpriteSet sprites) {
        super(level, x, y, z);
        this.pickSprite(sprites);
        this.ex = (float) dx;
        this.ey = (float) dy;
        this.ez = (float) dz;
        this.xd = 0.0;
        this.yd = 0.0;
        this.zd = 0.0;
        this.gravity = 0.0f;
        this.hasPhysics = false;
        this.lifetime = 2 + this.random.nextInt(3);
        this.quadSize = 0.10f + this.random.nextFloat() * 0.03f;
        this.setBoundingBox(new AABB(x, y, z, x + dx, y + dy, z + dz).inflate(0.3));
    }

    @Override
    public void tick() {
        this.xo = this.x;
        this.yo = this.y;
        this.zo = this.z;
        if (this.age++ >= this.lifetime) {
            this.remove();
        }
    }

    @Override
    public int getLightColor(float partialTick) {
        return LightTexture.FULL_BRIGHT;
    }

    @Override
    public void render(VertexConsumer buffer, Camera camera, float partialTicks) {
        Vec3 cam = camera.getPosition();
        float x0 = (float) (this.x - cam.x());
        float y0 = (float) (this.y - cam.y());
        float z0 = (float) (this.z - cam.z());
        float x1 = x0 + this.ex;
        float y1 = y0 + this.ey;
        float z1 = z0 + this.ez;
        // 幅の向き: 線と、目から線の中ほどへの向きの両方に直角（帯がこちらを向く）
        Vector3f side = new Vector3f(this.ex, this.ey, this.ez).cross((x0 + x1) * 0.5f, (y0 + y1) * 0.5f, (z0 + z1) * 0.5f);
        if (side.lengthSquared() < 1.0E-10f) {
            return;
        }
        side.normalize(this.quadSize);
        float u0 = this.getU0();
        float u1 = this.getU1();
        float v0 = this.getV0();
        float v1 = this.getV1();
        int light = this.getLightColor(partialTicks);
        float[][] q = {
                {x0 - side.x, y0 - side.y, z0 - side.z, u0, v0},
                {x0 + side.x, y0 + side.y, z0 + side.z, u1, v0},
                {x1 + side.x, y1 + side.y, z1 + side.z, u1, v1},
                {x1 - side.x, y1 - side.y, z1 - side.z, u0, v1}};
        // 表と裏の 2 枚（面の表裏のどちらが描かれるかは描画の状態しだい）
        for (int pass = 0; pass < 2; pass++) {
            for (int k = 0; k < 4; k++) {
                float[] p = q[pass == 0 ? k : 3 - k];
                buffer.vertex(p[0], p[1], p[2]).uv(p[3], p[4])
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
                                       double dx, double dy, double dz) {
            return new BoltParticle(level, x, y, z, dx, dy, dz, this.sprites);
        }
    }
}
