package net.erutobusiness.erutosmobs.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.erutobusiness.erutosmobs.ErutosMobs;
import net.erutobusiness.erutosmobs.entity.ShearwaterEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

/**
 * 光る層。琥珀の端・目・顔のハネの泡の線（キットが `shearwater_glow.png` に塗る）を、暗さと嵐で光らせる。
 *
 * 前例（jar を読んだ一次の値）: タイヨウチョウは `sunbird_glow.png` を目の描き方（eyes）で重ね、明るさも常に最大。
 * Cobblemon のファイヤー・ホウオウは体の光る層、サンダー・フリーザー・ルギアは目だけ。
 * こちらは嵐の側なので、昼は弱く（0.35）、夜で 0.8、雷雨でさらに強く、雷を受けて帯電した間と
 * 鳴いた直後は最大近くまで上がる。
 * ⚠ 昼は前 0.06 で、晴れた昼はほぼ光らなかった。2026-10-08・ユーザー「なんかあんまスクショに伝説感が無いなぁ」で
 *   0.35 へ上げた（翼端の琥珀と目が、晴れた昼にも明かりより明るく見える）。夜の強さは変えていない。
 * ⚠ eyes は足し算で重ねるので、頂点の色（k,k,k）がそのまま光の強さになる。光らない画素の色は 0（キット側でそろえる）。
 */
public class ShearwaterGlowLayer extends GeoRenderLayer<ShearwaterEntity> {
    private static final ResourceLocation GLOW = new ResourceLocation(ErutosMobs.MOD_ID, "textures/entity/shearwater_glow.png");
    /** 晴れた昼の光の強さ。夜（暗さ 1）は {@link #NIGHT} */
    private static final float DAY = 0.35f;
    private static final float NIGHT = 0.8f;

    public ShearwaterGlowLayer(GeoRenderer<ShearwaterEntity> renderer) {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, ShearwaterEntity animatable, BakedGeoModel bakedModel, RenderType renderType,
                       MultiBufferSource bufferSource, VertexConsumer buffer, float partialTick, int packedLight,
                       int packedOverlay) {
        float k = intensity(animatable, partialTick);
        if (k <= 0.02f) {
            return;
        }
        RenderType glow = RenderType.eyes(GLOW);
        getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, glow, bufferSource.getBuffer(glow),
                partialTick, LightTexture.FULL_SKY, OverlayTexture.NO_OVERLAY, k, k, k, 1.0f);
    }

    /** 光の強さ 0〜1。 */
    static float intensity(ShearwaterEntity bird, float partialTick) {
        Level level = bird.level();
        float dark = 0.0f;
        if (level instanceof ClientLevel cl) {
            float sky = cl.getSkyDarken(partialTick);            // 昼 1.0 〜 夜 0.2（雨と雷でも下がる）
            dark = Mth.clamp((1.0f - sky) / 0.8f, 0.0f, 1.0f);
        }
        float thunder = level.getThunderLevel(partialTick);
        float t = bird.tickCount + partialTick;
        float k = DAY + (NIGHT - DAY) * dark + 0.25f * thunder;
        if (thunder > 0.5f) {
            // 雷雨: 雲の中の稲光のように、ときどき強く瞬く
            float flash = Mth.sin(t * 0.37f) * Mth.sin(t * 0.113f + 1.3f);
            if (flash > 0.8f) {
                k += 0.4f * (flash - 0.8f) / 0.2f;
            }
        }
        if (bird.isCharged()) {
            k = Math.max(k, 0.9f) + 0.1f * Math.abs(Mth.sin(t * 0.9f));
        }
        k += bird.cryGlow(partialTick);
        return Mth.clamp(k, 0.0f, 1.0f);
    }
}
