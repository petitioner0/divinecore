package com.petitioner0.divinecore.blocks;

import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.edicts.twenty_first_edicts.AntientropyCatalystBlockEntity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModBlockEntities {
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, DivineCore.MODID);

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<AntientropyCatalystBlockEntity>>
            ANTIENTROPY_CATALYST_BE = BLOCK_ENTITIES.register(
                    "antientropy_catalyst",
                    () -> BlockEntityType.Builder.of(
                            AntientropyCatalystBlockEntity::new,
                            ModBlocks.ANTIENTROPY_CATALYST.get()
                    ).build(null)
            );

    public static void register(IEventBus bus) {
        BLOCK_ENTITIES.register(bus);
    }
}