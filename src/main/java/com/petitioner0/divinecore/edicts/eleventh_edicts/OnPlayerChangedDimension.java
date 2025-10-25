package com.petitioner0.divinecore.edicts.eleventh_edicts;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.petitioner0.divinecore.DivineCore;


@EventBusSubscriber(modid = DivineCore.MODID)
public class OnPlayerChangedDimension {

    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;

        ResourceKey<Level> to = event.getTo();
        if (!Level.END.equals(to)) return; 

        ServerLevel endLevel = serverPlayer.server.getLevel(Level.END);
        if (endLevel == null) return;

        // Use the Overworld SavedData for a one-time mark (persisted with save)
        ServerLevel overworld = serverPlayer.server.getLevel(Level.OVERWORLD);
        if (overworld == null) return;

        OneTimeEndWitherData data = OneTimeEndWitherData.get(overworld);
        if (data.isTriggered()) {
            return; 
        }

        // First time a player enters the End -> spawn two withers near (0,0) at the top
        WitherSpawner.spawnTwoWithersNearOriginTop(endLevel);

        data.setTriggered(true); 
    }
}