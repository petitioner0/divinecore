package com.petitioner0.divinecore.loot;

import com.mojang.serialization.MapCodec;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.edicts.thirteenth_edicts.PunishCropLootModifier;

import java.util.function.Supplier;

public class ModLootModifiers {
    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> GLM =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, DivineCore.MODID);

    public static final Supplier<MapCodec<PunishCropLootModifier>> PUNISH_CROP =
            GLM.register("punish_crop", () -> PunishCropLootModifier.CODEC);

    public static void register(IEventBus bus) {
        GLM.register(bus);
    }
}