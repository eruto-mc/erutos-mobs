package net.erutobusiness.erutosmobs.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import javax.annotation.Nullable;
import java.util.List;

/**
 * ミズナギドリの風切羽。倒すと必ず 1 枚落とし、船に並んで飛び終えた鳥が信用している人へ 1 枚くれる。
 * 奇妙なポーションに入れると嵐渡りのポーションになる。絵は風切羽の粒子と同じ（mc-model-kit が塗る）。
 */
public class ShearwaterFeatherItem extends Item {
    public ShearwaterFeatherItem() {
        super(new Item.Properties().rarity(Rarity.RARE));
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("item.erutosmobs.shearwater_feather.desc").withStyle(ChatFormatting.GRAY));
    }
}
