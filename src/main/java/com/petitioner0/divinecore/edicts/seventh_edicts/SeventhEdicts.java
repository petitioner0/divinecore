package com.petitioner0.divinecore.edicts.seventh_edicts;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.BlockEvent;
import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.items.ItemHelper;

@EventBusSubscriber(modid = DivineCore.MODID)
public class SeventhEdicts {

    // Trigger condition: player breaks any block
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled())
            return;
        if (!(event.getLevel() instanceof ServerLevel server))
            return;

        BlockPos pos = event.getPos();

        if (pos.getY() >= 0)
            return;

        RandomSource random = server.getRandom();
        if (random.nextFloat() < 0.005f) {
            ItemStack bonus = new ItemStack(ItemHelper.getItemByName("fragment_of_nihil").get());

            Containers.dropItemStack(
                    server,
                    pos.getX() + 0.5,
                    pos.getY() + 0.5,
                    pos.getZ() + 0.5,
                    bonus);
        }
    }
}