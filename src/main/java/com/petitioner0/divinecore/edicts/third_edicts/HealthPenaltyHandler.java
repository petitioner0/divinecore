package com.petitioner0.divinecore.edicts.third_edicts;

import com.petitioner0.divinecore.DivineCore;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerLoggedInEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerChangedDimensionEvent;

@EventBusSubscriber(modid = DivineCore.MODID)
public class HealthPenaltyHandler {

    /** Add one penalty when the player truly dies. If it is canceled by totem etc., it will not be triggered*/
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        Entity e = event.getEntity();
        if (!(e instanceof ServerPlayer player))
            return;
        if (event.isCanceled())
            return;
        HealthData.addPenaltyAndApply(player);
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!(event.getEntity() instanceof ServerPlayer newPlayer))
            return;

        // Only copy when cloning due to death
        if (event.isWasDeath()) {
            // Copy our root Tag from the original player
            var oldData = event.getOriginal().getPersistentData().getCompound("divinecore");
            newPlayer.getPersistentData().put("divinecore", oldData.copy());
        }
    }

    /** Reapply penalty modifier when player logs in to avoid失效。 */
    @SubscribeEvent
    public static void onLogin(PlayerLoggedInEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            HealthData.reapply(sp);
        }
    }

    @SubscribeEvent
    public static void onRespawn(PlayerRespawnEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            HealthData.reapply(sp);
        }
    }

    @SubscribeEvent
    public static void onChangeDim(PlayerChangedDimensionEvent event) {
        if (event.getEntity() instanceof ServerPlayer sp) {
            HealthData.reapply(sp);
        }
    }
}