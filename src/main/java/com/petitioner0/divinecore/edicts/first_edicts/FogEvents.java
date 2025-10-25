package com.petitioner0.divinecore.edicts.first_edicts;

import com.petitioner0.divinecore.DivineCore;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(value = Dist.CLIENT, modid = DivineCore.MODID)
public class FogEvents {

    @SubscribeEvent
    public static void onFogColors(ViewportEvent.ComputeFogColor event) {
        Level level = event.getCamera().getEntity().level();
        if (level.dimension().location().equals(ResourceLocation.parse(DivineCore.MODID + ":" + "dream"))) {
            // Set the custom fog color for the dream dimension (R,G,B)
            event.setRed(0.8f);
            event.setGreen(0.7f);
            event.setBlue(1.0f);
        }
    }

    @SubscribeEvent
    public static void onFogRender(ViewportEvent.RenderFog event) {
        Level level = Minecraft.getInstance().level;
        if (level != null
                && level.dimension().location().equals(ResourceLocation.parse(DivineCore.MODID + ":" + "dream"))) {
            // Set the fog rendering parameters for the dream dimension
            event.setNearPlaneDistance(0.0f); 
            event.setFarPlaneDistance(50.0f); 
            event.setCanceled(true); 
        }
    }
}
