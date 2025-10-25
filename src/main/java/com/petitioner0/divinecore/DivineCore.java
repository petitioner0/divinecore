package com.petitioner0.divinecore;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;
import com.mojang.serialization.MapCodec;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.Difficulty;

import com.petitioner0.divinecore.blocks.ModBlockEntities;
import com.petitioner0.divinecore.blocks.ModBlocks;
import com.petitioner0.divinecore.edicts.first_edicts.FirstEdicts;
import com.petitioner0.divinecore.edicts.second_edicts.SecondEdicts;
import com.petitioner0.divinecore.edicts.sixth_edicts.SixthEdicts;
import com.petitioner0.divinecore.edicts.third_edicts.ThirdEdicts;
import com.petitioner0.divinecore.edicts.thirteenth_edicts.PunishCropLootModifier;
import com.petitioner0.divinecore.effects.ModEffects;
import com.petitioner0.divinecore.entities.ModEntities;
import com.petitioner0.divinecore.items.ModItems;
import com.petitioner0.divinecore.items.AbstractOrbitingItem.logics.OrbitingEffectHandler;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(DivineCore.MODID)
public class DivineCore {
    public static final String MODID = "divinecore";
    public static final Logger LOGGER = LogUtils.getLogger();

    // register loot modifiers codec
    private static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
        DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, MODID);

    public static final DeferredHolder<MapCodec<? extends IGlobalLootModifier>, MapCodec<? extends IGlobalLootModifier>> PUNISH_CROP =
        LOOT_MODIFIERS.register("punish_crop", () -> PunishCropLootModifier.CODEC);

    public DivineCore(IEventBus modEventBus, ModContainer modContainer) {
        NeoForge.EVENT_BUS.register(this);
        
        // initialize first edicts
        FirstEdicts.init(modEventBus, NeoForge.EVENT_BUS);
        
        // initialize second edicts
        SecondEdicts.init(modEventBus, NeoForge.EVENT_BUS);
        
        // initialize third edicts
        ThirdEdicts.init(modEventBus, NeoForge.EVENT_BUS);
        
        // initialize sixth edicts
        SixthEdicts.init(modEventBus, NeoForge.EVENT_BUS);
        
        // register items
        ModItems.register(modEventBus);
        // register blocks (including block items)
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModEntities.ENTITIES.register(modEventBus);
        ModEffects.EFFECTS.register(modEventBus);

        // register loot modifiers
        LOOT_MODIFIERS.register(modEventBus);
        OrbitingEffectHandler.registerOrbitingLogics();

    }
    

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("DivineCore 服务器启动完成");
        MinecraftServer server = event.getServer();
        server.setDifficulty(Difficulty.HARD, true);
    }
}
