package com.omnimine.server;

import com.omnimine.OmniConfig;
import com.omnimine.data.MobKind;
import com.omnimine.net.VfxPacket;
import com.omnimine.net.VfxType;
import com.omnimine.registry.ModSounds;
import com.omnimine.util.TargetUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.Tags;

/** Le petit pouvoir de chacun des huit mobs mines (clic droit). */
public final class MobPowers {

    public static boolean use(MobKind kind, ServerLevel level, ServerPlayer p) {
        return switch (kind) {
            case SKELETON -> boneStorm(level, p);
            case CREEPER -> blast(level, p);
            case ZOMBIE -> rot(level, p);
            case SPIDER -> web(level, p);
            case GUARDIAN -> laser(level, p);
            case VILLAGER -> sonic(level, p);
            case IRON_GOLEM -> quake(level, p);
            case ENDERMAN -> warp(level, p);
        };
    }

    private static boolean destructive() {
        return OmniConfig.TERRAIN_DESTRUCTION.get();
    }

    // ------------------------------------------------------------- squelette

    /** Tempete d'os : une pluie d'esquilles d'os s'abat pendant trois secondes sur une zone. */
    private static boolean boneStorm(ServerLevel level, ServerPlayer p) {
        Vec3 c = TargetUtil.groundTarget(p, 40.0D);
        VfxPacket.of(VfxType.BONE_RAIN, c).param(7.0F).color(MobKind.SKELETON.color).duration(76).send(level);
        ModSounds.play(level, c, SoundEvents.SKELETON_SHOOT, 3.0F, 0.6F);
        ModSounds.play(level, c, ModSounds.WHIRL, 2.0F, 1.4F);
        ServerEffects.repeat(level, 4, 16, i -> {
            for (LivingEntity e : Abilities.around(level, p, c, 7.0D)) {
                Abilities.hurt(level, e, p, 5.0F);
                e.setDeltaMovement(e.getDeltaMovement().multiply(0.5D, 1.0D, 0.5D));
            }
            for (int k = 0; k < 3; k++) {
                double ang = level.random.nextDouble() * Math.PI * 2.0D;
                double rad = Math.sqrt(level.random.nextDouble()) * 7.0D;
                ModSounds.play(level, c.add(Math.cos(ang) * rad, 0, Math.sin(ang) * rad),
                        SoundEvents.BONE_BLOCK_BREAK, 1.4F, 0.8F + level.random.nextFloat() * 0.6F);
            }
        });
        return true;
    }

    // --------------------------------------------------------------- creeper

