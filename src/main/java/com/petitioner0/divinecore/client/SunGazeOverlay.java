package com.petitioner0.divinecore.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public final class SunGazeOverlay {

    // sync/local update
    private static volatile float targetAlpha = 0f;
    private static float renderAlpha = 0f;

    public static void setProgress01(float p) {
        if (Float.isNaN(p)) p = 0f;
        targetAlpha = Math.min(1f, Math.max(0f, p));
    }

    /** Callback registered by RegisterGuiLayersEvent: called by GUI every frame */
    public static void render(net.minecraft.client.gui.GuiGraphics gg, float partialTick) {
        // partialTick is the frame interpolation value in [0..1); approximate dt in seconds
        float dt = Math.min(0.1f, partialTick / 20.0f);

        float diff = targetAlpha - renderAlpha;
        float speed = Math.abs(diff) * 8.0f + 2.0f;
        renderAlpha += diff * Math.min(1.0f, speed * dt);

        if (renderAlpha <= 0.003f) return;

        int w = gg.guiWidth(), h = gg.guiHeight();
        int a = (int)(renderAlpha * 255f) & 0xFF;
        int argb = (a << 24) | 0xFFFFFF; // white screen

        com.mojang.blaze3d.systems.RenderSystem.enableBlend();
        gg.fill(0, 0, w, h, argb);
        com.mojang.blaze3d.systems.RenderSystem.disableBlend();
    }
}
