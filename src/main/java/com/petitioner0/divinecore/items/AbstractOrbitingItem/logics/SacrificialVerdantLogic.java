package com.petitioner0.divinecore.items.AbstractOrbitingItem.logics;


import java.util.List;
import java.util.Random;

import com.petitioner0.divinecore.damage_type.ModDamageSources;
import com.petitioner0.divinecore.damage_type.ModDamageTypes;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

public class SacrificialVerdantLogic implements IOrbitingLogic {

    private int tickCounter = 0;
    private static final Random RANDOM = new Random();

    @Override
    public void tick(Player player) {
        // 仅服务端
        if (player.level().isClientSide()) return;

        tickCounter++;

        // 每10tick触发一次
        if (tickCounter < 10) return;
        tickCounter = 0;

        // 搜索范围（你可按需求改）
        double range = 3.0;

        List<LivingEntity> nearby = player.level().getEntitiesOfClass(
                LivingEntity.class,
                new AABB(
                        player.getX() - range, player.getY() - range, player.getZ() - range,
                        player.getX() + range, player.getY() + range, player.getZ() + range
                ),
                e -> e != player && e.isAlive()
        );

        if (nearby.isEmpty()) {
            return;
        }

        // 随机选一个目标
        LivingEntity target = nearby.get(RANDOM.nextInt(nearby.size()));

        // 构造伤害（算作玩家造成）
        DamageSource source = ModDamageSources.of(
                player.level(),
                ModDamageTypes.SACRIFICIAL_VERDANT,
                player,   // direct
                player    // attacker
        );

        // 造成 2 点伤害
        target.hurt(source, 2.0f);

        // 玩家回血 2 点
        player.heal(2.0f);
    }
}