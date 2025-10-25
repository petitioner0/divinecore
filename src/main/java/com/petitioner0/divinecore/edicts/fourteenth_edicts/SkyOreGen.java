package com.petitioner0.divinecore.edicts.fourteenth_edicts;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.ChunkDataEvent;
import net.neoforged.neoforge.event.level.ChunkEvent;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = "divinecore")
public class SkyOreGen {

    private static final int MIN_Y = 260;
    private static final int MAX_Y = 319;                 
    private static final int TRY_DENOM = 56;               
    private static final int MAX_COUNT_PER_CHUNK = 1;     

    private static final String TAG_KEY = "divinecore_sky_ore_done";
    private static final Set<ChunkPos> DONE = ConcurrentHashMap.newKeySet();

    // Read chunk additional data: if already processed, add to cache
    @SubscribeEvent
    public static void onChunkDataLoad(ChunkDataEvent.Load e) {
        if (!(e.getChunk() instanceof LevelChunk lc)) return;
        if (e.getData().getBoolean(TAG_KEY)) {
            DONE.add(lc.getPos());
        }
    }

    // Write back: if the chunk is marked as already attempted, persist the boolean value
    @SubscribeEvent
    public static void onChunkDataSave(ChunkDataEvent.Save e) {
        if (!(e.getChunk() instanceof LevelChunk lc)) return;
        if (DONE.contains(lc.getPos())) {
            e.getData().putBoolean(TAG_KEY, true);
        }
    }

    @SubscribeEvent
    public static void onChunkLoad(ChunkEvent.Load e) {
        if (!(e.getLevel() instanceof ServerLevel level)) return; 
        if (!(e.getChunk() instanceof LevelChunk lc)) return;

        ChunkPos cpos = lc.getPos();
        if (DONE.contains(cpos)) return;

        if (level.dimension() != Level.OVERWORLD) {
            DONE.add(cpos);
            return;
        }

        long seed = level.getSeed();
        long mix = mix64(seed ^ cpos.toLong());

        boolean shouldTry = ((int) mix & (TRY_DENOM - 1)) == 0;
        if (shouldTry) {
            int count = (int) ((mix >>> 8) % (MAX_COUNT_PER_CHUNK + 1));
            if (count < 0) count += (MAX_COUNT_PER_CHUNK + 1);

            RandomSource rand = RandomSource.create(seed ^ (cpos.toLong() * 0x9E3779B97F4A7C15L));
            for (int i = 0; i < count; i++) {
                int x = cpos.getMinBlockX() + rand.nextInt(16);
                int z = cpos.getMinBlockZ() + rand.nextInt(16);
                int y = MIN_Y + rand.nextInt(MAX_Y - MIN_Y + 1);
                SkyOreScheduler.enqueue(level, cpos, new BlockPos(x, y, z), false);
            }
        }

        DONE.add(cpos);
    }

    private static long mix64(long z) {
        z = (z ^ (z >>> 33)) * 0xff51afd7ed558ccdL;
        z = (z ^ (z >>> 33)) * 0xc4ceb9fe1a85ec53L;
        return z ^ (z >>> 33);
    }
}