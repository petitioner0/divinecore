package com.petitioner0.divinecore.edicts.thirteenth_edicts;


import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import net.minecraft.util.RandomSource;

import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.damage_type.ModDamageSources;
import com.petitioner0.divinecore.damage_type.ModDamageTypes;

@EventBusSubscriber(modid = DivineCore.MODID)
public final class CropPunisher {

    public static final Map<ServerLevel, Map<BlockPos, Integer>> GROWN_TRACKER = new ConcurrentHashMap<>();

    /** One-time re-check scheduling: do a 33% check again at pos (can continue to schedule the next one) */
    private static final Map<ServerLevel, Queue<ScheduledRecheck>> RECHECK_QUEUE = new ConcurrentHashMap<>();

    /** Tracker cleanup timer: each dimension has its own timer */
    private static final Map<ServerLevel, Integer> CLEANUP_TIMERS = new ConcurrentHashMap<>();

    private static final int RADIUS_XZ = 8;
    private static final float SUCCESS_PROB = 0.33f;
    private static final int RECHECK_WINDOW_TICKS = 5;
    private static final float DAMAGE_AMOUNT = 1.0F;
    private static final int CLEANUP_INTERVAL_TICKS = 100; 

    private CropPunisher() {}

    @SubscribeEvent
    public static void onLivingDamagedPost(LivingDamageEvent.Post event) {
        LivingEntity target = event.getEntity();
        if (!(target instanceof Player player)) return;
        Level level = player.level();
        if (!(level instanceof ServerLevel serverLevel)) return;

        final BlockPos base = player.blockPosition();
        final int minY = Mth.clamp(base.getY() - 1, level.getMinBuildHeight(), level.getMaxBuildHeight());
        final int maxY = Mth.clamp(base.getY() + 2, level.getMinBuildHeight(), level.getMaxBuildHeight());
        RandomSource rand = serverLevel.getRandom();

        // Only check "non-full-grown" crops with 33% probability
        for (int dx = -RADIUS_XZ; dx <= RADIUS_XZ; dx++) {
            for (int dz = -RADIUS_XZ; dz <= RADIUS_XZ; dz++) {
                for (int y = minY; y <= maxY; y++) {
                    BlockPos pos = new BlockPos(base.getX() + dx, y, base.getZ() + dz);
                    BlockState state = serverLevel.getBlockState(pos);
                    Block block = state.getBlock();
                    if (!(block instanceof CropBlock crop)) continue;

                    if (crop.isMaxAge(state)) continue; // Skip if full-grown

                    // 33% check
                    if (rand.nextFloat() <= SUCCESS_PROB) {
                        player.hurt(ModDamageSources.of(serverLevel, ModDamageTypes.CROP_PUNISHMENT), DAMAGE_AMOUNT);

                        tryGrowOnceAndTrack(serverLevel, pos, crop, state);

                        BlockState after = serverLevel.getBlockState(pos);
                        if (after.getBlock() instanceof CropBlock c2) {
                            if (!c2.isMaxAge(after)) {
                                scheduleRecheck(serverLevel, pos.immutable(), 1 + rand.nextInt(RECHECK_WINDOW_TICKS));
                            }
                        }
                    }
                }
            }
        }
    }

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        Level level = event.getLevel();
        if (!(level instanceof ServerLevel serverLevel)) return;

        // Execute re-check
        Queue<ScheduledRecheck> queue = RECHECK_QUEUE.get(serverLevel);
        if (queue != null && !queue.isEmpty()) {
            List<BlockPos> tasksToExecute = new ArrayList<>();
            int size = queue.size();
            for (int i = 0; i < size; i++) {
                ScheduledRecheck job = queue.poll();
                if (job == null) break;
                if (--job.ticksRemaining <= 0) {
                    tasksToExecute.add(job.pos);
                } else {
                    queue.offer(job); // If not expired, put it back at the end of the queue
                }
            }
            for (BlockPos pos : tasksToExecute) {
                doRecheckChainable(serverLevel, pos);
            }
        }

        // Schedule cleanup of tracker
        int timer = CLEANUP_TIMERS.getOrDefault(serverLevel, 0);
        if (++timer >= CLEANUP_INTERVAL_TICKS) {
            CLEANUP_TIMERS.put(serverLevel, 0);
            cleanupGrownTracker(serverLevel);
        } else {
            CLEANUP_TIMERS.put(serverLevel, timer);
        }
    }

    /** Re-check: if 33% success, damage the nearest player and grow 1 level; if still not full-grown, continue to schedule the next re-check (chainable) */
    private static void doRecheckChainable(ServerLevel level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof CropBlock crop)) return;

        if (crop.isMaxAge(state)) return;

        RandomSource rand = level.getRandom();
        if (rand.nextFloat() > SUCCESS_PROB) return;

        Player nearest = level.getNearestPlayer(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, RADIUS_XZ + 2, false);
        if (nearest != null) {
            nearest.hurt(ModDamageSources.of(level, ModDamageTypes.CROP_PUNISHMENT), DAMAGE_AMOUNT);
        }

        tryGrowOnceAndTrack(level, pos, crop, state);

        BlockState after = level.getBlockState(pos);
        if (after.getBlock() instanceof CropBlock c2) {
            if (!c2.isMaxAge(after)) {
                scheduleRecheck(level, pos.immutable(), 1 + rand.nextInt(RECHECK_WINDOW_TICKS));
            }
        }
    }

    /** Grow the crop by 1 level and add 1 to the tracker */
    private static void tryGrowOnceAndTrack(ServerLevel level, BlockPos pos, CropBlock crop, BlockState state) {
        int current = crop.getAge(state);
        if (current >= crop.getMaxAge()) return;

        BlockState newState = crop.getStateForAge(current + 1);
        level.setBlock(pos, newState, 2 | 16);

        GROWN_TRACKER
            .computeIfAbsent(level, k -> new ConcurrentHashMap<>())
            .merge(pos.immutable(), 1, Integer::sum);
    }

    private static void scheduleRecheck(ServerLevel level, BlockPos pos, int delayTicks) {
        RECHECK_QUEUE.computeIfAbsent(level, k -> new ConcurrentLinkedQueue<>())
             .add(new ScheduledRecheck(pos, delayTicks));
    }

    private static void cleanupGrownTracker(ServerLevel level) {
        Map<BlockPos, Integer> map = GROWN_TRACKER.get(level);
        if (map != null && !map.isEmpty()) {
            Iterator<Map.Entry<BlockPos, Integer>> it = map.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<BlockPos, Integer> e = it.next();
                BlockState state = level.getBlockState(e.getKey());
                if (!(state.getBlock() instanceof CropBlock)) {
                    it.remove();
                }
            }
        }
    }

    public static Map<BlockPos, Integer> getGrownTrackerFor(ServerLevel level) {
        return Collections.unmodifiableMap(GROWN_TRACKER.getOrDefault(level, Map.of()));
    }

    private static final class ScheduledRecheck {
        final BlockPos pos;
        int ticksRemaining;
        ScheduledRecheck(BlockPos pos, int ticks) {
            this.pos = pos;
            this.ticksRemaining = ticks;
        }
    }
}