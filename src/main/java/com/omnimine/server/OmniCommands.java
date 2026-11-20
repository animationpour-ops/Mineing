package com.omnimine.server;

import com.omnimine.Omnimine;
import com.omnimine.item.BedrockPickaxeItem;
import com.omnimine.registry.ModItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.registries.RegistryObject;

/** Commandes pratiques pour tourner la video (niveau 2 / operateur). */
@Mod.EventBusSubscriber(modid = Omnimine.MODID)
public final class OmniCommands {

    @SubscribeEvent
    public static void onCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("omnimine")
                .requires(src -> src.hasPermission(2))
                .then(Commands.literal("kit").executes(ctx -> kit(ctx.getSource(), false)))
                .then(Commands.literal("tout").executes(ctx -> kit(ctx.getSource(), true)))
                .then(Commands.literal("orage").executes(ctx -> storm(ctx.getSource())))
                .then(Commands.literal("ciel").executes(ctx -> sky(ctx.getSource())))
                .then(Commands.literal("soleil").executes(ctx -> give(ctx.getSource(), ModItems.SUN_SWORD.get(), 1)))
                .then(Commands.literal("lune").executes(ctx -> give(ctx.getSource(), ModItems.MOON_BOOTS.get(), 1)))
                .then(Commands.literal("livre").executes(ctx -> giveStack(ctx.getSource(), BedrockPickaxeItem.enchantedBook()))));
    }

    private static int giveStack(CommandSourceStack src, ItemStack stack) {
        try {
            ServerPlayer p = src.getPlayerOrException();
            ItemHandlerHelper.giveItemToPlayer(p, stack);
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("Il faut etre un joueur."));
            return 0;
        }
    }

    private static int give(CommandSourceStack src, Item item, int count) {
        return giveStack(src, new ItemStack(item, count));
    }

    /** kit : la pioche enchantee et le livre. tout : en plus, tous les items du mod (pour tester ou lancer la video). */
    private static int kit(CommandSourceStack src, boolean all) {
        try {
            ServerPlayer p = src.getPlayerOrException();
            ItemHandlerHelper.giveItemToPlayer(p, BedrockPickaxeItem.enchantedPickaxe());
            ItemHandlerHelper.giveItemToPlayer(p, BedrockPickaxeItem.enchantedBook());
            if (all) {
                give(src, ModItems.SUN_SWORD.get(), 1);
                give(src, ModItems.MOON_BOOTS.get(), 1);
                give(src, ModItems.LIGHTNING_HELMET.get(), 1);
                give(src, ModItems.LIGHTNING_CHESTPLATE.get(), 1);
                give(src, ModItems.LIGHTNING_LEGGINGS.get(), 1);
                for (RegistryObject<Item> r : ModItems.MOBS.values()) {
                    give(src, r.get(), 1);
                }
                for (RegistryObject<Item> r : ModItems.STRUCTURES.values()) {
                    give(src, r.get(), 1);
                }
            }
            return 1;
        } catch (Exception e) {
            src.sendFailure(Component.literal("Il faut etre un joueur."));
            return 0;
        }
    }

    /** Orage : de quoi capturer des eclairs avec la pioche (ou clic droit avec la pioche). */
    private static int storm(CommandSourceStack src) {
        ServerLevel level = src.getLevel();
        level.setWeatherParameters(0, 6000, true, true);
        try {
            ServerPlayer p = src.getPlayerOrException();
            Vec3 look = p.getLookAngle();
            Vec3 target = p.position().add(look.x * 14.0D, 0.0D, look.z * 14.0D);
            StormPowers.callBolt(level, p, target);
        } catch (Exception e) {
            // pas de joueur : l'orage seul suffit
        }
        return 1;
    }

    private static int sky(CommandSourceStack src) {
        SkyBodies.restoreNow();
        return 1;
    }

    private OmniCommands() {}
}
