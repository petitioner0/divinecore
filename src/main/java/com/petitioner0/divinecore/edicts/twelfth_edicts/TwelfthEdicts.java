package com.petitioner0.divinecore.edicts.twelfth_edicts;

import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.FTBHelper;
import com.petitioner0.divinecore.damage_type.ModDamageSources;
import com.petitioner0.divinecore.damage_type.ModDamageTypes;
import com.petitioner0.divinecore.effects.ModEffects;
import com.petitioner0.divinecore.items.ItemHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.EyeOfEnder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

/** Only track EyeOfEnder that will "explode"; get the nearest player when it explodes */
@EventBusSubscriber(modid = DivineCore.MODID)
public class TwelfthEdicts {

    /** Record EyeOfEnder that will explode */
    private static final Set<UUID> TO_SHATTER = ConcurrentHashMap.newKeySet();
    
    /** Track rapid teleportation sequences for players */
    private static final Map<UUID, TeleportationTask> TELEPORTATION_TASKS = new ConcurrentHashMap<>();
    
    /** Task for tracking rapid teleportation sequence */
    private static class TeleportationTask {
        final ServerPlayer player;
        final ServerLevel level;
        final Vec3 origin;
        final RandomSource random;
        int currentTeleport;
        int ticksUntilNextTeleport;
        final int radius = 40;
        final int totalTeleports = 5;
        final int delayTicks = 5; // 5 ticks = 0.25 seconds between teleports
        
        TeleportationTask(ServerPlayer player, ServerLevel level, Vec3 origin) {
            this.player = player;
            this.level = level;
            this.origin = origin;
            this.random = player.getRandom();
            this.currentTeleport = 0;
            this.ticksUntilNextTeleport = 0; // First teleport happens immediately
        }
    }

    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        Entity ent = e.getEntity();
        if (!(ent instanceof EyeOfEnder eye)) return;

