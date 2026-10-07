package net.erutobusiness.erutosmobs.registry;

import net.erutobusiness.erutosmobs.ErutosMobs;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 嵐渡りのポーション（3 分）と、レッドストーンで延ばした物（8 分）。作り方は {@code brewing.PotionMix}。
 * ⚠ どちらも名前の元を "storm_crossing" にする（延ばした物も同じ名前で出る。バニラの long_ と同じ）。
 */
public final class ModPotions {
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(ForgeRegistries.POTIONS, ErutosMobs.MOD_ID);

    public static final RegistryObject<Potion> STORM_CROSSING = POTIONS.register("storm_crossing",
            () -> new Potion("storm_crossing", new MobEffectInstance(ModEffects.STORM_CROSSING.get(), 3600)));
    public static final RegistryObject<Potion> LONG_STORM_CROSSING = POTIONS.register("long_storm_crossing",
            () -> new Potion("storm_crossing", new MobEffectInstance(ModEffects.STORM_CROSSING.get(), 9600)));

    private ModPotions() {
    }
}
