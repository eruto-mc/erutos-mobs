package net.erutobusiness.erutosmobs;

import net.erutobusiness.erutosmobs.entity.ShearwaterEntity;
import net.erutobusiness.erutosmobs.registry.ModEntities;
import net.erutobusiness.erutosmobs.registry.ModItems;
import net.erutobusiness.erutosmobs.registry.ModSounds;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

/**
 * Eruto's Mobs — 嵐の側の生き物たち。第一号はミズナギドリ。
 *
 * 模型と動きは mc-model-kit が作り、GeckoLib の JSON として resources に写す
 * （`assets/erutosmobs/geo`・`animations`・`textures/entity`）。ここに手で描いた模型は無い。
 */
@Mod(ErutosMobs.MOD_ID)
public class ErutosMobs {
    public static final String MOD_ID = "erutosmobs";

    public ErutosMobs() {
        IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();
        ModEntities.ENTITIES.register(bus);
        ModItems.ITEMS.register(bus);
        ModSounds.SOUNDS.register(bus);
        bus.addListener(this::onAttributes);
        bus.addListener(this::onSpawnPlacements);
        bus.addListener(this::onCreativeTab);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, ErutosMobsConfig.SPEC, "erutosmobs-common.toml");
    }

    private void onAttributes(EntityAttributeCreationEvent event) {
        event.put(ModEntities.SHEARWATER.get(), ShearwaterEntity.createAttributes().build());
    }

    private void onSpawnPlacements(SpawnPlacementRegisterEvent event) {
        // 海の水面に湧く（イルカと同じ IN_WATER）。稀さは ShearwaterEntity.checkShearwaterSpawn が持つ
        event.register(ModEntities.SHEARWATER.get(), SpawnPlacements.Type.IN_WATER,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, ShearwaterEntity::checkShearwaterSpawn,
                SpawnPlacementRegisterEvent.Operation.REPLACE);
    }

    private void onCreativeTab(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.SPAWN_EGGS) {
            event.accept(ModItems.SHEARWATER_SPAWN_EGG);
        }
    }
}
