package com.petitioner0.divinecore.edicts.fifteenth_edicts;

import com.petitioner0.divinecore.DivineCore;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ViewportEvent;

@EventBusSubscriber(modid = DivineCore.MODID, value = Dist.CLIENT)
public class BloodMoonClient {


    private static final float BLOOD_NEAR_AT_PEAK = 4f;    // The nearest fog face at the peak
    private static final float BLOOD_FAR_AT_PEAK  = 16f;   // The farthest fog face at the peak
    // Configuration: blood moon window and color
    private static final int START_TICK = 11000;
    private static final int PEAK_TICK  = 18000;   // <- The deepest red, the thickest fog
    private static final int END_TICK   = 23000;

    //use a dark red to avoid over exposure: RGB(138, 0, 0) -> normalized
    private static final float TARGET_R = 138f / 255f;
    private static final float TARGET_G = 0f;
    private static final float TARGET_B = 0f;

    public static boolean isBloodMoonWindow(ClientLevel level) {
        if (level == null) return false;
        if (level.dimension() != net.minecraft.world.level.Level.OVERWORLD) return false;
    
        long day = level.getDayTime() / 24000L;
        boolean fullMoon = level.getMoonPhase() == 0; // Full moon
        long time = level.getDayTime() % 24000L;
        boolean inWindow = time >= START_TICK && time <= END_TICK;
    
        // Skip the first full moon (day==8)
        if (fullMoon && day == 8 || day == 1 || day == 0) {
            return false;
        }
    
        return fullMoon && inWindow;
    }

    private static float bloodMoonStrength(ClientLevel level, float partialTick) {
        long t = level.getDayTime() % 24000L;
        if (t < START_TICK || t > END_TICK) return 0f;

        float s;
        if (t <= PEAK_TICK) {
            // Rising segment: 11000 -> 18000
            s = (t - START_TICK) / (float)(PEAK_TICK - START_TICK);
        } else {
            // Falling segment: 18000 -> 23000
            s = (END_TICK - t) / (float)(END_TICK - PEAK_TICK);
        }
        // For a smoother effect, use an ease function (can be removed to get a linear effect)
        return easeInOutCubic(Mth.clamp(s, 0f, 1f));
    }

    /** Ease function: cubic bezier easing in and out */
    private static float easeInOutCubic(float t) {
        return t < 0.5f ? 4f * t * t * t : 1f - (float) Math.pow(-2f * t + 2f, 3) / 2f;
    }

    /** Adjust the fog color */
    @SubscribeEvent
    public static void onComputeFogColor(ViewportEvent.ComputeFogColor event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !isBloodMoonWindow(level)) return;

        float t = bloodMoonStrength(level, (float) event.getPartialTick());
        
        // Original values
        float r = event.getRed();
        float g = event.getGreen();
        float b = event.getBlue();
        // Interpolate towards the target color
        float nr = Mth.lerp(t, r, TARGET_R);
        float ng = Mth.lerp(t, g, TARGET_G);
        float nb = Mth.lerp(t, b, TARGET_B);

        event.setRed(nr);
        event.setGreen(ng);
        event.setBlue(nb);
    }

    /** Adjust the fog depth */
    @SubscribeEvent
    public static void onRenderFog(ViewportEvent.RenderFog event) {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null || !isBloodMoonWindow(level)) return;

        float t = bloodMoonStrength(level, (float) event.getPartialTick());
        if (t <= 0f) return;

        float near = event.getNearPlaneDistance();
        float far  = event.getFarPlaneDistance();

        float newNear = Mth.lerp(t, near, BLOOD_NEAR_AT_PEAK);
        float newFar  = Mth.lerp(t, far,  BLOOD_FAR_AT_PEAK);

        newNear = Math.max(0.05f, newNear);  
        // If the interpolation results in near >= far, force giving far a little extra space
        if (newNear >= newFar) {
            newFar = newNear + 0.5f; 
        }

        event.setCanceled(true);
        event.setNearPlaneDistance(newNear);
        event.setFarPlaneDistance(newFar);
    }

}