package com.petitioner0.divinecore.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

import java.util.UUID;

public class OrbitingRenderInstance {
    
    private final UUID ownerUUID;
    private final ItemStack item;
    private final double startAngle;
    private final float radius;
    private final float height;
    private final float speed;
    private int tickCount = 0;

    public OrbitingRenderInstance(UUID ownerUUID, ItemStack item,
                                  float radius, float height, float speed) {
        this.ownerUUID = ownerUUID;
        this.item = item.copy();
        this.startAngle = Math.random() * Math.PI * 2;
        this.radius = radius;
        this.height = height;
        this.speed = speed;
    }

    public UUID getOwnerUUID() { return ownerUUID; }
    
    public ItemStack getItem() { return item; }

    public void tick() { tickCount++; }

    // No longer expires by time, only checks if player is dead
    public boolean isExpired() {
        var level = Minecraft.getInstance().level;
        if (level == null) return true;
        
        Player p = level.getPlayerByUUID(ownerUUID);
        return p == null || !p.isAlive();
    }

    public void render(PoseStack poseStack, MultiBufferSource buffer, float partialTicks) {
        var level = Minecraft.getInstance().level;
        if (level == null) return;
        
        Player p = level.getPlayerByUUID(ownerUUID);
        if (p == null) return;

        double time = level.getGameTime() + partialTicks;
        double angle = (time * speed + startAngle) % (Math.PI * 2);
        double dy = Math.sin(time * 0.1) * 0.15;
    
        double x = Math.cos(angle) * radius;
        double y = height + dy;
        double z = Math.sin(angle) * radius;
    
        var cam = Minecraft.getInstance().gameRenderer.getMainCamera();
        double camX = cam.getPosition().x;
        double camY = cam.getPosition().y;
        double camZ = cam.getPosition().z;
        
        double interpX = Mth.lerp(partialTicks, p.xo, p.getX());
        double interpY = Mth.lerp(partialTicks, p.yo, p.getY());
        double interpZ = Mth.lerp(partialTicks, p.zo, p.getZ());
        
        // Calculate final render position
        double finalX = interpX + x - camX;
        double finalY = interpY + y - camY;
        double finalZ = interpZ + z - camZ;
        
        
        poseStack.pushPose();
        poseStack.translate(finalX, finalY, finalZ);
        
        poseStack.scale(1,1,1);
        poseStack.mulPose(Axis.YP.rotationDegrees((tickCount + partialTicks) * 3f));

        
        ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
        renderer.renderStatic(item, ItemDisplayContext.GROUND,
            15728880, OverlayTexture.NO_OVERLAY,
            poseStack, buffer, level, 0);
        
        poseStack.popPose();
    }
}