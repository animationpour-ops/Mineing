package com.omnimine.server;

import com.omnimine.Omnimine;
import com.omnimine.item.LightningArmorItem;
import com.omnimine.net.VfxPacket;
import com.omnimine.net.VfxType;
import com.omnimine.registry.ModItems;
import com.omnimine.registry.ModSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Eclairs : appel de la foudre par la pioche, et les trois pouvoirs de l'armure d'eclair.
 * Casque : foudre en chaine a chaque coup. Plastron : decharge statique quand on te frappe.
 * Jambieres : course foudroyante.
 */
@Mod.EventBusSubscriber(modid = Omnimine.MODID)
public final class StormPowers {
    public static final int COLOR = 0xBFE8FF;
    private static final Map<UUID, Long> NEXT_CHAIN = new HashMap<>();
    private static final Map<UUID, Long> NEXT_NOVA = new HashMap<>();

    // ------------------------------------------------------------ pioche : appeler un eclair

    /** Fait tomber un eclair (capture et fige par la pioche si le joueur la tient). */
    public static boolean callBolt(ServerLevel level, ServerPlayer p, Vec3 target) {
        LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
        if (bolt == null) {
            return false;
        }
        bolt.moveTo(target.x, target.y, target.z);
        bolt.setCause(p);
        level.addFreshEntity(bolt);
        return true;
    }

    /** La prochaine piece d'armure donnee par un eclair mine : une piece qu'on n'a pas encore, sinon au hasard. */
    public static Item nextPiece(ServerPlayer p) {
        List<Item> all = List.of(ModItems.LIGHTNING_HELMET.get(), ModItems.LIGHTNING_CHESTPLATE.get(),
                ModItems.LIGHTNING_LEGGINGS.get());
        List<Item> missing = new ArrayList<>();
        for (Item it : all) {
            boolean has = false;
            for (ItemStack s : p.getInventory().items) {
                has |= s.is(it);
            }
            for (ItemStack s : p.getInventory().armor) {
                has |= s.is(it);
            }
            if (!has) {
                missing.add(it);
            }
        }
        List<Item> pool = missing.isEmpty() ? all : missing;
        return pool.get(p.getRandom().nextInt(pool.size()));
    }

    // ------------------------------------------------------- casque : foudre en chaine

    @SubscribeEvent
    public static void onHurt(LivingHurtEvent event) {
        DamageSource src = event.getSource();
        LivingEntity victim = event.getEntity();
        if (!(victim.level() instanceof ServerLevel level)) {
            return;
        }
        // casque : un coup au corps a corps declenche la foudre en chaine
        if (src.is(DamageTypes.PLAYER_ATTACK) && src.getEntity() instanceof ServerPlayer p
                && LightningArmorItem.wears(p, EquipmentSlot.HEAD)
                && level.getGameTime() >= NEXT_CHAIN.getOrDefault(p.getUUID(), 0L)) {
            NEXT_CHAIN.put(p.getUUID(), level.getGameTime() + 14L);
            event.setAmount(event.getAmount() + 6.0F);
            chain(level, p, victim);
        }
        // plastron : decharge statique quand le porteur est frappe par une creature
        if (victim instanceof ServerPlayer wearer && LightningArmorItem.wears(wearer, EquipmentSlot.CHEST)
                && src.getEntity() instanceof LivingEntity && src.getEntity() != wearer
                && level.getGameTime() >= NEXT_NOVA.getOrDefault(wearer.getUUID(), 0L)) {
            NEXT_NOVA.put(wearer.getUUID(), level.getGameTime() + 50L);
            nova(level, wearer);
        }
    }

    private static Vec3 center(LivingEntity e) {
        return e.position().add(0, e.getBbHeight() * 0.5D, 0);
    }

