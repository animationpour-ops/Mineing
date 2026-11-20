package com.omnimine.server;

import com.omnimine.Omnimine;
import com.omnimine.OmniConfig;
import com.omnimine.item.BedrockPickaxeItem;
import com.omnimine.net.VfxPacket;
import com.omnimine.net.VfxType;
import com.omnimine.registry.ModSounds;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * Eclairs captures : quand la foudre tombe pres d'un joueur qui tient la pioche (enchantee), l'eclair est fige dans
 * le ciel. On peut le miner ; s'il n'est pas mine a temps il frappe pour de vrai.
 */
@Mod.EventBusSubscriber(modid = Omnimine.MODID)
public final class FrozenBolts {
    public static final double HEIGHT = 96.0D;
    public static final String TAG_REAL = "omnimine_real_bolt";

    public static final class Bolt {
        public final int id;
        public final int seed;
        public final Vec3 base;
        public final long expiry;
        public final UUID cause;

        Bolt(int id, Vec3 base, long expiry, UUID cause) {
            this.id = id;
            this.seed = id * 7919 + 4242;
            this.base = base;
            this.expiry = expiry;
            this.cause = cause;
        }
    }

    private static final Map<ServerLevel, List<Bolt>> BOLTS = new HashMap<>();
    private static int nextId = 1;

    public static List<Bolt> in(ServerLevel level) {
        List<Bolt> list = BOLTS.get(level);
        return list == null ? List.of() : list;
    }

    private static ServerPlayer holder(ServerLevel level, Vec3 pos) {
        double r = OmniConfig.BOLT_CAPTURE_RADIUS.get();
        for (ServerPlayer p : level.players()) {
            if (p.position().distanceToSqr(pos) <= r * r && BedrockPickaxeItem.holdsOmni(p)) {
                return p;
            }
        }
        return null;
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof LightningBolt bolt) || !(event.getLevel() instanceof ServerLevel level)
                || bolt.getPersistentData().getBoolean(TAG_REAL)) {
            return;
        }
        ServerPlayer owner = holder(level, bolt.position());
        if (owner == null) {
            return;
        }
        event.setCanceled(true);
        capture(level, bolt.position(), owner);
    }

    private static void capture(ServerLevel level, Vec3 pos, ServerPlayer cause) {
        int ticks = OmniConfig.BOLT_FREEZE_SECONDS.get() * 20;
        Bolt b = new Bolt(nextId++, pos, level.getGameTime() + ticks, cause.getUUID());
        BOLTS.computeIfAbsent(level, l -> new ArrayList<>()).add(b);
        VfxPacket.of(VfxType.FROZEN_BOLT, pos).duration(ticks).seed(b.seed).color(0xBFE8FF).send(level);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.LIGHTNING_BOLT_THUNDER, SoundSource.WEATHER, 8.0F, 1.1F);
        ModSounds.play(level, pos, ModSounds.THUNDER, 6.0F, 1.0F);
    }

    /** L'eclair a ete mine : il disparait. */
    public static void remove(ServerLevel level, Bolt bolt) {
        List<Bolt> list = BOLTS.get(level);
        if (list != null) {
            list.remove(bolt);
        }
        VfxPacket.cancel(VfxType.FROZEN_BOLT, bolt.seed, bolt.base).send(level);
    }

    /** Fait tomber un eclair reel a cet endroit (sans le recapturer). */
    public static void strikeReal(ServerLevel level, Vec3 pos, ServerPlayer cause) {
        LightningBolt real = EntityType.LIGHTNING_BOLT.create(level);
        if (real == null) {
            return;
        }
        real.moveTo(pos.x, pos.y, pos.z);
        real.getPersistentData().putBoolean(TAG_REAL, true);
        if (cause != null) {
            real.setCause(cause);
        }
        level.addFreshEntity(real);
    }

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.level instanceof ServerLevel level)) {
            return;
        }
        List<Bolt> list = BOLTS.get(level);
        if (list == null || list.isEmpty()) {
            return;
        }
        long now = level.getGameTime();
        Iterator<Bolt> it = list.iterator();
        while (it.hasNext()) {
            Bolt b = it.next();
            if (now >= b.expiry) {
                it.remove();
                ServerPlayer cause = null;
                for (ServerPlayer p : level.players()) {
                    if (p.getUUID().equals(b.cause)) {
                        cause = p;
                    }
                }
                strikeReal(level, b.base, cause);
            } else if (now % 14L == b.id % 14L) {
                ModSounds.play(level, b.base, ModSounds.ZAP, 2.5F, 0.7F + level.random.nextFloat() * 0.6F);
            }
        }
    }

    @SubscribeEvent
    public static void onServerStopped(ServerStoppedEvent event) {
        BOLTS.clear();
    }

    private FrozenBolts() {}
}
