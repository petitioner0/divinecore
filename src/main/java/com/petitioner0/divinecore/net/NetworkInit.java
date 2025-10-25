package com.petitioner0.divinecore.net;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;
import com.petitioner0.divinecore.DivineCore;

@EventBusSubscriber(modid = DivineCore.MODID)
public class NetworkInit {
    
    /**
     * Registers all network payloads for client-server communication.
     */
    @SubscribeEvent
    public static void register(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1"); 

        // Used to synchronize sun gaze progress with the client overlay
        registrar.playToClient(
            S2CSetSunGazeProgress.TYPE,
            S2CSetSunGazeProgress.STREAM_CODEC,
            (payload, ctx) -> {
                ctx.enqueueWork(() -> {
                    com.petitioner0.divinecore.client.SunGazeOverlay.setProgress01(payload.progress());
                });
            }
        );
        // Used to create orbiting item effects around the player
        registrar.playToClient(
            S2CSpawnOrbitingItem.TYPE,
            S2CSpawnOrbitingItem.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(payload::handleClient)
        );
        // Used to remove orbiting item effects from the player
        registrar.playToClient(
            S2CRemoveOrbitingItem.TYPE,
            S2CRemoveOrbitingItem.STREAM_CODEC,
            (payload, ctx) -> ctx.enqueueWork(payload::handleClient)
        );
    }
}
