package com.petitioner0.divinecore.edicts.eleventh_edicts;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import com.petitioner0.divinecore.DivineCore;

import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = DivineCore.MODID)
public class EndBossSharedHealthHandler {
    private static boolean lock = false;
    private static final Map<LivingEntity, Float> healthBeforeDamage = new WeakHashMap<>();

    @SubscribeEvent
    public static void onPreDamage(LivingDamageEvent.Pre event) {
        if (lock) return;

        LivingEntity victim = event.getEntity();
        Level lvl = victim.level();
        if (!(lvl instanceof ServerLevel sl)) return;
        if (sl.dimension() != Level.END) return;

        EndBossSharedHealthData data = EndBossSharedHealthData.get(sl);
        if (!data.contains(victim.getUUID())) return;

        // 记录伤害前的血量，用于计算实际造成的伤害
        healthBeforeDamage.put(victim, victim.getHealth());
    }

    @SubscribeEvent
    public static void onPostDamage(LivingDamageEvent.Post event) {
        if (lock) return;

        LivingEntity victim = event.getEntity();
        Level lvl = victim.level();
        if (!(lvl instanceof ServerLevel sl)) return;
        if (sl.dimension() != Level.END) return;

        EndBossSharedHealthData data = EndBossSharedHealthData.get(sl);
        if (!data.contains(victim.getUUID())) return;

        Float healthBefore = healthBeforeDamage.remove(victim);
        if (healthBefore == null) return;
        
        // 计算实际造成的伤害（考虑护甲、附魔等修改）
        float actualDamage = healthBefore - victim.getHealth();
        if (actualDamage <= 0f) return;

        lock = true;

        // 回补实际伤害，由共享血量统一扣除
        victim.heal(actualDamage);

        float newPool = data.get() - actualDamage;
        data.set(newPool);

        // 同步所有成员的血量
        for (UUID id : data.ids()) {
            Entity e = sl.getEntity(id);
            if (e instanceof LivingEntity le && !le.isDeadOrDying()) {
                var inst = le.getAttribute(Attributes.MAX_HEALTH);
                if (inst != null && (float)inst.getBaseValue() < 500f)
                    inst.setBaseValue(500f);

                float hp = Math.max(0f, Math.min(500f, newPool));
                le.setHealth(hp);
                if (hp <= 0f) le.kill();
            }
        }

        lock = false;
    }

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (lock) return;

        LivingEntity entity = event.getEntity();
        Level lvl = entity.level();
        if (!(lvl instanceof ServerLevel sl)) return;
        if (sl.dimension() != Level.END) return;

        EndBossSharedHealthData data = EndBossSharedHealthData.get(sl);
        if (data.contains(entity.getUUID())) {
            // 从共享血量系统中移除已死亡的实体
            data.remove(entity.getUUID());
        }
    }
}
