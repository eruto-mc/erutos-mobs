package net.erutobusiness.erutosmobs.client;

import net.erutobusiness.erutosmobs.entity.ShearwaterEntity;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class ShearwaterRenderer extends GeoEntityRenderer<ShearwaterEntity> {
    /**
     * 模型は翼幅 322（1/16 ブロック単位）＝約 20 ブロック。伝説として大きすぎない 0.6 倍
     * （翼幅 約 12 ブロック。Alex's Mobs の Sunbird は 13 ブロック）。
     */
    public static final float SCALE = 0.6f;

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
}
