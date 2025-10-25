package com.petitioner0.divinecore.net;

import com.petitioner0.divinecore.client.OrbitingRenderManager;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.core.UUIDUtil;
import net.minecraft.world.item.Item;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.core.registries.Registries;

import java.util.UUID;

public record S2CRemoveOrbitingItem(UUID owner, Item item) implements CustomPacketPayload {
    public static final Type<S2CRemoveOrbitingItem> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("divinecore", "remove_orbiting_item"));

    public static final StreamCodec<RegistryFriendlyByteBuf, S2CRemoveOrbitingItem> STREAM_CODEC =
            StreamCodec.composite(
                    UUIDUtil.STREAM_CODEC, S2CRemoveOrbitingItem::owner,
                    ByteBufCodecs.registry(Registries.ITEM), S2CRemoveOrbitingItem::item,
                    S2CRemoveOrbitingItem::new
            );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handleClient() {
        OrbitingRenderManager.remove(owner, item);
    }
}