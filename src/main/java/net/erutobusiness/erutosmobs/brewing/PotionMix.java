package net.erutobusiness.erutosmobs.brewing;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.item.alchemy.PotionUtils;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraftforge.common.brewing.IBrewingRecipe;

/**
 * 醸造台の 1 段（ある薬 ＋ 材料 → 別の薬）。ふつうの・スプラッシュ・残留のどれでも、形はそのままで中身だけ替える。
 * ⚠ Forge の `BrewingRecipe` に薬の瓶を `Ingredient.of` で渡すと、中身（NBT）を見ずに水の瓶でも通るので、自分で見る。
 */
public class PotionMix implements IBrewingRecipe {
    private final Potion from;
    private final Ingredient ingredient;
    private final Potion to;

    public PotionMix(Potion from, Ingredient ingredient, Potion to) {
        this.from = from;
        this.ingredient = ingredient;
        this.to = to;
    }

    @Override
    public boolean isInput(ItemStack input) {
        return (input.is(Items.POTION) || input.is(Items.SPLASH_POTION) || input.is(Items.LINGERING_POTION))
                && PotionUtils.getPotion(input) == this.from;
    }

    @Override
    public boolean isIngredient(ItemStack ingredient) {
        return this.ingredient.test(ingredient);
    }

    @Override
    public ItemStack getOutput(ItemStack input, ItemStack ingredient) {
        if (!isInput(input) || !isIngredient(ingredient)) {
            return ItemStack.EMPTY;
        }
        return PotionUtils.setPotion(new ItemStack(input.getItem()), this.to);
    }
}
