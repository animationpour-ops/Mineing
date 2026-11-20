package com.omnimine.item;

import com.omnimine.data.MobKind;
import com.omnimine.server.MobPowers;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Un mob mine : un item avec un seul petit pouvoir (clic droit). */
public class MobItem extends Item {
    public final MobKind kind;

    public MobItem(MobKind kind, Properties properties) {
        super(properties);
        this.kind = kind;
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return PowerUse.use(this, (l, p, s, h) -> MobPowers.use(kind, l, p), kind.cooldown, level, player, hand);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable(getDescriptionId() + ".desc1").withStyle(ChatFormatting.LIGHT_PURPLE));
        tip.add(Component.translatable(getDescriptionId() + ".desc2").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("tooltip.omnimine.cooldown", kind.cooldown / 20).withStyle(ChatFormatting.DARK_AQUA));
    }
}
