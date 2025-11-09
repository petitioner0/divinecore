package com.petitioner0.divinecore.edicts.eleventh_edicts;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.phys.AABB;

public final class WitherSpawner {
    private WitherSpawner() {}

    private static void boostMaxHealth(LivingEntity e, float max) {
        var inst = e.getAttribute(Attributes.MAX_HEALTH);
        if (inst != null) {
            inst.setBaseValue(max);
            e.setHealth(max);
        }
    }

    /** 在末地原点附近生成两只凋灵，并绑定末影龙共享500血 */
    public static void spawnTwoWithersNearOriginTop(ServerLevel end) {
        RandomSource rnd = end.getRandom();
        EndBossSharedHealthData data = EndBossSharedHealthData.get(end);

        for (int i = 0; i < 2; i++) {
            int dx = rnd.nextIntBetweenInclusive(-3, 3);
            int dz = rnd.nextIntBetweenInclusive(-3, 3);
            int dy = rnd.nextIntBetweenInclusive(0, 5);
            BlockPos p = new BlockPos(dx, 64 + dy, dz);

            WitherBoss w = EntityType.WITHER.create(end);
            if (w != null) {
                w.moveTo(p.getX() + 0.5, p.getY() + 0.1, p.getZ() + 0.5, rnd.nextFloat() * 360f, 0f);
                end.addFreshEntity(w);
                boostMaxHealth(w, 500f);
                data.add(w.getUUID());
            }
        }

        for (EnderDragon dragon : end.getEntitiesOfClass(
                EnderDragon.class,
                new AABB(-256, 0, -256, 256, end.getMaxBuildHeight(), 256))) {
            boostMaxHealth(dragon, 500f);
            data.add(dragon.getUUID());
        }

        data.set(500f);
        
        // 同步所有成员的血量，确保它们都使用共享血量池的值
        float sharedHealth = data.get();
        for (var id : data.ids()) {
            var e = end.getEntity(id);
            if (e instanceof LivingEntity le) {
                var inst = le.getAttribute(Attributes.MAX_HEALTH);
                if (inst != null && (float)inst.getBaseValue() < 500f)
                    inst.setBaseValue(500f);
                float hp = Math.max(0f, Math.min(500f, sharedHealth));
                le.setHealth(hp);
            }
        }
    }
}