package com.petitioner0.divinecore.items.AbstractOrbitingItem.logics;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;

public class ShatteredRiftstoneLogic implements IOrbitingLogic {
    
    private static final double TELEPORT_DISTANCE = 50.0;
    private static final double TELEPORT_CHANCE = 0.01; // 1% 概率
    
    @Override
    public void tick(Player player) {
        // 只在服务端执行
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        // 1% 概率触发传送
        if (serverPlayer.getRandom().nextDouble() >= TELEPORT_CHANCE) {
            return;
        }
        
        // 获取玩家当前朝向角度
        float yaw = serverPlayer.getYRot();
        float pitch = serverPlayer.getXRot();
        
        // 转换为弧度
        float yawRad = (float) Math.toRadians(yaw);
        float pitchRad = (float) Math.toRadians(pitch);
        
        // 计算朝向方向向量
        // Minecraft 中：yaw 0° 是南（+Z），90° 是西（-X），180° 是北（-Z），270° 是东（+X）
        double dx = -Mth.sin(yawRad) * Mth.cos(pitchRad);
        double dy = -Mth.sin(pitchRad);
        double dz = Mth.cos(yawRad) * Mth.cos(pitchRad);
        
        // 计算目标位置（当前位置 + 方向向量 * 距离）
        double targetX = serverPlayer.getX() + dx * TELEPORT_DISTANCE;
        double targetY = serverPlayer.getY() + dy * TELEPORT_DISTANCE;
        double targetZ = serverPlayer.getZ() + dz * TELEPORT_DISTANCE;
        
        // 执行传送
        serverPlayer.teleportTo(
            targetX,
            targetY,
            targetZ
        );
    }
}
