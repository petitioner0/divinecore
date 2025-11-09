package com.petitioner0.divinecore.edicts.eleventh_edicts;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import com.petitioner0.divinecore.DivineCore;

@EventBusSubscriber(modid = DivineCore.MODID)
public class OnPlayerChangedDimension {

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer sp)) return;

        ResourceKey<Level> to = event.getTo();
        if (!Level.END.equals(to)) return;

        ServerLevel end = sp.server.getLevel(Level.END);
        if (end == null) return;

        // 只在第一次进入时触发
        var data = EndBossSharedHealthData.get(end);
        if (data.ids().isEmpty()) {
            WitherSpawner.spawnTwoWithersNearOriginTop(end);
        }
    }
}