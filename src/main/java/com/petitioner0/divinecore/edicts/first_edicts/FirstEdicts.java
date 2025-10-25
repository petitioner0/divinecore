package com.petitioner0.divinecore.edicts.first_edicts;

import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.items.ItemHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FirstEdicts {

    // Store the timer for the floating effect of the player
    private static final Map<UUID, Integer> playerFloatTimers = new HashMap<>();
    // Store the state of whether the player is in the dream dimension
    private static final Map<UUID, Boolean> playerInDreamDimension = new HashMap<>();
    // Store the stay time of the player in the dream dimension (in ticks)
    private static final Map<UUID, Integer> playerStayTimers = new HashMap<>();
    // Store the maximum停留时间 of the player in the dream dimension (in ticks)
    private static final Map<UUID, Integer> playerMaxStayTime = new HashMap<>();
    // 存储玩家传送前的位置信息（用于传送回主世界）
    private static final Map<UUID, Vec3> playerOriginalPositions = new HashMap<>();

    // The basic stay time: 20 seconds = 400 ticks
    private static final int BASE_STAY_TIME = 400;
    // The stay time increase per flower: 10 seconds = 200 ticks
    private static final int FLOWER_BONUS_TIME = 200;

    public static void init(IEventBus modEventBus, IEventBus gameEventBus) {
        FirstEdicts instance = new FirstEdicts();
        gameEventBus.register(instance);
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        Level level = player.level();

        // Check if the player is entering the dream dimension
        if (isDreamDimension(level)) {
            UUID playerUUID = player.getUUID();
            playerInDreamDimension.put(playerUUID, true);
            playerFloatTimers.put(playerUUID, 0);

            // Record the position of the player before teleporting (for returning to the main world)
            if (player instanceof ServerPlayer) {
                ServerPlayer serverPlayer = (ServerPlayer) player;
                // Get the player's spawn position or current position in the main world
                BlockPos spawnPos = serverPlayer.getRespawnPosition();
                if (spawnPos != null) {
                    // If there is a spawn point, use the spawn point coordinates
                    ServerLevel spawnLevel = serverPlayer.getServer().getLevel(serverPlayer.getRespawnDimension());
                    if (spawnLevel != null) {
                        playerOriginalPositions.put(playerUUID,
                                new Vec3(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5));
                    }
                } else {
                    // If there is no spawn point, use the current position
                    playerOriginalPositions.put(playerUUID, new Vec3(player.getX(), player.getY(), player.getZ()));
                }

                // Determine the drop position: use the spawn point first, otherwise use the world spawn point
                BlockPos dropAt = spawnPos != null
                    ? spawnPos
                    : serverPlayer.serverLevel().getSharedSpawnPos();
                ItemHelper.dropItemAt(serverPlayer.serverLevel(),
                    new Vec3(dropAt.getX(), dropAt.getY(), dropAt.getZ()),
                    "aetherial_reverie", 1);

                // Calculate and set the maximum stay time of the player
                int maxStayTime = calculateMaxStayTime(serverPlayer);
                playerMaxStayTime.put(playerUUID, maxStayTime);
                playerStayTimers.put(playerUUID, 0);

                // Immediately give the floating effect
                serverPlayer.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 200, 17)); 
                // Record the log
                DivineCore.LOGGER.info("玩家 {} 进入梦境维度，最大停留时间: {} 秒",
                        player.getName().getString(), maxStayTime / 20);
            }
        } else {
            // The player is leaving the dream dimension
            UUID playerUUID = player.getUUID();
            playerInDreamDimension.put(playerUUID, false);
            playerFloatTimers.remove(playerUUID);
            playerStayTimers.remove(playerUUID);
            playerMaxStayTime.remove(playerUUID);
            playerOriginalPositions.remove(playerUUID);

            // Remove the floating effect
            if (player instanceof ServerPlayer) {
                ((ServerPlayer) player).removeEffect(MobEffects.LEVITATION);
            }
        }
    }

    /**
     * Listen to the server tick event, handle the floating effect loop and stay time check
     */
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        // Handle the floating effect loop and stay time check
        MinecraftServer server = event.getServer();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID playerUUID = player.getUUID();

            // Check if the player is in the dream dimension
            if (playerInDreamDimension.getOrDefault(playerUUID, false)) {
                // Handle the floating effect loop
                int timer = playerFloatTimers.getOrDefault(playerUUID, 0);
                timer++;

                // 20 seconds is a complete cycle: 10 seconds floating + 10 seconds blank
                if (timer >= 400) { 
                    timer = 0;
                }

                // The first 10 seconds give the floating effect
                if (timer < 200) { // 200 ticks = 10秒
                    if (timer == 0 || !player.hasEffect(MobEffects.LEVITATION)) {
                        player.addEffect(new MobEffectInstance(MobEffects.LEVITATION, 200, 17)); // 10秒，等级18
                    }
                } else {
                    // The last 10 seconds remove the floating effect
                    if (player.hasEffect(MobEffects.LEVITATION)) {
                        player.removeEffect(MobEffects.LEVITATION);
                    }
                }

                playerFloatTimers.put(playerUUID, timer);

                // Handle the stay time check
                int stayTimer = playerStayTimers.getOrDefault(playerUUID, 0);
                int maxStayTime = playerMaxStayTime.getOrDefault(playerUUID, BASE_STAY_TIME);

                stayTimer++;

                // Check if it is timed out
                if (stayTimer >= maxStayTime) {
                    // The time is up, teleport the player back to the main world
                    teleportPlayerBack(player);
                    continue;
                }

                playerStayTimers.put(playerUUID, stayTimer);
            }
        }
    }

    /**
     * Calculate the maximum stay time of the player in the dream dimension
     * The basic time is 40 seconds, each flower adds 20 seconds
     */
    private int calculateMaxStayTime(ServerPlayer player) { 
        int totalTime = BASE_STAY_TIME; // The basic 40 seconds

        BlockPos spawnPos = player.getRespawnPosition();
        if (spawnPos == null) {
            return totalTime; 
        }

        // Check the flowers in the 3x3x3 area around the spawn point
        int flowerCount = 0;
        ServerLevel spawnLevel = player.getServer().getLevel(player.getRespawnDimension());
        if (spawnLevel == null) {
            return totalTime;
        }

        // Check the 3x3x3 area (centered on the spawn point)
        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos checkPos = spawnPos.offset(x, y, z);
                    BlockState state = spawnLevel.getBlockState(checkPos);

                    // Check if it is a flower-like plant
                    if (isFlowerOrPottedFlower(state)) {
                        flowerCount++;
                    }
                }
            }
        }

        // Each flower adds 20 seconds
        totalTime += flowerCount * FLOWER_BONUS_TIME;

        return totalTime;
    }

    /**
     * Check if the block is a flower-like plant
     */
    private boolean isFlowerOrPottedFlower(BlockState state) {
        // 1) It is a flower (including high flowers, etc.). The official has also tagged FLOWERS
        if (state.is(BlockTags.FLOWERS)) {
            return true;
        }

        // 2) Flower pot block: read the content block, then use the FLOWERS tag to判断
        if (state.getBlock() instanceof FlowerPotBlock pot) {
            Block content = pot.getPotted(); 
            if (content.defaultBlockState().is(BlockTags.FLOWERS)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Teleport the player back to the main world
     */
    private void teleportPlayerBack(ServerPlayer player) {
        UUID playerUUID = player.getUUID();

        try {
            // Get the main world
            ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
            if (overworld == null) {
                DivineCore.LOGGER.error("无法找到主世界，传送失败");
                return;
            }

            // Get the player's original position
            Vec3 originalPos = playerOriginalPositions.get(playerUUID);
            if (originalPos == null) {
                // If there is no recorded position, use the spawn point
                BlockPos spawnPos = player.getRespawnPosition();
                if (spawnPos != null) {
                    originalPos = new Vec3(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5);
                } else {
                    // If there is no spawn point, use the world spawn point
                    originalPos = new Vec3(overworld.getSharedSpawnPos().getX() + 0.5,
                            overworld.getSharedSpawnPos().getY(),
                            overworld.getSharedSpawnPos().getZ() + 0.5);
                }
            }

            // Execute the teleport
            player.teleportTo(overworld, originalPos.x, originalPos.y, originalPos.z,
                    player.getYRot(), player.getXRot());

            // Clean up the player data
            playerInDreamDimension.put(playerUUID, false);
            playerFloatTimers.remove(playerUUID);
            playerStayTimers.remove(playerUUID);
            playerMaxStayTime.remove(playerUUID);
            playerOriginalPositions.remove(playerUUID);

            // Remove the floating effect
            player.removeEffect(MobEffects.LEVITATION);

            DivineCore.LOGGER.info("玩家 {} 在梦境维度停留时间已到，已传送回主世界",
                    player.getName().getString());

        } catch (Exception e) {
            DivineCore.LOGGER.error("传送玩家回主世界时发生错误", e);
        }
    }

    /**
     * Check if it is the dream dimension
     */
    private boolean isDreamDimension(Level level) {
        // 根据data文件夹中的配置，梦境维度的ID为"divinecore:dream"
        ResourceLocation dimensionId = level.dimension().location();
        return dimensionId.toString().equals("divinecore:dream");
    }


    
}