    /** Explosion verte : la zone se charge puis explose. */
    private static boolean blast(ServerLevel level, ServerPlayer p) {
        Vec3 c = TargetUtil.groundTarget(p, 28.0D);
        VfxPacket.of(VfxType.BLAST, c).color(MobKind.CREEPER.color).duration(72).send(level);
        level.playSound(null, c.x, c.y, c.z, SoundEvents.CREEPER_PRIMED, SoundSource.PLAYERS, 3.0F, 0.7F);
        ServerEffects.delay(level, 26, () -> p.addEffect(
                new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 40, 4, true, false)));
        ServerEffects.delay(level, 28, () -> {
            Level.ExplosionInteraction mode = destructive() ? Level.ExplosionInteraction.TNT
                    : Level.ExplosionInteraction.NONE;
            level.explode(p, c.x, c.y + 0.5D, c.z, 7.0F, mode);
            ModSounds.play(level, c, ModSounds.BOOM, 8.0F, 0.8F);
            for (LivingEntity e : Abilities.around(level, p, c, 9.0D)) {
                Abilities.knock(e, c, 1.6D, 0.8D);
            }
        });
        return true;
    }

    // ---------------------------------------------------------------- zombie

    /** Souffle de peste : un cone de brume putride empoisonne, affaiblit et te soigne. */
    private static boolean rot(ServerLevel level, ServerPlayer p) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        VfxPacket.of(VfxType.ROT, eye).to(eye.add(look.scale(13.0D))).color(MobKind.ZOMBIE.color).duration(64).send(level);
        ModSounds.play(level, eye, SoundEvents.ZOMBIE_AMBIENT, 3.0F, 0.5F);
        ModSounds.play(level, eye, SoundEvents.ZOMBIE_VILLAGER_CURE, 2.0F, 0.6F);
        ServerEffects.repeat(level, 4, 14, i -> {
            Vec3 o = eye;
            int infected = 0;
            for (LivingEntity e : Abilities.around(level, p, o.add(look.scale(6.5D)), 8.0D)) {
                Vec3 to = e.position().add(0, e.getBbHeight() * 0.5D, 0).subtract(o);
                double dist = to.length();
                if (dist > 13.0D || dist < 0.4D || to.normalize().dot(look) < 0.8D) {
                    continue;
                }
                Abilities.hurt(level, e, p, 3.0F);
                e.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                e.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 60, 1));
                infected++;
            }
            if (infected > 0) {
                p.heal(Math.min(4, infected));
            }
        });
        return true;
    }

    // --------------------------------------------------------------- araignee

    /** Toile geante : des fils s'abattent sur la zone, les ennemis sont attires et englues. */
    private static boolean web(ServerLevel level, ServerPlayer p) {
        Vec3 c = TargetUtil.groundTarget(p, 30.0D);
        VfxPacket.of(VfxType.WEB, c).to(p.getEyePosition().add(0, -0.3D, 0)).param(6.5F)
                .color(MobKind.SPIDER.color).duration(110).send(level);
        ModSounds.play(level, c, SoundEvents.SPIDER_AMBIENT, 3.0F, 0.6F);
        ModSounds.play(level, c, SoundEvents.WOOL_PLACE, 3.0F, 0.5F);
        List<BlockPos> placed = new ArrayList<>();
        for (int i = 0; i < 70; i++) {
            double ang = level.random.nextDouble() * Math.PI * 2.0D;
            double rad = Math.sqrt(level.random.nextDouble()) * 5.5D;
            BlockPos pos = BlockPos.containing(c.x + Math.cos(ang) * rad, c.y, c.z + Math.sin(ang) * rad)
                    .above(level.random.nextInt(3));
            if (level.getBlockState(pos).isAir()) {
                level.setBlock(pos, Blocks.COBWEB.defaultBlockState(), 3);
                placed.add(pos);
            }
        }
        ServerEffects.delay(level, 180, () -> {
            for (BlockPos pos : placed) {
                if (level.getBlockState(pos).is(Blocks.COBWEB)) {
                    level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
                }
            }
        });
        ServerEffects.repeat(level, 5, 8, i -> {
            for (LivingEntity e : Abilities.around(level, p, c, 6.5D)) {
                Abilities.pull(e, c.add(0, 1.0D, 0), 0.35D, 0.05D);
                e.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 100, 3));
                if (i == 0) {
                    Abilities.hurt(level, e, p, 6.0F);
                }
            }
        });
        return true;
    }

    // ---------------------------------------------------------------- gardien

    /** Rayon du gardien : le rayon se charge puis perce tout, ennemis et terrain, sur 38 blocs. */
    private static boolean laser(ServerLevel level, ServerPlayer p) {
        Vec3 eye = p.getEyePosition();
        Vec3 dir = p.getLookAngle().normalize();
        Vec3 end = eye.add(dir.scale(38.0D));
        VfxPacket.of(VfxType.LASER, eye).to(end).color(MobKind.GUARDIAN.color).duration(58).send(level);
        ModSounds.play(level, eye, SoundEvents.GUARDIAN_ATTACK, 3.0F, 0.6F);
        ModSounds.play(level, eye, ModSounds.LASER, 4.0F, 1.0F);
        ServerEffects.delay(level, 30, () -> ServerEffects.repeat(level, 3, 9, k -> {
            double s0 = Math.max(2.0D, k * 4.2D);
            double s1 = (k + 1) * 4.2D;
            Vec3 a = eye.add(dir.scale(s0));
            Vec3 b = eye.add(dir.scale(s1));
            AABB box = new AABB(a, b).inflate(2.0D);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, x -> Abilities.validTarget(p, x))) {
                if (e.getBoundingBox().inflate(0.9D).clip(a, b).isPresent()) {
                    Abilities.hurt(level, e, p, 20.0F);
                    e.setSecondsOnFire(4);
                }
            }
            if (destructive()) {
                for (double t = s0; t <= s1; t += 0.7D) {
                    Abilities.eraseSphere(level, eye.add(dir.scale(t)), 1.7D);
                }
            }
            ModSounds.play(level, b, SoundEvents.GUARDIAN_ATTACK, 2.0F, 1.2F);
        }));
        return true;
    }

    // -------------------------------------------------------------- villageois

    /** Cri du village : trois ondes sonores repoussent tout, brisent le verre et font cracher des emeraudes. */
    private static boolean sonic(ServerLevel level, ServerPlayer p) {
        Vec3 c = p.position().add(0, 1.0D, 0);
        VfxPacket.of(VfxType.SONIC, c).follow(p).color(MobKind.VILLAGER.color).param(11.0F).duration(54).send(level);
        ServerEffects.repeat(level, 7, 3, i -> {
            double inner = i == 0 ? 0.0D : 4.0D + (i - 1) * 3.5D;
            double outer = 4.0D + i * 3.5D;
            ModSounds.play(level, c, SoundEvents.VILLAGER_NO, 3.0F, 0.5F + i * 0.25F);
            ModSounds.play(level, c, ModSounds.PULSE, 3.0F, 0.8F + i * 0.2F);
            for (LivingEntity e : Abilities.around(level, p, c, outer)) {
                if (e.position().distanceTo(c) < inner) {
                    continue;
                }
                Abilities.hurt(level, e, p, 8.0F);
                Abilities.knock(e, c, 1.8D, 0.6D);
                e.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 100, 0));
                if (e instanceof Enemy) {
                    level.addFreshEntity(new ItemEntity(level, e.getX(), e.getY() + 0.5D, e.getZ(),
                            new ItemStack(Items.EMERALD, 1 + level.random.nextInt(2))));
                }
            }
            if (destructive()) {
                shatterGlass(level, c, outer);
            }
        });
        return true;
    }

    private static void shatterGlass(ServerLevel level, Vec3 c, double radius) {
        int r = (int) Math.ceil(radius);
        BlockPos center = BlockPos.containing(c);
        BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
        for (int dx = -r; dx <= r; dx++) {
            for (int dy = -r; dy <= r; dy++) {
                for (int dz = -r; dz <= r; dz++) {
                    if (dx * dx + dy * dy + dz * dz > radius * radius) {
                        continue;
                    }
                    m.set(center.getX() + dx, center.getY() + dy, center.getZ() + dz);
                    BlockState s = level.getBlockState(m);
                    if (s.is(Tags.Blocks.GLASS) || s.is(Tags.Blocks.GLASS_PANES)) {
                        level.destroyBlock(m.immutable(), false);
                    }
                }
            }
        }
    }

    // ------------------------------------------------------------- golem de fer

    /** Frappe du golem : le sol se fend en etoile, tout est projete en l'air. */
    private static boolean quake(ServerLevel level, ServerPlayer p) {
        Vec3 c = p.position();
        VfxPacket.of(VfxType.QUAKE, c).color(MobKind.IRON_GOLEM.color).param(11.0F).duration(64).send(level);
        ModSounds.play(level, c, SoundEvents.IRON_GOLEM_ATTACK, 3.0F, 0.5F);
        ServerEffects.delay(level, 8, () -> {
            ModSounds.play(level, c, ModSounds.QUAKE, 6.0F, 0.8F);
            ModSounds.play(level, c, ModSounds.BOOM, 4.0F, 0.7F);
            p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 60, 2, true, false));
            for (LivingEntity e : Abilities.around(level, p, c, 10.5D)) {
                Abilities.hurt(level, e, p, 22.0F);
                Abilities.knock(e, c, 0.9D, 0.0D);
                e.setDeltaMovement(e.getDeltaMovement().x, 1.5D, e.getDeltaMovement().z);
                e.hurtMarked = true;
            }
            if (destructive()) {
                for (int k = 0; k < 8; k++) {
                    double ang = k * Math.PI / 4.0D + (level.random.nextDouble() - 0.5D) * 0.3D;
                    for (double dist = 1.5D; dist <= 10.0D; dist += 1.0D) {
                        BlockPos col = BlockPos.containing(c.x + Math.cos(ang) * dist, c.y + 2.0D, c.z + Math.sin(ang) * dist);
                        for (int dy = 0; dy < 6; dy++) {
                            BlockPos g = col.below(dy);
                            BlockState s = level.getBlockState(g);
                            if (!s.isAir()) {
                                if (Abilities.erasable(level, g, s)) {
                                    Abilities.erase(level, g);
                                    if (level.random.nextFloat() < 0.5F) {
                                        BlockPos g2 = g.below();
                                        BlockState s2 = level.getBlockState(g2);
                                        if (Abilities.erasable(level, g2, s2)) {
                                            Abilities.erase(level, g2);
                                        }
                                    }
                                }
                                break;
                            }
                        }
                    }
                }
            }
        });
        return true;
    }

    // ---------------------------------------------------------------- enderman

    /** Passage de l'ombre : tu te teleportes vers ta visee en brisant tout ce qui est sur le chemin. */
    private static boolean warp(ServerLevel level, ServerPlayer p) {
        Vec3 from = p.position();
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle();
        double dist = 30.0D;
        BlockHitResult hit = level.clip(new ClipContext(eye, eye.add(look.scale(dist)),
                ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, p));
        Vec3 end = hit.getType() == HitResult.Type.MISS ? eye.add(look.scale(dist))
                : hit.getLocation().subtract(look.scale(0.9D));
        Vec3 feet = end.subtract(0, p.getEyeHeight(), 0);
        for (int i = 0; i < 8 && !level.noCollision(p, p.getBoundingBox().move(feet.subtract(from))); i++) {
            feet = feet.subtract(look.scale(0.6D)).add(0, 0.25D, 0);
        }
        if (feet.distanceTo(from) < 2.0D) {
            return false;
        }
        AABB box = new AABB(from, feet).inflate(2.4D);
        Vec3 a = from.add(0, 1.0D, 0);
        Vec3 b = feet.add(0, 1.0D, 0);
        for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, box, x -> Abilities.validTarget(p, x))) {
            if (e.getBoundingBox().inflate(1.2D).clip(a, b).isPresent()) {
                Abilities.hurt(level, e, p, 14.0F);
                e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            }
        }
        VfxPacket.of(VfxType.WARP, a).to(b).color(MobKind.ENDERMAN.color).duration(40).send(level);
        ModSounds.play(level, from, SoundEvents.ENDERMAN_TELEPORT, 2.5F, 0.8F);
        ModSounds.play(level, from, ModSounds.BLINK, 3.0F, 1.1F);
        p.teleportTo(feet.x, feet.y, feet.z);
        p.setDeltaMovement(Vec3.ZERO);
        p.fallDistance = 0.0F;
        p.hurtMarked = true;
        ModSounds.play(level, feet, SoundEvents.ENDERMAN_TELEPORT, 2.5F, 1.2F);
        return true;
    }

    private MobPowers() {}
}
