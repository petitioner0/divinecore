package com.petitioner0.divinecore.edicts.eighth_edicts;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.minecraft.world.level.storage.ServerLevelData;
import com.petitioner0.divinecore.DivineCore;

@EventBusSubscriber(modid = DivineCore.MODID)
public class WeatherReplaceModule {

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) return;

        // If it's raining and not thundering, switch to thunderstorm and align the thunderstorm duration to the remaining time of this rain
        if (level.isRaining() && !level.isThundering()) {
            ServerLevelData data = (ServerLevelData) level.getLevelData();

            int clearLeft = data.getClearWeatherTime(); 
            int rainLeft  = data.getRainTime();         // The remaining time of this rain (planned by vanilla)

            if (rainLeft <= 0) rainLeft = 1;

            level.setWeatherParameters(
                clearLeft,
                rainLeft,
                true,
                true
            );
        }
    }
}