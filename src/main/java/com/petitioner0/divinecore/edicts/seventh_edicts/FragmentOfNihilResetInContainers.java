package com.petitioner0.divinecore.edicts.seventh_edicts;

import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerContainerEvent;
import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.items.AllItems.FragmentOfNihil;

@EventBusSubscriber(modid = DivineCore.MODID) // Remember to replace with your modid
public class FragmentOfNihilResetInContainers {

    /** When a player closes any container, remove the expiration tag from "Fragment of Nihil" items in the container */
    @SubscribeEvent
    public static void onContainerClose(PlayerContainerEvent.Close event) {
        AbstractContainerMenu menu = event.getContainer();

        // Slots that are not the player's inventory are "container slots".
        for (Slot slot : menu.slots) {
            // Skip player inventory slots: these slots have container == event.getEntity().getInventory()
            if (slot.container == event.getEntity().getInventory())
                continue;

            ItemStack stack = slot.getItem();
            if (stack.isEmpty())
                continue;

            if (stack.getItem() instanceof FragmentOfNihil) {
                // Remove the custom expiration tag so it gets reassigned when taken out later
                stack.update(
                        DataComponents.CUSTOM_DATA,
                        CustomData.EMPTY,
                        cd -> cd.update(tag -> tag.remove(FragmentOfNihil.TAG_EXPIRE_AT)));

                // Notify menu of changes to avoid client display desync
                slot.setChanged();
            }
        }

        // Broadcast overall changes once
        event.getEntity().containerMenu.broadcastChanges();
    }
}