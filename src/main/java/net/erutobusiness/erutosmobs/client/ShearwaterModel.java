package net.erutobusiness.erutosmobs.client;

import net.erutobusiness.erutosmobs.ErutosMobs;
import net.erutobusiness.erutosmobs.entity.ShearwaterEntity;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

/** 模型・絵・動きの在り処。中身は mc-model-kit の書き出し（`ship_to_mod.py` が写す）。 */
public class ShearwaterModel extends GeoModel<ShearwaterEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation(ErutosMobs.MOD_ID, "geo/shearwater.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(ErutosMobs.MOD_ID, "textures/entity/shearwater.png");
    private static final ResourceLocation ANIMATIONS = new ResourceLocation(ErutosMobs.MOD_ID, "animations/shearwater.animation.json");

    @Override
    public ResourceLocation getModelResource(ShearwaterEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(ShearwaterEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(ShearwaterEntity animatable) {
        return ANIMATIONS;
    }
}
