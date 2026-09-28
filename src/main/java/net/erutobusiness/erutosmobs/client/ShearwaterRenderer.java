package net.erutobusiness.erutosmobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.erutobusiness.erutosmobs.entity.ShearwaterEntity;
import net.erutobusiness.erutosmobs.entity.ShearwaterLocators;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ShearwaterRenderer extends GeoEntityRenderer<ShearwaterEntity> {
    /**
     * 模型は翼幅 322（1/16 ブロック単位）＝約 20 ブロック。伝説として大きすぎない 0.6 倍
     * （翼幅 約 12 ブロック。Alex's Mobs の Sunbird は 13 ブロック）。
     */
    public static final float SCALE = 0.6f;

    /** 翼の点を取る骨（{@link ShearwaterLocators#LIVE_BONES} の並び）。組み直された模型に替わったら引き直す */
    private BakedGeoModel trackedModel;
    private GeoBone[] trackedBones;

    public ShearwaterRenderer(EntityRendererProvider.Context context) {
        super(context, new ShearwaterModel());
        this.shadowRadius = 1.6f;
        this.withScale(SCALE);
        addRenderLayer(new ShearwaterGlowLayer(this));
    }

    /**
     * ⚠ GeckoLib は死亡中の生き物を自前で横へ 90° 倒す（`GeoEntityRenderer.applyRotations`）。
     * こちらの「気絶」の動きも横へ 75° 倒れるので、重なると背中が真上から 162° の裏返しになった
     * （キットの `gecko_space.py` で原文どおりに再現して測った。重ならなければ 75°）。倒れ方は動きに任せる。
     */
    @Override
    protected float getDeathMaxRotation(ShearwaterEntity animatable) {
        return 0.0f;
    }

    @Override
    public void preRender(PoseStack poseStack, ShearwaterEntity animatable, BakedGeoModel model, MultiBufferSource bufferSource,
                          VertexConsumer buffer, boolean isReRender, float partialTick, int packedLight, int packedOverlay,
                          float red, float green, float blue, float alpha) {
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, partialTick, packedLight, packedOverlay,
                red, green, blue, alpha);
        if (!isReRender) {
            tracked(model);
        }
    }

    /**
     * ⚠⚠ 描いた翼の点を実体へ渡す（2026-09-28・ユーザー「帯電の火花も見えない」）。
     * ⚠ 前は滑空の姿勢で計算した決まった点から火花・水滴を出していたので、羽ばたいている間は翼から離れた空中に出た。
     *   GeckoLib は骨の行列を写す印（`setTrackingMatrices`）の付いた骨だけ、描くときに回転の中心の行列を残す
     *   （4.8.4 `GeoEntityRenderer.renderRecursively`）。そこへキットが計算した点（中心から見た位置）を掛ける。
     * ⚠ 模型は同じ種類の鳥で 1 つを使い回すので、骨の行列は次の鳥を描くと上書きされる。描いた直後にここで写す。
     * ⚠⚠ 本描きの直後（光る層より前）に写す。最初は描き終わり（`postRender`）で写していて、稲妻が翼の外・上へ大きくずれた。
     *   光る層の描き直し（`reRender`）が `preRender` で基準の行列を「0.6 倍に縮めた後」で取り直し、骨の行列を
     *   縮める前の大きさで上書きしていた（4.8.4 `GeoEntityRenderer.preRender` は描き直しでも基準を取り直す）。
     *   ずれにはもう 1 つ、GeckoLib の行列の誤りが重なっていた（{@link #unpolluted}）。
     */
    @Override
    public void actuallyRender(PoseStack poseStack, ShearwaterEntity animatable, BakedGeoModel model, RenderType renderType,
                               MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, float partialTick,
                               int packedLight, int packedOverlay, float red, float green, float blue, float alpha) {
        super.actuallyRender(poseStack, animatable, model, renderType, bufferSource, buffer, isReRender, partialTick, packedLight,
                packedOverlay, red, green, blue, alpha);
        if (isReRender) {
            return;
        }
        GeoBone[] bones = tracked(model);
        if (bones == null) {
            return;
        }
        double[][] points = new double[bones.length][];
        Vector3f v = new Vector3f();
        Matrix4f m = new Matrix4f();
        for (int i = 0; i < bones.length; i++) {
            if (!unpolluted(m.set(bones[i].getLocalSpaceMatrix()))) {
                return;                                   // 形の分からない行列。粒子は決まった点を使う
            }
            float[] o = ShearwaterLocators.LIVE_OFFSETS[i];
            m.transformPosition(v.set(o[0], o[1], o[2]));
            points[i] = new double[] {v.x, v.y, v.z};
        }
        animatable.setLivePoints(points);
    }

    /**
     * ⚠⚠ GeckoLib 4.8.4 の骨の行列から、余計に足された単位行列を引く（2026-09-28。飛んでいる間、稲妻が翼から数ブロック離れた）。
     * ⚠ `RenderUtils.translateMatrix` は平行移動を足すつもりで `matrix.add(new Matrix4f().m30(x).m31(y).m32(z))` としていて、
     *   `new Matrix4f()` が単位行列なので、回転の部分に単位行列が 1 つ乗る（右下の m33 も 2 になる）。GeckoLib 自身は
     *   骨の中心（点 0）しか使わないので表に出ないが、中心から離れた点を掛けると、その点の分（翼端なら 3.75）が
     *   向きに関係なくずれる。写した値で確かめた: 対角 0.739・1.577・0.727 から 1 を引くと、キットの計算の −0.226・0.587・−0.233 に近い。
     * m33 が 2 ならこの誤りとみて引き戻し、1 ならそのまま（直った版）。どちらでもなければ false（使わない）。
     */
    private static boolean unpolluted(Matrix4f m) {
        if (Math.abs(m.m33() - 2.0f) < 1.0e-3f) {
            m.m00(m.m00() - 1.0f).m11(m.m11() - 1.0f).m22(m.m22() - 1.0f).m33(1.0f);
            return true;
        }
        return Math.abs(m.m33() - 1.0f) < 1.0e-3f;
    }

    /** 点を取る骨を引き、行列を写す印を付ける。骨が 1 本でも無ければ null（粒子は決まった点を使う） */
    private GeoBone[] tracked(BakedGeoModel model) {
        if (model != this.trackedModel) {
            String[] names = ShearwaterLocators.LIVE_BONES;
            GeoBone[] bones = new GeoBone[names.length];
            for (int i = 0; i < names.length; i++) {
                bones[i] = model.getBone(names[i]).orElse(null);
                if (bones[i] == null) {
                    bones = null;
                    break;
                }
                bones[i].setTrackingMatrices(true);
            }
            this.trackedModel = model;
            this.trackedBones = bones;
        }
        return this.trackedBones;
    }
}
