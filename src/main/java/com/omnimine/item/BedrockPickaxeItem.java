package com.omnimine.item;

import com.omnimine.registry.ModEnchantments;
import com.omnimine.registry.ModItems;
import com.omnimine.server.StormPowers;
import com.omnimine.util.TargetUtil;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.EnchantedBookItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tiers;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Pioche en bedrock : une pioche vanilla, tres rapide et incassable. Avec l'enchantement Omniminage elle mine aussi
 * les mobs, le soleil, la lune, les eclairs et (accroupi) les structures. Clic droit : appelle un eclair.
 */
public class BedrockPickaxeItem extends PickaxeItem {
    public static final int COOLDOWN = 20 * 3;

    public BedrockPickaxeItem(Properties properties) {
        super(Tiers.NETHERITE, 1, -2.4F, properties);
    }

    @Override
    public boolean canBeDepleted() {
        return false;
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float base = super.getDestroySpeed(stack, state);
        return base > 1.0F ? base * 2.5F : base;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        return PowerUse.use(this, BedrockPickaxeItem::callLightning, COOLDOWN, level, player, hand);
    }

    private static boolean callLightning(ServerLevel level, ServerPlayer player, ItemStack stack, InteractionHand hand) {
        if (!hasOmni(stack)) {
            player.displayClientMessage(Component.translatable("message.omnimine.need_enchant")
                    .withStyle(ChatFormatting.RED), true);
            return false;
        }
        Vec3 target = TargetUtil.groundTarget(player, 70.0D);
        return StormPowers.callBolt(level, player, target);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.omnimine.bedrock_pickaxe.desc1").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.omnimine.bedrock_pickaxe.desc2").withStyle(ChatFormatting.GRAY));
        tip.add(Component.translatable("item.omnimine.bedrock_pickaxe.desc3").withStyle(ChatFormatting.DARK_AQUA));
        tip.add(Component.translatable(hasOmni(stack) ? "tooltip.omnimine.omni_on" : "tooltip.omnimine.omni_off")
                .withStyle(hasOmni(stack) ? ChatFormatting.GREEN : ChatFormatting.RED));
    }

    // ---- aides ----

    /** La pioche porte-t-elle l'enchantement Omniminage ? */
    public static boolean hasOmni(ItemStack stack) {
        return stack.getItem() instanceof BedrockPickaxeItem
                && EnchantmentHelper.getItemEnchantmentLevel(ModEnchantments.OMNI_MINING.get(), stack) > 0;
    }

    /** Le joueur tient-il la pioche enchantee en main principale ? */
    public static boolean holdsOmni(Player player) {
        return hasOmni(player.getMainHandItem());
    }

    public static ItemStack enchantedPickaxe() {
        ItemStack stack = new ItemStack(ModItems.BEDROCK_PICKAXE.get());
        stack.enchant(ModEnchantments.OMNI_MINING.get(), 1);
        return stack;
    }

    public static ItemStack enchantedBook() {
        return EnchantedBookItem.createForEnchantment(new EnchantmentInstance(ModEnchantments.OMNI_MINING.get(), 1));
    }
}
