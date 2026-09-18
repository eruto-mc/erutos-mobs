package net.erutobusiness.erutosmobs.registry;

import net.erutobusiness.erutosmobs.ErutosMobs;
import net.erutobusiness.erutosmobs.entity.ShearwaterEntity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModEntities {
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, ErutosMobs.MOD_ID);

    /** ミズナギドリ。水の生き物の枠（WATER_CREATURE）で海面に湧き、飛び立つ。 */
    public static final RegistryObject<EntityType<ShearwaterEntity>> SHEARWATER = ENTITIES.register("shearwater",
            () -> EntityType.Builder.of(ShearwaterEntity::new, MobCategory.WATER_CREATURE)
                    .sized(3.0f, 1.4f)
                    .clientTrackingRange(12)
                    .updateInterval(3)
                    .build("shearwater"));

    private ModEntities() {
    }
}
