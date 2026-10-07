package net.erutobusiness.erutosmobs.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraftforge.common.ForgeMod;

/**
 * 嵐渡り。嵐の海の主が並んで飛ぶ人へ分ける力。
 *   雷に打たれない … {@code ModEvents.onLightning} が雷の当たりを取り消す（火も傷も無い）
 *   泳ぎが速い     … 泳ぐ速さ ×1.5（{@link #addSwimSpeed}）
 *   ボートが速い   … 水の上で前へ漕ぐ間、船の速さの上限が約 2 割上がる（{@code client.BoatBoost}。
 *                    人が漕ぐ船は漕ぐ人の画面の側が動かすので、そちらで足す）
 */
public class StormCrossingEffect extends MobEffect {
    private static final String SWIM_UUID = "6f1d2c3e-8a47-4f0b-9c5e-2b7a1e9d4c61";

    public StormCrossingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0x2C60BE);
    }

    /**
     * 泳ぐ速さの上乗せを付ける。⚠ 効果を登録する時点では Forge の属性（泳ぐ速さ）がまだ引けないことがあるので、
     * 全部の登録が済んだ後（共通の準備）で呼ぶ。
     */
    public void addSwimSpeed() {
        this.addAttributeModifier(ForgeMod.SWIM_SPEED.get(), SWIM_UUID, 0.5, AttributeModifier.Operation.MULTIPLY_TOTAL);
    }
}
