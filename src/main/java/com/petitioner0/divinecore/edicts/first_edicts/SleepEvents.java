package com.petitioner0.divinecore.edicts.first_edicts;

import com.petitioner0.divinecore.DivineCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;

import java.util.*;


@EventBusSubscriber(modid = DivineCore.MODID)
public class SleepEvents {

    private static final Set<UUID> naturalWakePlayers = new HashSet<>();
    private static final Random random = new Random();

    @SubscribeEvent
    public static void onPlayerWake(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        UUID uuid = player.getUUID();
        naturalWakePlayers.add(uuid);

        DivineCore.LOGGER.info("玩家 {} 自然醒来，当前自然醒人数={}", player.getName().getString(), naturalWakePlayers.size());

        // 检查是否所有在线玩家都自然醒
        List<ServerPlayer> all = player.server.getPlayerList().getPlayers();

        boolean allAwake = all.stream().allMatch(p -> naturalWakePlayers.contains(p.getUUID()));
        if (!allAwake) return;

        DivineCore.LOGGER.info("所有玩家自然醒来，进行梦境概率判定");

        // 25% 触发
        if (random.nextFloat() < 0.25f) {
            DivineCore.LOGGER.info("梦境事件触发，正在传送玩家进入梦境...");
            for (ServerPlayer p : all) {
                teleportToDream(p);
            }
        } else {
            DivineCore.LOGGER.info("梦境事件未触发");
        }

        reset();
    }

    private static void reset() {
        naturalWakePlayers.clear();
        DivineCore.LOGGER.info("梦境检测已重置");
    }

    private static void teleportToDream(ServerPlayer player) {
        try {

            ServerLevel from = player.serverLevel();   // 玩家当前维度
            DreamWorldCache.BLOCK_CACHE.put(player.getUUID(), DreamWorldMirrorUtil.read3x3(from, player.blockPosition()));
            DivineCore.LOGGER.info("已缓存玩家 {} 周围 3×3 chunk", player.getName().getString());

            ResourceKey<Level> dreamDimension = ResourceKey.create(Registries.DIMENSION, ResourceLocation.parse(DivineCore.MODID + ":" + "dream"));
            ServerLevel dreamLevel = player.getServer().getLevel(dreamDimension);

            if (dreamLevel == null) {
                DivineCore.LOGGER.warn("无法找到 dream 维度，传送失败");
                return;
            }
            int baseChunkX = player.chunkPosition().x;
            int baseChunkZ = player.chunkPosition().z;

            int offX = random.nextInt(3) - 1;
            int offZ = random.nextInt(3) - 1;

            int targetChunkX = baseChunkX + offX;
            int targetChunkZ = baseChunkZ + offZ;

            int blockX = (targetChunkX << 4) + random.nextInt(16);
            int blockZ = (targetChunkZ << 4) + random.nextInt(16);

            int topY = dreamLevel.getMaxBuildHeight();

            double x = blockX + 0.5;
            double y = topY - 1;
            double z = blockZ + 0.5;

            player.teleportTo(dreamLevel, x, y, z, player.getYRot(), player.getXRot());

            MinecraftServer server = player.getServer();
            if (server == null) return;

            server.execute(new Runnable() {
                int wait = 5; // 延迟 5 tick

                @Override
                public void run() {
                    // 还要继续等
                    if (wait > 0) {
                        wait--;                // 先减
                        server.execute(this);  // 再把自己丢到下一个 tick
                        return;
                    }

                    // === 延迟结束，正式写入镜像 ===
                    List<BlockState> data = DreamWorldCache.BLOCK_CACHE.remove(player.getUUID());
                    if (data == null) {
                        DivineCore.LOGGER.warn("无法找到缓存的镜像数据");
                        return;
                    }

                    ServerLevel dreamLevel = player.serverLevel(); // 此时玩家已经在梦境
                    DreamWorldMirrorUtil.write3x3(dreamLevel, player.blockPosition(), data);
                    DivineCore.LOGGER.info("梦境镜像构建完成！");
                }
            });

        } catch (Exception e) {
            DivineCore.LOGGER.error("传送至梦境维度时发生错误", e);
        }
    }
}
