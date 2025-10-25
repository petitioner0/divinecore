package com.petitioner0.divinecore.effects;

import com.petitioner0.divinecore.DivineCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.effect.MobEffect;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEffects {
    public static final DeferredRegister<MobEffect> EFFECTS =
            DeferredRegister.create(Registries.MOB_EFFECT, DivineCore.MODID);

    public static final DeferredHolder<MobEffect, MobEffect> Spacilabile =
            EFFECTS.register("spacilabile", () -> new Spacilabile());

    public static final DeferredHolder<MobEffect, MobEffect> Oscillaspace =
            EFFECTS.register("oscillaspace", () -> new Oscillaspace());

    public static final DeferredHolder<MobEffect, MobEffect> Spatiolysis =
            EFFECTS.register("spatiolysis", () -> new Spatiolysis());

    public static final DeferredHolder<MobEffect, MobEffect> FrostCorrosion =
            EFFECTS.register("frost_corrosion", () -> new FrostCorrosion());

    public static final DeferredHolder<MobEffect, MobEffect> InfernalScorch =
            EFFECTS.register("infernal_scorch", () -> new InfernalScorch());

}