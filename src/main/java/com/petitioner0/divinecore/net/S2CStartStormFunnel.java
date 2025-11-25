package com.petitioner0.divinecore.net;

import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.client.StormEffect;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;

public record S2CStartStormFunnel(double x, double y, double z) implements CustomPacketPayload {

    public static final Type<S2CStartStormFunnel> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath(DivineCore.MODID, "start_storm_funnel"));

    public static final StreamCodec<FriendlyByteBuf, S2CStartStormFunnel> STREAM_CODEC =
            StreamCodec.of(S2CStartStormFunnel::write, S2CStartStormFunnel::read);

    private static S2CStartStormFunnel read(FriendlyByteBuf buf) {
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        return new S2CStartStormFunnel(x, y, z);
    }

    private static void write(FriendlyByteBuf buf, S2CStartStormFunnel payload) {
        buf.writeDouble(payload.x);
        buf.writeDouble(payload.y);
        buf.writeDouble(payload.z);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    /**
     * 客户端处理：启动粒子风暴
     */
    public void handleClient() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        Vec3 center = new Vec3(this.x, this.y, this.z);
        StormEffect.spawnStorm(level, center);
    }
}
