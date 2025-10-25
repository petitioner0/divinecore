package com.petitioner0.divinecore.entities;

import com.petitioner0.divinecore.DivineCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModEntities {
    // 注册表对象
    public static final DeferredRegister<EntityType<?>> ENTITIES =
            DeferredRegister.create(Registries.ENTITY_TYPE, DivineCore.MODID);
}
