package net.erutobusiness.erutosmobs.registry;

import net.erutobusiness.erutosmobs.ErutosMobs;
import net.erutobusiness.erutosmobs.effect.StormCrossingEffect;
import net.erutobusiness.erutosmobs.effect.StormWrathEffect;
import net.minecraft.world.effect.MobEffect;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, ErutosMobs.MOD_ID);

    /** 嵐渡り: 雷に打たれない・泳ぎとボートが速い（ミズナギドリが並んで飛ぶ間と、羽根の薬） */
    public static final RegistryObject<MobEffect> STORM_CROSSING = EFFECTS.register("storm_crossing", StormCrossingEffect::new);
    /** 嵐の怒り: ミズナギドリを傷つけた人。近くに見た目だけの雷が落ち、鳥は付いてこない・魚も取らない */
    public static final RegistryObject<MobEffect> STORM_WRATH = EFFECTS.register("storm_wrath", StormWrathEffect::new);

    private ModEffects() {
    }
}
