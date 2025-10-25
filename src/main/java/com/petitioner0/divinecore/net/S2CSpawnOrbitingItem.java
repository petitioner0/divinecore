package com.petitioner0.divinecore.net;

import com.petitioner0.divinecore.client.OrbitingRenderInstance;
import com.petitioner0.divinecore.client.OrbitingRenderManager;

import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.UUIDUtil;
import java.util.UUID;

public record S2CSpawnOrbitingItem(UUID owner, ItemStack item,
                                   float radius, float height, float speed)
        implements CustomPacketPayload {

    public static final Type<S2CSpawnOrbitingItem> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("divinecore", "spawn_orbiting_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CSpawnOrbitingItem> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, S2CSpawnOrbitingItem::owner,
                    ItemStack.STREAM_CODEC, S2CSpawnOrbitingItem::item,
                    ByteBufCodecs.FLOAT, S2CSpawnOrbitingItem::radius,
                    ByteBufCodecs.FLOAT, S2CSpawnOrbitingItem::height,
                    ByteBufCodecs.FLOAT, S2CSpawnOrbitingItem::speed,
                    S2CSpawnOrbitingItem::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handleClient() {
        OrbitingRenderManager.add(new OrbitingRenderInstance(owner, item, radius, height, speed));
    }
}