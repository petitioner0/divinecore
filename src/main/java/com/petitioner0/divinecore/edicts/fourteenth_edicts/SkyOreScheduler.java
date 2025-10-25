package com.petitioner0.divinecore.edicts.fourteenth_edicts;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

@EventBusSubscriber(modid = "divinecore")
public class SkyOreScheduler {

    // Queue element
    private record Task(ChunkPos cpos, BlockPos pos, boolean shouldNotify) {}

    // Overworld sky ore generation queue
    private static final Queue<Task> QUEUE = new ConcurrentLinkedQueue<>();

    static void enqueue(ServerLevel level, ChunkPos cpos, BlockPos pos, boolean shouldNotify) {
        if (level.dimension() == net.minecraft.world.level.Level.OVERWORLD) {
            QUEUE.add(new Task(cpos, pos, shouldNotify));
        }
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {
        ServerLevel overworld = e.getServer().overworld();
        if (overworld == null || QUEUE.isEmpty()) return;

        int budget = 64;

        for (int i = 0; i < budget; i++) {
            Task t = QUEUE.poll();
            if (t == null) break;

            // Check if position is loaded
            if (!overworld.isLoaded(t.pos)) {
                QUEUE.add(t); 
                continue;
            }

            var state = com.petitioner0.divinecore.blocks.ModBlocks.AURUM_OF_THE_FIRMAMENT_ORE.get().defaultBlockState();
            overworld.setBlock(t.pos, state, Block.UPDATE_CLIENTS);
        }
    }
}