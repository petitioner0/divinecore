package com.petitioner0.divinecore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.Item;

import java.util.*;

public class OrbitingRenderManager {
    private static final List<OrbitingRenderInstance> ACTIVE = new ArrayList<>();

    public static void add(OrbitingRenderInstance inst) {
        ACTIVE.add(inst);
    }

    public static void remove(UUID owner) {
        ACTIVE.removeIf(i -> i.getOwnerUUID().equals(owner));
    }

    public static void remove(UUID owner, Item item) {
        ACTIVE.removeIf(i -> i.getOwnerUUID().equals(owner) && i.getItem().getItem().equals(item));
    }

    public static void tick() {
        ACTIVE.removeIf(OrbitingRenderInstance::isExpired);
        for (OrbitingRenderInstance inst : ACTIVE) inst.tick();
    }

    public static void renderAll(PoseStack poseStack, MultiBufferSource buffer, float partialTicks) {
        for (OrbitingRenderInstance inst : ACTIVE)
            inst.render(poseStack, buffer, partialTicks);
    }
}