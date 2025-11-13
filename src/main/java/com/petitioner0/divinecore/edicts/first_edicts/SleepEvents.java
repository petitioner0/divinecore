package com.petitioner0.divinecore.edicts.first_edicts;

import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.FTBHelper;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerWakeUpEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.*;

public class SleepEvents {

    private static final Map<UUID, Integer> delayedSleepCheck = new HashMap<>();
    private static final Set<UUID> passedSleepCheck = new HashSet<>();
    private static final Map<UUID, Vec3> playerBedPositions = new HashMap<>();

    private static int unifiedTeleportTimer = -1;
    private static boolean isGlobalSleepCheck = false;

    private static final Random random = new Random();

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        UUID uuid = player.getUUID();

        if (player.isSleeping() && !isGlobalSleepCheck) {
            boolean allSleeping = player.server.getPlayerList().getPlayers().stream()
                    .allMatch(ServerPlayer::isSleeping);

            if (allSleeping) {
                isGlobalSleepCheck = true;
                delayedSleepCheck.clear();

                for (ServerPlayer p : player.server.getPlayerList().getPlayers()) {
                    delayedSleepCheck.put(p.getUUID(), 2); // 延迟2tick再判定
                    playerBedPositions.put(p.getUUID(), new Vec3(p.getX(), p.getY(), p.getZ()));

                    FTBHelper.completeTask(p, "6906E5C429DFB835");
                }


                DivineCore.LOGGER.info("检测到所有玩家都在睡觉，开始全局梦境检测，共 {} 人", delayedSleepCheck.size());
            }
        }

        if (delayedSleepCheck.containsKey(uuid)) {
            int delay = delayedSleepCheck.get(uuid);

            if (delay <= 0) {
                if (player.isSleeping()) {
                    passedSleepCheck.add(uuid);
                    DivineCore.LOGGER.info("玩家 {} 通过睡眠判定", player.getName().getString());
                } else {
                    DivineCore.LOGGER.info("玩家 {} 未通过睡眠判定，重置检测", player.getName().getString());
                    resetAllStates();
                    return;
                }

                delayedSleepCheck.remove(uuid);

                // 所有玩家都判定完成
                if (delayedSleepCheck.isEmpty()) {
                    if (random.nextFloat() < 0.25f) {
                        unifiedTeleportTimer = 97;
                        DivineCore.LOGGER.info("所有玩家通过检测，25% 概率触发梦境传送，启动倒计时");
                    } else {
                        DivineCore.LOGGER.info("未触发梦境传送，重置状态");
                        resetAllStates();
                    }
                }
            } else {
                delayedSleepCheck.put(uuid, delay - 1);
            }
        }

        if (unifiedTeleportTimer == 0 && passedSleepCheck.contains(uuid)) {
            DivineCore.LOGGER.info("梦境传送计时结束，开始传送所有通过检测的玩家: {}", passedSleepCheck.size());

            for (UUID id : passedSleepCheck) {
                ServerPlayer target = player.server.getPlayerList().getPlayer(id);
                if (target != null && !target.isRemoved()) {
                    teleportToDream(target);
                }
            }
            resetAllStates();
        }
    }

    /**
     * 服务器tick，倒计时递减
     */
    @SubscribeEvent
    public static void onServerTick(ServerTickEvent event) {
        if (unifiedTeleportTimer > 0) {
            unifiedTeleportTimer--;
            if (unifiedTeleportTimer == 0) {
                DivineCore.LOGGER.info("梦境传送计时器结束");
            }
        }
    }

    @SubscribeEvent
    public static void onPlayerWakeUp(PlayerWakeUpEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            UUID uuid = player.getUUID();
            if (passedSleepCheck.contains(uuid) || delayedSleepCheck.containsKey(uuid)) {
                DivineCore.LOGGER.info("玩家 {} 醒来，取消梦境检测", player.getName().getString());
                resetAllStates();
            }
        }
    }

    private static void resetAllStates() {
        delayedSleepCheck.clear();
        passedSleepCheck.clear();
        unifiedTeleportTimer = -1;
        isGlobalSleepCheck = false;
        DivineCore.LOGGER.info("梦境检测状态已重置");
    }

    private static void teleportToDream(ServerPlayer player) {
        try {
            ResourceKey<net.minecraft.world.level.Level> dreamDimension = ResourceKey.create(
                    Registries.DIMENSION,
                    ResourceLocation.parse(DivineCore.MODID + ":" + "dream")
            );

            ServerLevel dreamLevel = player.getServer().getLevel(dreamDimension);
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
