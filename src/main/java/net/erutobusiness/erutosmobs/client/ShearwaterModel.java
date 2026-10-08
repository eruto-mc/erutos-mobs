package net.erutobusiness.erutosmobs.client;

import net.erutobusiness.erutosmobs.ErutosMobs;
import net.erutobusiness.erutosmobs.entity.ShearwaterEntity;
import net.minecraft.client.renderer.RenderType;
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

    /**
     * ⚠⚠ 裏を向いた面を描かない描き方（2026-09-28・ユーザー「遠くから見るとチラついたけど、近くから見るとチラつかないね」）。
     * ⚠ GeckoLib の既定は裏も描く `entityCutoutNoCull`（4.8.4 のソース `GeoModel.getRenderType`）。羽の板は表と裏が
     *   0.1 しか離れておらず（押し出し 0.05）、奥行きの細かさが距離の 2 乗で粗くなる遠くでは、上面の紺と下面の白が
     *   取り合っていた（近い面 0.05・24 bit で見積もると 55 ブロックより先）。裏を描かなければ取り合う相手が無い。
     *   板は厚み 0 でも上と下の 2 面を持つので、どちらから見ても 1 面は描かれる。
     */
    @Override
    public RenderType getRenderType(ShearwaterEntity animatable, ResourceLocation texture) {
        return RenderType.entityCutout(texture);
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
     * ⚠⚠ 足した後に骨の「このコマで動かした」の印を消す（{@link #add}）。消さないと足し算が毎コマ積み上がる
     *   （2026-10-08・ユーザー「新しい晴れた昼、なんか仰向けだけどどうした？」。描くたびの骨の値を書き出すと、羽ばたく間の
     *   胴の上下の回転が 1880° → 2358° → 7264° と 1 コマに傾きの分ずつ増え、胴が 1 秒に約 3 回、前後にぐるぐる回って描かれていた）。
     *   GeckoLib 4.8.4 の原文: `GeoBone.setRotX` は印を付ける（`markRotationAsChanged`）。`AnimationProcessor.tickAnimation` は、
     *   今の動きに回転のキーが無い骨を、印が無いときだけ元の値へ戻し（`if (!bone.hasRotationChanged())`）、最後に印を消してから
     *   ここが呼ばれる。ここで付けた印は次のコマまで残るので、キーの無い骨（羽ばたき swim と鳴き cry の胴、被弾 hurt の首）は
     *   戻されず、そこへまた足していた。前は「毎コマ上書きされるので積み上がらない」と思い込んでいた。
     */
    @Override
    public void setCustomAnimations(ShearwaterEntity animatable, long instanceId, AnimationState<ShearwaterEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);
        int st = animatable.getState();
        if (animatable.isDeadOrDying()) {
            return;
        }
        EntityModelData data = animationState.getData(DataTickets.ENTITY_MODEL_DATA);
        if (data != null && st != ShearwaterEntity.SLEEP && st != ShearwaterEntity.DIVE) {
            float yaw = Mth.clamp(data.netHeadYaw(), -MAX_YAW, MAX_YAW) * Mth.DEG_TO_RAD;
            float pitch = Mth.clamp(data.headPitch(), -MAX_PITCH, MAX_PITCH) * Mth.DEG_TO_RAD;
            add("neck2", 0.0f, yaw / 3.0f);
            add("neck3", pitch * 0.5f, yaw / 3.0f);
            add("neck4", pitch * 0.5f, yaw / 3.0f);
        }
        // 上り下りの傾き（正で上り＝鼻先が上）。ShearwaterEntity#tick が、進んで飛ぶ間だけ位置の変化から決め、ほかの状態では
        // 4 tick ほどで 0 へ戻す。⚠ どの状態でも足す——前は飛ぶ状態のときだけ足していて、急降下・離陸・水面へ移った瞬間に
        // 最大 35° が 1 コマで戻っていた
        float tilt = Mth.lerp(animationState.getPartialTick(), animatable.tiltO, animatable.tilt);
        if (tilt != 0.0f) {
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
        // 足した分は今のコマだけのもの。印を消して、次のコマで GeckoLib が動き（か元の値）から組み直せるようにする
        bone.resetStateChanges();
    }
}
