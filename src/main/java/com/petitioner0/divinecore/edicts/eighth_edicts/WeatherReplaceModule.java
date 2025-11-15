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
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }

        // 不是下雨，直接无视
        if (!level.isRaining()) {
            return;
        }

        // 已经是雷暴了，也不需要管
        if (level.isThundering()) {
            return;
        }

        // 这里开始：把“纯雨”替换成“等长雷暴”
        ServerLevelData data = (ServerLevelData) level.getLevelData();

        int rainTime = data.getRainTime();
        if (rainTime <= 0) {
            // 理论上很少出现 <= 0，这里兜底，至少给 1 tick
            rainTime = 1;
        }

        // 保持当前的雨时长不变，只把雷暴时间改成和雨一样
        data.setRaining(true);            // 本来就是 true，再写一遍也没关系
        data.setThundering(true);         // 开启雷暴
        data.setThunderTime(rainTime);    // 雷暴持续时间 = 当前雨剩余时间
    }
}