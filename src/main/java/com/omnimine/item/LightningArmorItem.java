package com.omnimine.item;

import com.omnimine.Omnimine;
import java.util.List;
import javax.annotation.Nullable;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * Armure d'eclair (modele vanilla, dessin anime d'orage). Un pouvoir par piece :
 * casque = foudre en chaine, plastron = decharge statique, jambieres = course foudroyante.
 */
public class LightningArmorItem extends ArmorItem {
    /** Nombre d'images de l'animation du dessin porte (textures lightning_layer_N_fK.png). */
    public static final int FRAMES = 24;

    public LightningArmorItem(Type type, Properties properties) {
        super(OmniArmorMaterials.LIGHTNING, type, properties);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }

    @Override
    public boolean canBeDepleted() {
        return false;
    }

    /** Le dessin de l'armure est anime : on choisit l'image selon l'age de l'entite. */
    @Override
    public String getArmorTexture(ItemStack stack, Entity entity, EquipmentSlot slot, String type) {
        int frame = (entity.tickCount / 2) % FRAMES;
        int layer = slot == EquipmentSlot.LEGS ? 2 : 1;
        return Omnimine.MODID + ":textures/models/armor/lightning_layer_" + layer + "_f" + frame + ".png";
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tip, TooltipFlag flag) {
        tip.add(Component.translatable(getDescriptionId() + ".desc1").withStyle(ChatFormatting.YELLOW));
        tip.add(Component.translatable(getDescriptionId() + ".desc2").withStyle(ChatFormatting.GRAY));
    }

    public static boolean wears(LivingEntity e, EquipmentSlot slot) {
        return e.getItemBySlot(slot).getItem() instanceof LightningArmorItem;
    }
}
