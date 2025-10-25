package com.petitioner0.divinecore.damage_type;

import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

public final class ModDamageSources {
    private ModDamageSources() {}

    // No context (pure source)
    public static DamageSource of(Level level, ResourceKey<DamageType> key) {
        Holder<DamageType> holder = level.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                .getHolderOrThrow(key);
        return new DamageSource(holder);
    }

    // With direct damage entity
    public static DamageSource of(Level level, ResourceKey<DamageType> key, Entity direct) {
        Holder<DamageType> holder = level.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                .getHolderOrThrow(key);
        return new DamageSource(holder, direct);
    }

    // With direct damage entity + attacker/causing entity
    public static DamageSource of(Level level, ResourceKey<DamageType> key, Entity direct, Entity attacker) {
        Holder<DamageType> holder = level.registryAccess()
                .registryOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE)
                .getHolderOrThrow(key);
        return new DamageSource(holder, direct, attacker);
    }
}