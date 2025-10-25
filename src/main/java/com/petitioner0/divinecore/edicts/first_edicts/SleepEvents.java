package com.petitioner0.divinecore.edicts.first_edicts;

import com.petitioner0.divinecore.DivineCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerSetSpawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * Sleep event handler
 * Handles teleporting players to dream dimension when sleeping
 */
public class SleepEvents {

    // Players with delayed sleep check: UUID -> remaining ticks
    private static final Map<UUID, Integer> delayedSleepCheck = new HashMap<>();
    // Players who passed sleep check for unified teleport
    private static final java.util.Set<UUID> passedSleepCheck = new java.util.HashSet<>();
    // Unified teleport timer
    private static int unifiedTeleportTimer = -1;
    // Global sleep check in progress flag
    private static boolean isGlobalSleepCheck = false;
    private static final Random random = new Random();
    // Player bed positions: UUID -> bed position
    private static final Map<UUID, Vec3> playerBedPositions = new HashMap<>();

    /**
     * Handle player set spawn event (triggered when sleeping)
     */
    @SubscribeEvent
    public static void onPlayerSetSpawn(PlayerSetSpawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            // Record bed position (player is at bed when sleeping)
            Vec3 bedPos = new Vec3(player.getX(), player.getY(), player.getZ());
            playerBedPositions.put(player.getUUID(), bedPos);
            DivineCore.LOGGER.info("记录玩家 {} 的床位置: ({}, {}, {})",
                    player.getName().getString(), bedPos.x, bedPos.y, bedPos.z);

            // Start global sleep check if not already started
            if (!isGlobalSleepCheck) {
                isGlobalSleepCheck = true;
                // Add all online players to delayed check list
                for (ServerPlayer onlinePlayer : player.getServer().getPlayerList().getPlayers()) {
                    delayedSleepCheck.put(onlinePlayer.getUUID(), 2);
                }
                DivineCore.LOGGER.info("开始全局睡眠检测，在线玩家数量: {}", delayedSleepCheck.size());
            }
        }
    }

    /**
     * Handle player wake up event
     */
    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerUUID = player.getUUID();

            // Reset all states if player is in check lists
            if (passedSleepCheck.contains(playerUUID) || delayedSleepCheck.containsKey(playerUUID)) {
                DivineCore.LOGGER.info("玩家 {} 醒来，重置全局睡眠检测", player.getName().getString());
                resetAllStates();
            }
        }
    }

    /**
     * Handle player tick, decrement timers and execute teleport
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID playerUUID = player.getUUID();

            // Handle delayed sleep check
            if (delayedSleepCheck.containsKey(playerUUID)) {
                int remainingDelay = delayedSleepCheck.get(playerUUID);
                if (remainingDelay <= 0) {
                    // Check player sleep status after delay
                    if (player.isSleeping()) {
                        // Add player to passed check list
                        passedSleepCheck.add(playerUUID);
                        DivineCore.LOGGER.info("玩家 {} 通过睡眠判定", player.getName().getString());
                    } else {
                        // Player not sleeping, reset all states
                        DivineCore.LOGGER.info("玩家 {} 未通过睡眠判定，重置全局检测", player.getName().getString());
                        resetAllStates();
                        return;
                    }

                    // Remove from delayed check list
                    delayedSleepCheck.remove(playerUUID);

                    // Check if all players passed
                    if (delayedSleepCheck.isEmpty()) {
                        // All players passed, roll for teleport chance
                        if (random.nextFloat() < 0.25f) {
                            unifiedTeleportTimer = 97;
                            DivineCore.LOGGER.info("所有玩家都通过睡眠判定，25%概率触发，开始统一梦境传送计时，玩家数量: {}", passedSleepCheck.size());
                        } else {
                            DivineCore.LOGGER.info("所有玩家都通过睡眠判定，但未触发传送，重置状态");
                            resetAllStates();
                        }
                    }
                } else {
                    // Decrement remaining delay ticks
                    delayedSleepCheck.put(playerUUID, remainingDelay - 1);
                }
            }

            // Check unified teleport timer status (countdown handled in ServerTickEvent)
            if (unifiedTeleportTimer == 0 && passedSleepCheck.contains(playerUUID)) {
                // Timer ended, teleport all players who passed check
                DivineCore.LOGGER.info("统一梦境传送计时结束，传送所有玩家，数量: {}", passedSleepCheck.size());

                // Teleport all players who passed check
                for (UUID uuid : passedSleepCheck) {
                    ServerPlayer targetPlayer = player.getServer().getPlayerList().getPlayer(uuid);
                    if (targetPlayer != null && !targetPlayer.isRemoved()) {
                        teleportToDream(targetPlayer);
                    }
                }

                // Clear all states
                resetAllStates();
            }
        }
    }

    /**
     * Handle server tick, decrement unified teleport timer
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) {
        // Handle unified teleport timer countdown
        if (unifiedTeleportTimer > 0) {
            unifiedTeleportTimer--;
            if (unifiedTeleportTimer == 0) {
                DivineCore.LOGGER.info("统一梦境传送计时器倒计时结束");
            }
        }
    }

    /**
     * Reset all states to initial state
     */
    private static void resetAllStates() {
        delayedSleepCheck.clear();
        passedSleepCheck.clear();
        playerBedPositions.clear(); // Clear bed position records
        unifiedTeleportTimer = -1;
        isGlobalSleepCheck = false;
    }

    public static Vec3 getPlayerBedPosition(UUID playerUUID) {
        return playerBedPositions.get(playerUUID);
    }

    /**
     * Teleport player to dream dimension
     */
    private static void teleportToDream(ServerPlayer player) {
        try {
            // Get dream dimension
            ResourceKey<net.minecraft.world.level.Level> dreamDimension = ResourceKey.create(
                    Registries.DIMENSION,
                    ResourceLocation.parse(DivineCore.MODID + ":" + "dream"));

            ServerLevel dreamLevel = player.getServer().getLevel(dreamDimension);

            if (dreamLevel != null) {
                double angleX = random.nextDouble() * 2 * Math.PI;
                double distanceX = 100.0 + random.nextDouble() * 100.0;

                double x = player.getX() + Math.cos(angleX) * distanceX;
                double z = player.getZ() + Math.sin(angleX) * distanceX;

                double center = player.getY();

                // Original intervals (skip ±100 safe zone, take 100~200 distance)
                double lMin = center - 200.0, lMax = center - 100.0; 
                double uMin = center + 100.0, uMax = center + 200.0; 

                // Strictly clamp to [0,300]
                lMin = Math.max(0.0, lMin);
                lMax = Math.min(300.0, lMax);
                uMin = Math.max(0.0, uMin);
                uMax = Math.min(300.0, uMax);

                // Calculate valid length (0 if invalid)
                double lLen = Math.max(0.0, lMax - lMin);
                double uLen = Math.max(0.0, uMax - uMin);

                double y;
                if (lLen <= 0.0 && uLen <= 0.0) {
                    // Both segments clipped, fallback
                    y = center;
                } else {
                    // Uniform sampling on total length
                    double total = lLen + uLen;
                    double r = random.nextDouble() * total;
                    if (r < lLen) {
                        y = lMin + r;
                    } else {
                        y = uMin + (r - lLen);
                    }
                }

                // Execute teleport
                player.teleportTo(dreamLevel, x, y, z, player.getYRot(), player.getXRot());

                DivineCore.LOGGER.info("玩家 {} 已传送到梦境维度，坐标: ({}, {}, {})",
                        player.getName().getString(), x, y, z);

            } else {
                DivineCore.LOGGER.warn("无法找到dream维度，传送失败");
            }
        } catch (Exception e) {
            DivineCore.LOGGER.error("传送玩家到dream维度时发生错误", e);
        }
    }
}