    private static void chain(ServerLevel level, ServerPlayer p, LivingEntity first) {
        Vec3 ground = first.position();
        VfxPacket.of(VfxType.SMITE, ground).param(0.7F).color(COLOR).duration(26).send(level);
        ModSounds.play(level, ground, ModSounds.THUNDER, 3.0F, 1.2F);
        zap(level, p, first, 8.0F);
        List<LivingEntity> hit = new ArrayList<>();
        hit.add(first);
        LivingEntity cur = first;
        for (int i = 0; i < 5; i++) {
            LivingEntity next = null;
            double best = 1.0E9D;
            for (LivingEntity e : Abilities.around(level, p, center(cur), 9.0D)) {
                double d = e.distanceToSqr(cur);
                if (!hit.contains(e) && d < best) {
                    best = d;
                    next = e;
                }
            }
            if (next == null) {
                break;
            }
            hit.add(next);
            LivingEntity a = cur;
            LivingEntity b = next;
            ServerEffects.delay(level, 3 * (i + 1), () -> {
                VfxPacket.of(VfxType.ARC, center(a)).to(center(b)).color(COLOR).duration(12).send(level);
                ModSounds.play(level, center(b), ModSounds.ZAP, 2.0F, 1.0F + level.random.nextFloat() * 0.4F);
                zap(level, p, b, 9.0F);
            });
            cur = next;
        }
    }

    private static void zap(ServerLevel level, ServerPlayer p, LivingEntity e, float damage) {
        if (!e.isAlive()) {
            return;
        }
        Abilities.hurt(level, e, p, damage);
        e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 40, 2));
    }

    // ------------------------------------------------------- plastron : decharge statique

    private static void nova(ServerLevel level, ServerPlayer wearer) {
        Vec3 c = wearer.position().add(0, 1.0D, 0);
        VfxPacket.of(VfxType.NOVA, c).follow(wearer).color(COLOR).param(8.0F).duration(30).send(level);
        ModSounds.play(level, c, ModSounds.THUNDER, 4.0F, 1.4F);
        ModSounds.play(level, c, ModSounds.PULSE, 3.0F, 1.6F);
        for (LivingEntity e : Abilities.around(level, wearer, c, 8.0D)) {
            Abilities.hurt(level, e, wearer, 16.0F);
            Abilities.knock(e, c, 2.0D, 0.7D);
            e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 50, 2));
        }
    }

    /** Le plastron rend insensible a la foudre. */
    @SubscribeEvent
    public static void onAttack(LivingAttackEvent event) {
        if (event.getSource().is(DamageTypes.LIGHTNING_BOLT) && event.getEntity() instanceof ServerPlayer p
                && LightningArmorItem.wears(p, EquipmentSlot.CHEST)) {
            event.setCanceled(true);
            p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 200, 0, true, false));
            p.clearFire();
        }
    }

    // ------------------------------------------------------ jambieres : course foudroyante

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer sp)
                || !LightningArmorItem.wears(sp, EquipmentSlot.LEGS) || !sp.isSprinting()) {
            return;
        }
        ServerLevel level = sp.serverLevel();
        int tick = sp.tickCount;
        if (tick % 10 == 0) {
            sp.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, 30, 2, true, false));
        }
        Vec3 dir = sp.getLookAngle().multiply(1.0D, 0.0D, 1.0D);
        if (tick % 3 == 0) {
            VfxPacket.of(VfxType.ZAP, sp.position()).to(dir).color(COLOR).duration(16).send(level);
        }
        if (tick % 6 == 0) {
            ModSounds.play(level, sp.position(), ModSounds.ZAP, 1.2F, 0.9F + level.random.nextFloat() * 0.5F);
        }
        if (tick % 4 == 0) {
            Vec3 c = sp.position().add(0, 0.9D, 0);
            for (LivingEntity e : Abilities.around(level, sp, c, 2.8D)) {
                Abilities.hurt(level, e, sp, 7.0F);
                Abilities.knock(e, sp.position(), 1.1D, 0.4D);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 30, 1));
            }
        }
        // de temps en temps la foudre tombe sur l'ennemi le plus proche
        if (tick % 30 == 0) {
            LivingEntity target = null;
            double best = 1.0E9D;
            for (LivingEntity e : Abilities.around(level, sp, sp.position(), 14.0D)) {
                double d = e.distanceToSqr(sp);
                if (d < best) {
                    best = d;
                    target = e;
                }
            }
            if (target != null) {
                VfxPacket.of(VfxType.MINI, target.position()).color(COLOR).duration(16).send(level);
                ModSounds.play(level, target.position(), ModSounds.THUNDER, 2.0F, 1.5F);
                zap(level, sp, target, 12.0F);
            }
        }
    }

    private StormPowers() {}
}
