package com.petitioner0.divinecore.edicts.fifth_edicts;

import com.petitioner0.divinecore.items.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.item.ItemStack;

import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import com.petitioner0.divinecore.DivineCore;

import java.util.List;
import java.util.ArrayList;

public final class FifthEdicts {

    // The brewing stand position -> tracking entry (each brewing stand only belongs to the latest player to open it, a player can track multiple brewing stands)
    private static final Map<BrewingStandKey, TrackedBrew> TRACKING = new ConcurrentHashMap<>();

    // The delayed task list: used to generate items after 1 tick
    private static final List<DelayedItemSpawn> DELAYED_SPAWNS = new ArrayList<>();

    /** 1) Listen to the player right-clicking the block: if it is a brewing stand, record the position and state */
    @SubscribeEvent
    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock e) {
        if (!(e.getEntity() instanceof ServerPlayer sp))
            return;
        Level level = e.getLevel();
        BlockPos pos = e.getPos();
        BlockState state = level.getBlockState(pos);

        if (!(state.getBlock() instanceof BrewingStandBlock))
            return;

        // Here we don't rely on the menu event, just register, and the blocks will be automatically cleaned up later if there is no block entity
        BrewingStandKey key = new BrewingStandKey(level.dimension(), pos);
        TRACKING.put(key, new TrackedBrew(sp, level.dimension(), pos));
    }

    /** 2) Clean up when the player logs out */
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent e) {
        if (e.getEntity() instanceof ServerPlayer sp) {
            // Clean up all the brewing stands tracked by this player
            TRACKING.entrySet().removeIf(entry -> entry.getValue().player.equals(sp));
        }
    }

    /** 3) Server Tick: poll the brewTime status of all tracked brewing stands */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post e) {

        if (TRACKING.isEmpty())
            return;

        MinecraftServer server = Objects.requireNonNull(e.getServer());
        // Use a snapshot to avoid concurrent modification during iteration
        Set<Map.Entry<BrewingStandKey, TrackedBrew>> entries = Set.copyOf(TRACKING.entrySet());

        for (Map.Entry<BrewingStandKey, TrackedBrew> entry : entries) {
            BrewingStandKey key = entry.getKey();
            TrackedBrew tracked = entry.getValue();

            // The player is not in the same server world or is invalid: clean up
            ServerLevel level = server.getLevel(tracked.dimension);
            if (level == null || tracked.player.isRemoved() || !level.isLoaded(tracked.pos)) {
                TRACKING.remove(key);
                continue;
            }

            BlockEntity be = level.getBlockEntity(tracked.pos);
            if (!(be instanceof BrewingStandBlockEntity brewBE)) {
                // The block is destroyed / unloaded
                TRACKING.remove(key);
                continue;
            }

            // This field has been set to public by accessTransformer, so the error can be ignored
            int brewTime = brewBE.brewTime;

            // Check if brewing has started: 0 -> >0
            if (!tracked.brewingStarted && tracked.lastBrewTime == 0 && brewTime > 0) {
                tracked.brewingStarted = true;
            }

            // Check if brewing is complete: marked as started && brewTime just fell to 0
            if (tracked.brewingStarted && brewTime == 0 && tracked.lastBrewTime > 0) {
                // More strict brewing detection: if it was not 1 before, it directly becomes 0,认为这不是一次有效酿造
                if (tracked.lastBrewTime != 1) {
                    // Reset the state, continue to listen
                    tracked.brewingStarted = false;
                    tracked.lastBrewTime = brewTime;
                    continue;
                }

                // Read the potions/bottles in slots 0~2
                Container inv = brewBE;
                // Count the number of potions (non-empty and of potion type)
                int potionCount = 0;
                if (inv.getItem(0).get(net.minecraft.core.component.DataComponents.POTION_CONTENTS) != null)
                    potionCount++;
                if (inv.getItem(1).get(net.minecraft.core.component.DataComponents.POTION_CONTENTS) != null)
                    potionCount++;
                if (inv.getItem(2).get(net.minecraft.core.component.DataComponents.POTION_CONTENTS) != null)
                    potionCount++;
                // Check if it is in the nether dimension
                boolean isInNether = level.dimension().location().toString().equals("minecraft:the_nether");

                // For each potion, determine the 10% probability, if in the nether, it will trigger explosion
                Random random = new Random();
                boolean shouldExplode = false;

                if (isInNether) {
                    // The nether dimension必定触发爆炸
                    shouldExplode = true;
                } else {
                    // Other dimensions according to the original logic: 10% probability for each potion
                    for (int i = 0; i < potionCount; i++) {
                        if (random.nextDouble() < 0.1) { // 10% 概率
                            shouldExplode = true;
                            break; // Any successful trigger will trigger
                        }
                    }
                }

                if (shouldExplode) {
                    // Check if the destroyed potion contains the chosen potion
                    boolean containsChosenPotion = false;
                    for (int i = 0; i < 3; i++) { // Only check the potion slots 0-2
                        ItemStack potionStack = inv.getItem(i);
                        // Directly convert ItemStack -> Potion, then call your own method
                        var contents = potionStack.get(net.minecraft.core.component.DataComponents.POTION_CONTENTS);
                        if (contents != null && contents.potion().isPresent()) {
                            net.minecraft.world.item.alchemy.Potion potion = contents.potion().get().value();
                            if (DivineWorldPotion.isChosenPotion(level, potion)) {
                                containsChosenPotion = true;
                                break;
                            }
                        }
                    }

                    // Clear the five slots of the brewing stand (0-4)
                    for (int i = 0; i < 5; i++) {
                        inv.setItem(i, ItemStack.EMPTY);
                    }

                    // Play the sound of glass breaking
                    level.playSound(null, tracked.pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0f, 1.0f);

                    // Destroy the brewing stand itself
                    level.setBlock(tracked.pos, Blocks.AIR.defaultBlockState(), 3);

                    // An explosion occurs at the coordinates with a strength of 1f
                    level.explode(null, tracked.pos.getX() + 0.5, tracked.pos.getY() + 0.5, tracked.pos.getZ() + 0.5,
                            1.0f, Level.ExplosionInteraction.BLOCK);

                    // If the chosen potion is contained, generate the module item after 1 tick
                    if (containsChosenPotion) {
                        DivineCore.LOGGER.info("[DivineCore] 检测到被选中的药水被摧毁，将生成物品: aetherial_reverie");
                        DELAYED_SPAWNS.add(new DelayedItemSpawn(level, tracked.pos, "aetherial_reverie", 1));
                    } else {
                        DivineCore.LOGGER.info("[DivineCore] 药水被摧毁，但不包含被选中的药水");
                    }
                }

                // After this completion, clean up the tracking (if you need to continue listening to the same brewing stand, you can not clean up or delay the cleanup)
                TRACKING.remove(key);
                continue;
            }

            tracked.lastBrewTime = brewTime;
        }

        // Handle the delayed generated items
        DELAYED_SPAWNS.removeIf(delayedSpawn -> {
            if (delayedSpawn.ticksRemaining <= 0) {
                delayedSpawn.spawnItem();
                return true; // Remove from the list
            } else {
                delayedSpawn.ticksRemaining--;
                return false; // Continue to wait
            }
        });
    }

    /** The task of delaying the generation of items */
    private static class DelayedItemSpawn {
        private final ServerLevel level;
        private final BlockPos pos;
        private final String itemName;
        private int ticksRemaining;

        public DelayedItemSpawn(ServerLevel level, BlockPos pos, String itemName, int ticks) {
            this.level = level;
            this.pos = pos;
            this.itemName = itemName;
            this.ticksRemaining = ticks;
        }

        public void spawnItem() {
            try {
                // Use ItemHelper to generate the item
                boolean success = ItemHelper.dropItemAt(level, 
                    new net.minecraft.world.phys.Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5), 
                    itemName, 1);
                
                if (success) {
                    DivineCore.LOGGER.info("[DivineCore] 成功生成物品: " + itemName + " 在位置: " + pos);
                } else {
                    DivineCore.LOGGER.error("[DivineCore] 错误：找不到物品: " + itemName);
                }
            } catch (Exception e) {
                DivineCore.LOGGER.error("[DivineCore] 生成物品时出错: " + e.getMessage(), e);
            }
        }
    }

}