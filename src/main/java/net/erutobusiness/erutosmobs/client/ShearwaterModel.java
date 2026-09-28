package net.erutobusiness.erutosmobs.client;

import net.erutobusiness.erutosmobs.ErutosMobs;
import net.erutobusiness.erutosmobs.entity.ShearwaterEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import software.bernie.geckolib.constant.DataTickets;
import software.bernie.geckolib.core.animatable.model.CoreGeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;
import software.bernie.geckolib.model.data.EntityModelData;

/**
 * 模型・絵・動きの在り処。中身は mc-model-kit の書き出し（`ship_to_mod.py` が写す）。
 *
 * 動きの上に、その場の状況で決まる 2 つを足す（{@link #setCustomAnimations}）。どちらも参考にした 2 体が持っている:
 *   ルギア（Cobblemon 1.8.1 の poser）  … `q.look('neck'…'neck4', 'head_ai')` と `q.pitch_tilt('body')`
 *   タイヨウチョウ（Alex's Mobs 1.22.9） … `faceTarget(yaw, pitch, 1, neck, head)` と、上下の速さで胴を傾ける `birdPitch`
 */
public class ShearwaterModel extends GeoModel<ShearwaterEntity> {
    private static final ResourceLocation MODEL = new ResourceLocation(ErutosMobs.MOD_ID, "geo/shearwater.geo.json");
    private static final ResourceLocation TEXTURE = new ResourceLocation(ErutosMobs.MOD_ID, "textures/entity/shearwater.png");
    private static final ResourceLocation ANIMATIONS = new ResourceLocation(ErutosMobs.MOD_ID, "animations/shearwater.animation.json");

    /** 首を回せる限り（度）。首の 3 節で等分する（1 節 25°） */
    private static final float MAX_YAW = 75.0f;
    /** 首を上下に振れる限り（度）。後ろの 2 節で等分する */
    private static final float MAX_PITCH = 30.0f;

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

    /**
     * 首で相手を見る分と、上り下りで胴が傾く分を、動きの値に足す。
     *
     * 向きは GeckoLib 4.8.4 の原文に合わせた（勘で符号を決めない）:
     *   `GeoEntityRenderer` は `new EntityModelData(…, -netHeadYaw, -headPitch)` と符号を返して渡し、
     *   `DefaultedEntityGeoModel` はその値を `head.setRotX(headPitch * DEG_TO_RAD)` / `setRotY(netHeadYaw * DEG_TO_RAD)` と
     *   そのまま入れる。つまり GeckoLib の中では rotX が正で上を向く。
     * 首の 4 節（neck〜neck4）は休めの回転が 0 なので、同じ足し方で向きが合う。
     * ⚠ 頭（head）は休めで [75, 0, 180] 回っているので触らない（そこへ足すと軸が曲がる）。
     * ⚠ GeckoLib は毎コマ、全部の骨を動き（か初期値）で上書きしてからここを呼ぶので、足しても積み上がらない。
     */
    @Override
    public void setCustomAnimations(ShearwaterEntity animatable, long instanceId, AnimationState<ShearwaterEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        int st = animatable.getState();
        if (animatable.isDeadOrDying() || st == ShearwaterEntity.SLEEP || st == ShearwaterEntity.DIVE) {
            return;
        }
        EntityModelData data = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (data != null) {
            float yaw = Mth.clamp(data.netHeadYaw(), -MAX_YAW, MAX_YAW) * Mth.DEG_TO_RAD;
            float pitch = Mth.clamp(data.headPitch(), -MAX_PITCH, MAX_PITCH) * Mth.DEG_TO_RAD;
            add("neck2", 0.0f, yaw / 3.0f);
            add("neck3", pitch * 0.5f, yaw / 3.0f);
            add("neck4", pitch * 0.5f, yaw / 3.0f);
        }
        if (st == ShearwaterEntity.FLY || st == ShearwaterEntity.GLIDE || st == ShearwaterEntity.HOVER) {
            // 正で上り（鼻先が上）。ShearwaterEntity#tick が位置の変化から決めて、なめらかにしている
            float tilt = Mth.lerp(animationState.getPartialTick(), animatable.tiltO, animatable.tilt);
            add("body", tilt * Mth.DEG_TO_RAD, 0.0f);
        }
    }

    private void add(String boneName, float rotX, float rotY) {
        CoreGeoBone bone = getAnimationProcessor().getBone(boneName);
        if (bone == null) {
            return;
        }
        if (rotX != 0.0f) {
            bone.setRotX(bone.getRotX() + rotX);
        }
        if (rotY != 0.0f) {
            bone.setRotY(bone.getRotY() + rotY);
        }
    }
}
