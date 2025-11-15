package com.petitioner0.divinecore.edicts.first_edicts;

import com.petitioner0.divinecore.DivineCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import java.util.*;

public class SleepEvents {

    private static final Set<UUID> naturalWakePlayers = new HashSet<>();
    private static final Random random = new Random();

    @SubscribeEvent
    public static void onPlayerWake(PlayerWakeUpEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // 必须是自然醒
        if (!event.updateLevel()) {
            DivineCore.LOGGER.info("玩家 {} 非自然醒来，重置状态", player.getName().getString());
            reset();
            return;
        }

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
            ResourceKey<net.minecraft.world.level.Level> dreamDimension = ResourceKey.create(
                    Registries.DIMENSION,
                    ResourceLocation.parse(DivineCore.MODID + ":" + "dream")
            );

            ServerLevel dreamLevel = Objects.requireNonNull(player.getServer()).getLevel(dreamDimension);
            if (dreamLevel == null) {
                DivineCore.LOGGER.warn("无法找到 dream 维度，传送失败");
                return;
            }

            double angle = random.nextDouble() * 2 * Math.PI;
            double distance = 100.0 + random.nextDouble() * 100.0;
            double x = player.getX() + Math.cos(angle) * distance;
            double z = player.getZ() + Math.sin(angle) * distance;

            double centerY = player.getY();
            double lMin = Math.max(0.0, centerY - 200.0);
            double lMax = Math.min(300.0, centerY - 100.0);
            double uMin = Math.max(0.0, centerY + 100.0);
            double uMax = Math.min(300.0, centerY + 200.0);

            double lLen = Math.max(0.0, lMax - lMin);
            double uLen = Math.max(0.0, uMax - uMin);
            double y;

            if (lLen <= 0 && uLen <= 0) {
                y = centerY;
            } else {
                double total = lLen + uLen;
                double r = random.nextDouble() * total;
                y = (r < lLen) ? lMin + r : uMin + (r - lLen);
            }

            player.teleportTo(dreamLevel, x, y, z, player.getYRot(), player.getXRot());
            DivineCore.LOGGER.info("玩家 {} 被传送至梦境维度 ({}, {}, {})", player.getName().getString(), x, y, z);

        } catch (Exception e) {
            DivineCore.LOGGER.error("传送至梦境维度时发生错误", e);
        }
    }
}
