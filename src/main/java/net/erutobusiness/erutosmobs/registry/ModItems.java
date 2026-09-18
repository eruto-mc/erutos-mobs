package net.erutobusiness.erutosmobs.registry;

import net.erutobusiness.erutosmobs.ErutosMobs;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeSpawnEggItem;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public final class ModItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ErutosMobs.MOD_ID);

    /** 卵の色は嵐の青（体）と琥珀（端の光）。 */
    public static final RegistryObject<Item> SHEARWATER_SPAWN_EGG = ITEMS.register("shearwater_spawn_egg",
            () -> new ForgeSpawnEggItem(ModEntities.SHEARWATER, 0x2C60BE, 0xF0C060, new Item.Properties()));

    private ModItems() {
    }
}
