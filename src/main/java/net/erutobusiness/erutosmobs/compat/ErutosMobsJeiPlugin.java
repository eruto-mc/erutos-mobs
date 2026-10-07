package net.erutobusiness.erutosmobs.compat;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.constants.VanillaTypes;
import mezz.jei.api.registration.IRecipeRegistration;
import net.erutobusiness.erutosmobs.ErutosMobs;
import net.erutobusiness.erutosmobs.registry.ModItems;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

/**
 * JEI（レシピを見る MOD）が在るときだけ読まれる（JEI が {@link JeiPlugin} の付いたクラスを探して呼ぶ。無ければ誰も読まない）。
 * 風切羽とスポーンエッグに説明の頁を足す。醸造の 2 段は Forge の醸造台の登録から JEI が自分で拾う。
 */
@JeiPlugin
public class ErutosMobsJeiPlugin implements IModPlugin {
    private static final ResourceLocation UID = new ResourceLocation(ErutosMobs.MOD_ID, "jei");

    @Override
    public ResourceLocation getPluginUid() {
        return UID;
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registration.addIngredientInfo(new ItemStack(ModItems.SHEARWATER_FEATHER.get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.erutosmobs.shearwater_feather.info"));
        registration.addIngredientInfo(new ItemStack(ModItems.SHEARWATER_SPAWN_EGG.get()), VanillaTypes.ITEM_STACK,
                Component.translatable("jei.erutosmobs.shearwater.info"));
    }
}
