package com.petitioner0.divinecore.edicts.twelfth_edicts;

import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.effects.ModEffects;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.EntityTeleportEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;

@EventBusSubscriber(modid = DivineCore.MODID)
public class EnderPearlTeleportHandler {

    private static final double DISTANCE_THRESHOLD = 40.0D;

    @SubscribeEvent
    public static void onEnderPearlTeleport(EntityTeleportEvent.EnderPearl event) {
        ServerPlayer player = event.getPlayer();
        if (player == null) {
            return; 
        }

        double distance = event.getPrev().distanceTo(event.getTarget());

        if (distance >= DISTANCE_THRESHOLD) {
            player.addEffect(new MobEffectInstance(ModEffects.Spacilabile, 800, 0, false, true, true));

            TwelfthEdicts.CheckAndGiveReward(player);
        }
    }
}