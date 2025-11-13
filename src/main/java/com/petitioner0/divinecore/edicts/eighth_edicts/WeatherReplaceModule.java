package com.petitioner0.divinecore.edicts.eighth_edicts;

import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.minecraft.world.level.storage.ServerLevelData;
import com.petitioner0.divinecore.DivineCore;

@EventBusSubscriber(modid = DivineCore.MODID)
public class WeatherReplaceModule {

    private static boolean wasThundering = false;

    @SubscribeEvent
    public static void onLevelTick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level))
            return;

        boolean raining = level.isRaining();
        boolean thundering = level.isThundering();

        // 只在当前是雨、之前不是雷时触发一次
        if (raining && !thundering && !wasThundering) {
            ServerLevelData data = (ServerLevelData) level.getLevelData();
            int clearLeft = data.getClearWeatherTime();
            int rainLeft = data.getRainTime();
            if (rainLeft <= 0)
                rainLeft = 1;

            level.setWeatherParameters(clearLeft, rainLeft, true, true);
            wasThundering = true; // 标记进入雷暴状态
        }

        // 当停止下雨后，重置状态
        if (!raining) {
            wasThundering = false;
        }
    }
}