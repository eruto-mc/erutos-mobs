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
    }
}
