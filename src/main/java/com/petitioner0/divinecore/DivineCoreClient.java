package com.petitioner0.divinecore;

import com.petitioner0.divinecore.client.OrbitingRenderManager;
import com.petitioner0.divinecore.client.SunGazeOverlay;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.minecraft.client.Minecraft;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.minecraft.client.renderer.MultiBufferSource;

@Mod(value = DivineCore.MODID, dist = Dist.CLIENT)
@EventBusSubscriber(modid = DivineCore.MODID, value = Dist.CLIENT)
public class DivineCoreClient {
    private static final ResourceLocation SUN_GAZE_ID =
            ResourceLocation.fromNamespaceAndPath(DivineCore.MODID, "sun_gaze");

    @SubscribeEvent
    public static void onRegisterLayers(net.neoforged.neoforge.client.event.RegisterGuiLayersEvent e) {
        e.registerAbove(net.neoforged.neoforge.client.gui.VanillaGuiLayers.HOTBAR,
                        SUN_GAZE_ID,
                        (gg, deltaTracker) -> SunGazeOverlay.render(gg, 1.0f));
    }

    @SubscribeEvent
    public static void clientTick(ClientTickEvent.Post event) {
        if (Minecraft.getInstance().level != null) {
            OrbitingRenderManager.tick();
        }
    }

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent e) {
        if (e.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;
        MultiBufferSource.BufferSource buffer = Minecraft.getInstance().renderBuffers().bufferSource(); 
        float pt = e.getPartialTick().getGameTimeDeltaPartialTick(false); 
        OrbitingRenderManager.renderAll(e.getPoseStack(), buffer, pt);
    }
}
