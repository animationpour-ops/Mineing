package com.omnimine.item;

import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Bottes lunaires (modele des bottes vanilla). Pouvoir 1 : double saut lunaire. Pouvoir 2 : impact lunaire. */
public class LunarBootsItem extends ArmorItem {

    public LunarBootsItem(Properties properties) {
        super(OmniArmorMaterials.MOON, Type.BOOTS, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canBeDepleted() {
        return false;
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable("item.omnimine.moon_boots.desc1").withStyle(ChatFormatting.AQUA));
        tip.add(Component.translatable("item.omnimine.moon_boots.desc2").withStyle(ChatFormatting.AQUA));
    }

    public static boolean wornBy(LivingEntity e) {
        return e.getItemBySlot(EquipmentSlot.FEET).getItem() instanceof LunarBootsItem;
    }
}
