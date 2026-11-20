package com.omnimine.server;

import com.omnimine.OmniConfig;
import com.omnimine.net.VfxPacket;
import com.omnimine.net.VfxType;
import com.omnimine.registry.ModSounds;
import com.omnimine.util.TargetUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/** Les deux pouvoirs de l'epee du soleil : eruption a chaque coup, rayon solaire orbital au clic droit. */
public final class SunPowers {
    public static final int COLOR = 0xFFA82E;
    private static final int CHARGE = 30;
    private static final int TOTAL = 66;

    /** Pouvoir 1 : chaque coup embrase la cible et declenche une eruption solaire autour d'elle. */
    public static void hit(ServerLevel level, LivingEntity attacker, LivingEntity target) {
        Vec3 t = target.position().add(0, target.getBbHeight() * 0.5D, 0);
        target.setSecondsOnFire(10);
        VfxPacket.of(VfxType.SUN_FLARE, t).color(COLOR).duration(24).send(level);
        ModSounds.play(level, t, ModSounds.FLAME, 1.3F, 0.9F + level.random.nextFloat() * 0.3F);
        for (LivingEntity e : Abilities.around(level, attacker, t, 3.6D)) {
            if (e == target) {
                continue;
            }
            e.setSecondsOnFire(6);
            Abilities.hurt(level, e, attacker, 6.0F);
            Abilities.knock(e, t, 0.6D, 0.3D);
        }
    }

    /** Pouvoir 2 : un rayon du soleil tombe sur la cible, puis un cratere en fusion reste en feu. */
    public static boolean beam(ServerLevel level, ServerPlayer p, ItemStack stack, InteractionHand hand) {
        Vec3 target = TargetUtil.groundTarget(p, 64.0D);
        VfxPacket.of(VfxType.SUN_BEAM, target).duration(TOTAL).color(COLOR).send(level);
        ModSounds.play(level, p.position(), ModSounds.FORGE_CHARGE, 3.0F, 1.5F);
        ModSounds.play(level, target, ModSounds.SOLAR, 8.0F, 1.0F);
        ServerEffects.add(level, new Beam(p, target));
        return true;
    }

    private static final class Beam implements ServerEffects.Timed {
        private final ServerPlayer owner;
        private final Vec3 c;
        private int t;

        Beam(ServerPlayer owner, Vec3 c) {
            this.owner = owner;
            this.c = c;
        }

        @Override
        public boolean tick(ServerLevel level) {
            t++;
            if (t == CHARGE) {
                impact(level);
            } else if (t > CHARGE && t % 4 == 0) {
                for (LivingEntity e : Abilities.around(level, owner, c, 9.0D)) {
                    e.setSecondsOnFire(6);
                    Abilities.hurt(level, e, owner, 5.0F);
                }
            }
            return t >= TOTAL;
        }

        private void impact(ServerLevel level) {
            for (LivingEntity e : Abilities.around(level, owner, c, 11.0D)) {
                e.setSecondsOnFire(15);
                Abilities.hurt(level, e, owner, 45.0F);
                Abilities.knock(e, c, 2.0D, 1.1D);
            }
            ModSounds.play(level, c, ModSounds.BOOM, 8.0F, 0.7F);
            level.playSound(null, c.x, c.y, c.z, SoundEvents.GENERIC_EXPLODE, SoundSource.PLAYERS, 6.0F, 0.6F);
            if (OmniConfig.TERRAIN_DESTRUCTION.get()) {
                crater(level, c);
            }
        }
    }

    /** Cratere : le coeur est vaporise, le pourtour devient magma et braises. */
    private static void crater(ServerLevel level, Vec3 c) {
        BlockPos center = BlockPos.containing(c);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        int r = 9;
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    double d2 = dx * dx + dy * dy * 1.4D + dz * dz;
                    if (d2 > r * r) {
                        continue;
                    }
                    m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState s = level.getBlockState(m);
                    if (s.isAir() || !Abilities.erasable(level, m, s)) {
                        continue;
                    }
                    if (d2 <= 38.0D) {
                        Abilities.erase(level, m);
                    } else if (dy <= 0 && level.random.nextFloat() < 0.35F) {
                        level.setBlock(m, Blocks.MAGMA_BLOCK.defaultBlockState(), 2 | 16);
                    }
                }
            }
        }
        // braises sur le pourtour
        for (int i = 0; i < 70; i++) {
            double ang = level.random.nextDouble() * Math.PI * 2.0D;
            double rad = 3.0D + level.random.nextDouble() * 6.5D;
            BlockPos top = BlockPos.containing(c.x + Math.cos(ang) * rad, c.y + 3.0D, c.z + Math.sin(ang) * rad);
            for (int k = 0; k < 8; k++) {
                BlockPos p = top.below(k);
                if (!level.getBlockState(p).isAir()) {
                    BlockPos above = p.above();
                    if (level.getBlockState(above).isAir()) {
                        BlockState fire = BaseFireBlock.getState(level, above);
                        if (!fire.isAir()) {
                            level.setBlock(above, fire, 3);
                        }
                    }
                    break;
                }
            }
        }
    }

    private SunPowers() {}
}
