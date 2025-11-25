package com.petitioner0.divinecore.edicts.first_edicts;

import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.items.ItemHelper;
import net.minecraft.server.TickTask;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.ServerLevel;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FirstEdicts {

    private static final Map<UUID, Boolean> playerInDreamDimension = new HashMap<>();
    private static final Map<UUID, Integer> playerStayTimers = new HashMap<>();
    private static final Map<UUID, Integer> playerMaxStayTime = new HashMap<>();
    private static final Map<UUID, Vec3> playerOriginalPositions = new HashMap<>();

    private static final int BASE_STAY_TIME = 400;   // 20 sec
    private static final int FLOWER_BONUS_TIME = 200;

    public static void init(IEventBus modEventBus, IEventBus gameEventBus) {
        FirstEdicts instance = new FirstEdicts();
        gameEventBus.register(instance);
    }

    @SubscribeEvent
    public void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        Level newLevel = player.level();
        UUID playerUUID = player.getUUID();

        // ========================
        //   玩家进入梦境维度
        // ========================
        if (isDreamDimension(newLevel)) {
            playerInDreamDimension.put(playerUUID, true);

            if (player instanceof ServerPlayer serverPlayer) {

                // ⚠ 镜像复制已搬到 SleepEvents 里执行
                // 这里不再调用 DreamWorldMirrorUtil.copy3x3()

                // 记录返回点
                BlockPos spawnPos = serverPlayer.getRespawnPosition();
                if (spawnPos != null) {
                    ServerLevel spawnLevel = serverPlayer.getServer()
                            .getLevel(serverPlayer.getRespawnDimension());
                    if (spawnLevel != null) {
                        playerOriginalPositions.put(
                                playerUUID,
                                new Vec3(
                                        spawnPos.getX() + 0.5,
                                        spawnPos.getY(),
                                        spawnPos.getZ() + 0.5
                                )
                        );
                    }
                } else {
                    playerOriginalPositions.put(
                            playerUUID,
                            new Vec3(player.getX(), player.getY(), player.getZ())
                    );
                }

                // 在主世界掉落道具（原本逻辑保留）
                ServerLevel overworld = serverPlayer.getServer().getLevel(Level.OVERWORLD);
                if (overworld != null) {
                    BlockPos overworldSpawnPos =
                            spawnPos != null ? spawnPos : overworld.getSharedSpawnPos();
                    Vec3 dropPos = new Vec3(
                            overworldSpawnPos.getX() + 0.5,
                            overworldSpawnPos.getY(),
                            overworldSpawnPos.getZ() + 0.5
                    );
                    ItemHelper.dropItemAt(serverPlayer.serverLevel(), dropPos,
                            "aetherial_reverie", 1);
                }

                // 计算梦境停留最大时长
                int maxStayTime = calculateMaxStayTime(serverPlayer);
                playerMaxStayTime.put(playerUUID, maxStayTime);
                playerStayTimers.put(playerUUID, 0);

                DivineCore.LOGGER.info(
                        "玩家 {} 进入梦境维度，最大停留时间 {} 秒",
                        player.getName().getString(), maxStayTime / 20
                );
            }

            return;
        }

        boolean wasInDream = playerInDreamDimension.getOrDefault(playerUUID, false);
        playerInDreamDimension.put(playerUUID, false);

        playerStayTimers.remove(playerUUID);
        playerMaxStayTime.remove(playerUUID);
        playerOriginalPositions.remove(playerUUID);

        if (player instanceof ServerPlayer serverPlayer) {

            ServerLevel fromLevel = serverPlayer.getServer().getLevel(event.getFrom());

            if (fromLevel != null && isDreamDimension(fromLevel) && wasInDream) {

                MinecraftServer server = fromLevel.getServer();
                server.tell(new TickTask(server.getTickCount() + 60, () -> {

                    if (fromLevel.players().isEmpty()) {
                        DivineCore.LOGGER.info("梦境维度无人，开始清空目录");
                        DreamWorldMirrorUtil.clearEntireDreamDimension(fromLevel);
                    } else {
                        DivineCore.LOGGER.warn("梦境维度仍有人，取消清空目录");
                    }

                }));
            }
        }
    }

    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();

        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            UUID uuid = player.getUUID();

            if (playerInDreamDimension.getOrDefault(uuid, false)) {
                int stayTimer = playerStayTimers.getOrDefault(uuid, 0);
                int maxStay = playerMaxStayTime.getOrDefault(uuid, BASE_STAY_TIME);

                stayTimer++;

                if (stayTimer >= maxStay) {
                    teleportPlayerBack(player);
                    continue;
                }

                playerStayTimers.put(uuid, stayTimer);
            }
        }
    }

    private int calculateMaxStayTime(ServerPlayer player) {

        int totalTime = BASE_STAY_TIME;
        BlockPos spawnPos = player.getRespawnPosition();
        if (spawnPos == null) return totalTime;

        ServerLevel spawnLevel = player.getServer().getLevel(player.getRespawnDimension());
        if (spawnLevel == null) return totalTime;

        int flowerCount = 0;
        for (int x = -3; x <= 3; x++) {
            for (int y = -3; y <= 3; y++) {
                for (int z = -3; z <= 3; z++) {
                    BlockPos pos = spawnPos.offset(x, y, z);
                    BlockState state = spawnLevel.getBlockState(pos);
                    if (isFlowerOrPottedFlower(state)) flowerCount++;
                }
            }
        }

        return totalTime + flowerCount * FLOWER_BONUS_TIME;
    }

    private boolean isFlowerOrPottedFlower(BlockState state) {
        if (state.is(BlockTags.FLOWERS)) return true;
        if (state.getBlock() instanceof FlowerPotBlock pot) {
            Block content = pot.getPotted();
            return content.defaultBlockState().is(BlockTags.FLOWERS);
        }
        return false;
    }

    private void teleportPlayerBack(ServerPlayer player) {
        UUID uuid = player.getUUID();

        try {
            ServerLevel overworld = player.getServer().getLevel(Level.OVERWORLD);
            if (overworld == null) return;

            Vec3 pos = playerOriginalPositions.get(uuid);
            if (pos == null) {
                BlockPos spawn = player.getRespawnPosition();
                if (spawn != null) {
                    pos = new Vec3(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
                } else {
                    BlockPos worldSpawn = overworld.getSharedSpawnPos();
                    pos = new Vec3(worldSpawn.getX() + 0.5, worldSpawn.getY(),
                            worldSpawn.getZ() + 0.5);
                }
            }

            player.teleportTo(overworld, pos.x, pos.y, pos.z,
                    player.getYRot(), player.getXRot());

            playerInDreamDimension.put(uuid, false);
            playerStayTimers.remove(uuid);
            playerMaxStayTime.remove(uuid);
            playerOriginalPositions.remove(uuid);

            player.removeEffect(MobEffects.LEVITATION);

            DivineCore.LOGGER.info("玩家 {} 在梦境停留结束，已返回主世界",
                    player.getName().getString());

        } catch (Exception e) {
            DivineCore.LOGGER.error("传送回主世界错误", e);
        }
    }

    private boolean isDreamDimension(Level level) {
        return level.dimension().location().toString().equals("divinecore:dream");
    }
}
