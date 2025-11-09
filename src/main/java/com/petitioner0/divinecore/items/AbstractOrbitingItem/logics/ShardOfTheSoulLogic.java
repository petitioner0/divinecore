package com.petitioner0.divinecore.items.AbstractOrbitingItem.logics;

import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.edicts.third_edicts.HealthData;
import com.petitioner0.divinecore.items.AbstractOrbitingItem.OrbitingItemManager;
import com.petitioner0.divinecore.items.ModItems;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@EventBusSubscriber(modid = DivineCore.MODID)
public class ShardOfTheSoulLogic implements IOrbitingLogic {
    
    // 跟踪每个玩家是否已经临时移除了惩罚
    private static final Map<UUID, Boolean> penaltyRemoved = new HashMap<>();
    
    @Override
    public void tick(Player player) {
        if (!(player instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        UUID playerId = player.getUUID();
        boolean isActive = OrbitingItemManager.hasOrbitingItem(playerId, ModItems.SHARD_OF_THE_SOUL.get());
        boolean wasRemoved = penaltyRemoved.getOrDefault(playerId, false);
        
        if (isActive && !wasRemoved) {
            // 激活时，临时移除生命值惩罚
            HealthData.temporarilyRemovePenalty(serverPlayer);
            penaltyRemoved.put(playerId, true);
        } else if (!isActive && wasRemoved) {
            // 取消激活时，重新应用生命值惩罚
            HealthData.reapplyPenalty(serverPlayer);
            penaltyRemoved.remove(playerId);
        }
    }
    
    /** 玩家死亡时清理状态 */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }
        
        UUID playerId = serverPlayer.getUUID();
        // 如果玩家死亡时有临时移除的惩罚，需要重新应用
        if (penaltyRemoved.remove(playerId) != null) {
            // 注意：此时玩家已经死亡，但我们需要确保状态被清理
            // 惩罚会在 HealthPenaltyHandler 中重新应用
        }
    }
}
