package com.petitioner0.divinecore;

import java.util.function.Supplier;

import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.items.ModItems;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModCreativeTabs {
    public static final DeferredRegister<CreativeModeTab> CREATIVE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, DivineCore.MODID);

    public static final Supplier<CreativeModeTab> DIVINECORE_TAB =
            CREATIVE_TABS.register("main", () -> CreativeModeTab.builder()
                    .title(Component.translatable("itemGroup." + DivineCore.MODID + ".main"))
                    .icon(() -> new ItemStack(ModItems.AETHERIAL_REVERIE.get()))
                    .displayItems((parameters, output) -> {
                        ModItems.ITEMS.getEntries().forEach(item -> output.accept(item.get()));
                    })
                    .build()
            );
}