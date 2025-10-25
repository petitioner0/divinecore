package com.petitioner0.divinecore.net;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

public record S2CSetSunGazeProgress(float progress) implements CustomPacketPayload {
    public static final Type<S2CSetSunGazeProgress> TYPE =
        new Type<>(ResourceLocation.fromNamespaceAndPath("divinecore", "sun_gaze_progress"));

    public static final StreamCodec<ByteBuf, S2CSetSunGazeProgress> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.FLOAT, S2CSetSunGazeProgress::progress,
        S2CSetSunGazeProgress::new
    );

    @Override 
    public Type<? extends CustomPacketPayload> type() { 
        return TYPE; 
    }
}
