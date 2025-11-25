package com.petitioner0.divinecore.particles;

import com.petitioner0.divinecore.DivineCore;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModParticles {
    // 粒子类型总注册器
    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
            DeferredRegister.create(Registries.PARTICLE_TYPE, DivineCore.MODID);

    // 你的风暴粒子类型
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> STORM_FUNNEL =
            PARTICLE_TYPES.register("storm_funnel", () -> new SimpleParticleType(false));
    }

