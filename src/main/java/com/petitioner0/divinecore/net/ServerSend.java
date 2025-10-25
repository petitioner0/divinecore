package com.petitioner0.divinecore.net;

import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

public class ServerSend {
    
    public static void sendSunGazeProgress(ServerPlayer player, float progress) {
        PacketDistributor.sendToPlayer(player, new S2CSetSunGazeProgress(progress));
    }
}