        if (!eye.surviveAfterDeath) {
            TO_SHATTER.add(eye.getUUID());
        }
    }

    @SubscribeEvent
    public static void onLeave(EntityLeaveLevelEvent e) {
        if (e.getLevel().isClientSide()) return;
        Entity ent = e.getEntity();
        if (!(ent instanceof EyeOfEnder eye)) return;

        if (!TO_SHATTER.remove(eye.getUUID())) return;

        Level level = e.getLevel();

        Player nearest = null;
        double bestDistSq = Double.MAX_VALUE;
        for (Player p : level.players()) {
            double d = p.distanceToSqr(eye);
            if (d < bestDistSq) {
                bestDistSq = d;
                nearest = p;
            }
        }

        if (nearest != null) {
            nearest.addEffect(new MobEffectInstance(ModEffects.Spacilabile, 800, 0, false, true, true));
            FTBHelper.completeTask((ServerPlayer) nearest, "155ABFC61F39FB6D");
            // Cause maximum health 50% damage (round down)
            float maxHealth = nearest.getMaxHealth();
            float damage = (float) Math.floor(maxHealth * 0.5f);
            nearest.hurt(ModDamageSources.of(nearest.level(), ModDamageTypes.ENDER_EYE_SHATTER), damage);
            
            // Teleport to a random safe block within 40 blocks
            if (nearest instanceof ServerPlayer serverPlayer) {
                teleportPlayerRandomly(serverPlayer, eye.blockPosition(), 40);
            }
            CheckAndGiveReward(nearest);
        }
    }
    
    /**
     * Teleport the player to a random safe block within 40 blocks
     */
    private static void teleportPlayerRandomly(ServerPlayer player, BlockPos center, int radius) {
        ServerLevel level = player.serverLevel();
        RandomSource random = player.getRandom();

        for (int attempts = 0; attempts < 64; attempts++) {
            double angle = random.nextDouble() * Math.PI * 2.0;
            double dist  = Math.sqrt(random.nextDouble()) * radius;

            int x = center.getX() + Mth.floor(dist * Math.cos(angle));
            int z = center.getZ() + Mth.floor(dist * Math.sin(angle));

            BlockPos check2D = new BlockPos(x, center.getY(), z);

            // World border and chunk loading check
            if (!level.getWorldBorder().isWithinBounds(check2D)) continue;
            int chunkX = SectionPos.blockToSectionCoord(x);
            int chunkZ = SectionPos.blockToSectionCoord(z);
            if (!level.hasChunk(chunkX, chunkZ)) continue;
            // Get the standing surface height of the (x,z) directly through Heightmap
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            if (y <= level.getMinBuildHeight()) continue; // Extreme terrain/dimension defense

            BlockPos ground = new BlockPos(x, y - 1, z);
            BlockPos feet   = new BlockPos(x, y, z);
            BlockPos head   = new BlockPos(x, y + 1, z);

            BlockState groundState = level.getBlockState(ground);

            // The ground must have a collision body and itself is not a fluid
            if (groundState.getFluidState() != Fluids.EMPTY.defaultFluidState()) continue;
            if (groundState.getCollisionShape(level, ground).isEmpty()) continue;


            // The feet and head two blocks need to be "empty or a fluid other than lava"
            if (!isSafeBlock(level, feet) || !isSafeBlock(level, head)) continue;

            // Use the AABB of the player's standing pose for the final non-collision check
            Vec3 target = new Vec3(x + 0.5, y, z + 0.5);
            AABB box = player.getDimensions(Pose.STANDING).makeBoundingBox(target);
            if (!level.noCollision(player, box)) continue;

            // Find a safe position, randomly a direction more natural
            float yaw = random.nextFloat() * 360f;
            player.teleportTo(level, target.x, target.y, target.z, yaw, player.getXRot());
            return;
        }

        int cx = center.getX();
        int cz = center.getZ();
        int cy = level.getHeight(Heightmap.Types.MOTION_BLOCKING, cx, cz);
        if (cy <= level.getMinBuildHeight()) {
            cy = center.getY() + 1;
        }
        player.teleportTo(level, cx + 0.5, cy, cz + 0.5, player.getYRot(), player.getXRot());
    }


    private static boolean isSafeBlock(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        // Empty blocks are safe
        if (level.isEmptyBlock(pos)) return true;
        
        // Check the fluid state, fluids other than lava are safe
        var fluidState = state.getFluidState();
        if (!fluidState.isEmpty()) {
            return true;
        }
        
        return false;
    }

    public static void CheckAndGiveReward(Player player) {
        if (player == null) return;

        // Check if the player has all three effects
        if (player.hasEffect(ModEffects.Spatiolysis)    
            && player.hasEffect(ModEffects.Oscillaspace)  
            && player.hasEffect(ModEffects.Spacilabile)) { 
            
            // Give the player the item through detection
            if (player instanceof ServerPlayer serverPlayer) {
                // Start rapid teleportation sequence before giving reward
                startRapidTeleportationSequence(serverPlayer);
            }
        }
    }
    
    /**
     * Rapidly teleport the player 5 times around their current position, then return to origin and give reward
     */
    private static void startRapidTeleportationSequence(ServerPlayer player) {
        if (player == null || !(player.level() instanceof ServerLevel level)) return;
        
        // Record original position
        Vec3 origin = player.position();
        
        // Create and register teleportation task
        TeleportationTask task = new TeleportationTask(player, level, origin);
        TELEPORTATION_TASKS.put(player.getUUID(), task);
    }
    
    /**
     * Handle rapid teleportation sequences on server tick
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        if (TELEPORTATION_TASKS.isEmpty()) return;
        
        TELEPORTATION_TASKS.entrySet().removeIf(entry -> {
            TeleportationTask task = entry.getValue();
            
            // Check if player is still valid
            if (!task.player.isAlive() || task.player.level() != task.level) {
                return true; // Remove invalid task
            }
            
            // Decrement timer
            if (task.ticksUntilNextTeleport > 0) {
                task.ticksUntilNextTeleport--;
            }
            
            // If it's time for next teleport (first teleport happens immediately)
            if (task.ticksUntilNextTeleport <= 0) {
                if (task.currentTeleport < task.totalTeleports) {
                    // Perform teleport
                    double angle = task.random.nextDouble() * Math.PI * 2.0;
                    double dist = Math.sqrt(task.random.nextDouble()) * task.radius;
                    
                    double x = task.origin.x + dist * Math.cos(angle);
                    double z = task.origin.z + dist * Math.sin(angle);
                    double y = task.origin.y;
                    
                    // Teleport without safety checks
                    task.player.teleportTo(task.level, x, y, z, task.random.nextFloat() * 360f, task.player.getXRot());
                    
                    task.currentTeleport++;
                    // Set delay for next teleport (if not the last one)
                    if (task.currentTeleport < task.totalTeleports) {
                        task.ticksUntilNextTeleport = task.delayTicks;
                    }
                } else {
                    // All teleports done, return to origin and give reward
                    task.player.teleportTo(task.level, task.origin.x, task.origin.y, task.origin.z, 
                                          task.player.getYRot(), task.player.getXRot());
                    
                    // Give reward
                    ItemHelper.giveItemToPlayer(task.player, "shattered_riftstone", 1);
                    
                    // Clear all three status effects
                    task.player.removeEffect(ModEffects.Spatiolysis);
                    task.player.removeEffect(ModEffects.Oscillaspace);
                    task.player.removeEffect(ModEffects.Spacilabile);
                    
                    return true; // Remove completed task
                }
            }
            
            return false; // Keep task
        });
    }
}