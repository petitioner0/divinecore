package com.petitioner0.divinecore.blocks;

import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.edicts.twenty_first_edicts.AntientropyCatalystBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.DeferredBlock;
import java.util.function.Supplier;

public class ModBlocks {
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(DivineCore.MODID);

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(DivineCore.MODID);

    public static final DeferredBlock<Block> CINDER_OF_MIGHT_BLOCK = registerBlock(
            "cinder_of_might_block",
            () -> new Block(BlockBehaviour.Properties
                    .of()
                    .mapColor(MapColor.STONE)
                    .strength(3.0F, 100.0F)
                    .requiresCorrectToolForDrops()
                    .sound(SoundType.DEEPSLATE)
                    .lightLevel(state -> 15)) 
    );

    public static final DeferredBlock<Block> AURUM_OF_THE_FIRMAMENT_ORE = registerBlock(
            "aurum_of_the_firmament_ore",
            () -> new Block(BlockBehaviour.Properties
                    .of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .strength(2.5F, 80.0F)
                    .sound(SoundType.STONE)
                    .lightLevel(state -> 15))
    );

    public static final DeferredBlock<Block> CRYOSTATIC_BLOCK = registerBlock(
            "cryostatic_block",
            () -> new Block(BlockBehaviour.Properties
                    .of()
                    .mapColor(MapColor.COLOR_BLUE)
                    .strength(2.5F, 80.0F)
                    .sound(SoundType.GLASS)
                    .lightLevel(state -> 2))
    );
    public static final DeferredBlock<Block> PYROSTATIC_BLOCK = registerBlock(
            "pyrostatic_block",
            () -> new Block(BlockBehaviour.Properties
                    .of()
                    .mapColor(MapColor.COLOR_RED)
                    .strength(2.5F, 80.0F)
                    .sound(SoundType.NETHER_BRICKS)
                    .lightLevel(state -> 2))
    );
    public static final DeferredBlock<Block> ANTIENTROPY_CATALYST = registerBlock(
        "antientropy_catalyst",
        () -> new AntientropyCatalystBlock(BlockBehaviour.Properties
                .of()
                .mapColor(MapColor.COLOR_PURPLE)
                .strength(2.5F, 80.0F)
                .sound(SoundType.AMETHYST)
                .lightLevel(state -> 2)),
                new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                );

    // Utility method: register both Block and BlockItem
    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block) {
        DeferredBlock<T> registeredBlock = BLOCKS.register(name, block);
        ITEMS.register(name, () -> new BlockItem(registeredBlock.get(), new Item.Properties()));
        return registeredBlock;
    }

    // Utility method: register Block and BlockItem with custom item properties
    private static <T extends Block> DeferredBlock<T> registerBlock(String name, Supplier<T> block, Item.Properties itemProperties) {
        DeferredBlock<T> registeredBlock = BLOCKS.register(name, block);
        ITEMS.register(name, () -> new BlockItem(registeredBlock.get(), itemProperties));
        return registeredBlock;
    }

    public static void register(IEventBus modEventBus) {
        BLOCKS.register(modEventBus);
        ITEMS.register(modEventBus);
    }
}