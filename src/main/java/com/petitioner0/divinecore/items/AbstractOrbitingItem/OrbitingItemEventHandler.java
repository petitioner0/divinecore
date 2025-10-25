package com.petitioner0.divinecore.items.AbstractOrbitingItem;

import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.net.S2CSpawnOrbitingItem;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.Set;
import java.util.UUID;

@EventBusSubscriber(modid = DivineCore.MODID)
public class OrbitingItemEventHandler {

    @SubscribeEvent
    public static void onPlayerChangeDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        UUID playerId = serverPlayer.getUUID();
        serverPlayer.server.execute(() -> {
            Set<Item> items = OrbitingItemManager.getItemsFor(playerId);
            if (items == null || items.isEmpty()) return;
    
            for (Item item : items) {
                if (!(item instanceof AbstractOrbitingItem floatingItem)) continue;
                ItemStack stack = new ItemStack(item);
    
                PacketDistributor.sendToPlayer(serverPlayer,
                    new S2CSpawnOrbitingItem(
                        playerId,
                        stack,
                        floatingItem.getOrbitRadius(stack),
                        floatingItem.getOrbitHeight(stack),
                        floatingItem.getOrbitSpeed(stack)
                    )
                );
            }
        });
    }

    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) {
            return;
        }

        UUID playerId = serverPlayer.getUUID();
        // Clear the orbiting items data on the server
        OrbitingItemManager.clearOrbitingItems(playerId);
    }
}
