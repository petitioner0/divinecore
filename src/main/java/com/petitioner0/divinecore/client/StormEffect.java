package com.petitioner0.divinecore.client;

import com.petitioner0.divinecore.particles.ModParticles;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class StormEffect {

    // 所有正在进行的风暴任务
    private static final List<StormTask> TASKS = new ArrayList<>();

    /** 对外调用：开始一个渐进式风暴粒子效果 */
    public static void spawnStorm(ClientLevel level, Vec3 center) {
        // level 不用存，后面每帧从 Minecraft 里拿当前 ClientLevel
        TASKS.add(new StormTask(center));
    }

    /** 每帧调用，在 client tick 里触发 */
    public static void clientTick() {
        Minecraft mc = Minecraft.getInstance();
        ClientLevel level = mc.level;
        if (level == null) return;

        // 遍历任务并移除已经完成的
        TASKS.removeIf(task -> task.tick(level));
    }

    /** 单个风暴任务，负责逐帧生成粒子 */
    private static class StormTask {
        private static final int TOTAL = 15000;     // 总粒子数
        private static final int MAX_PER_TICK = 200; // 每 tick 最大生成量
        private static final int GROW_TICKS = 50;   // 多少 tick 内从 0 增长到最大

        private final Vec3 center;
        private int spawned = 0;
        private int ticks = 0;

        public StormTask(Vec3 center) {
            this.center = center;
        }

        /** @return true 表示完成 */
        public boolean tick(ClientLevel level) {
            if (spawned >= TOTAL) return true;

            ticks++;

            // 线性增长：随 tick 增多而增加数量
            double t = Math.min(1.0, ticks / (double) GROW_TICKS);
            int perTick = (int)(t * MAX_PER_TICK); // 从 0 → MAX_PER_TICK

            // 限制不能超过剩余量
            int remain = TOTAL - spawned;
            perTick = Math.min(perTick, remain);

            // 生成粒子
            for (int i = 0; i < perTick; i++) {
                level.addParticle(
                        ModParticles.STORM_FUNNEL.get(),
                        center.x, center.y, center.z,
                        0.0, 0.0, 0.0
                );
            }

            spawned += perTick;
            return spawned >= TOTAL;
        }
    }
}
