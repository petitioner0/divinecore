package com.petitioner0.divinecore.items.AbstractOrbitingItem;

import net.minecraft.world.item.Item;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.petitioner0.divinecore.net.S2CRemoveOrbitingItem;
import com.petitioner0.divinecore.net.S2CSpawnOrbitingItem;
import net.neoforged.neoforge.network.PacketDistributor;
import javax.annotation.Nonnull;

public abstract class AbstractOrbitingItem extends Item {
    private final OrbitPreset preset;
    public AbstractOrbitingItem(Properties props, OrbitPreset preset) {
        super(props);
        this.preset = preset;
    }
    public OrbitPreset getOrbitPreset() {
        return preset;
    }


    public float getOrbitRadius(ItemStack stack) {
        return preset.getRadius();
    }

    public float getOrbitHeight(ItemStack stack) {
        return preset.getHeight();
    }

    public float getOrbitSpeed(ItemStack stack) {
        return preset.getSpeed();
    }

    @Override
    public InteractionResultHolder<ItemStack> use(@Nonnull Level level, @Nonnull Player player, @Nonnull InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (!level.isClientSide) {
            if (player.isShiftKeyDown()) {
                OrbitingItemManager.removeOrbitingItem(player.getUUID(), stack.getItem());
                // Remove the orbiting visual
                PacketDistributor.sendToAllPlayers(new S2CRemoveOrbitingItem(player.getUUID(), stack.getItem()));
            } else {
                // Check if the player already has this orbiting item, if not, summon it
                if (!OrbitingItemManager.hasOrbitingItem(player.getUUID(), stack.getItem())) {
                    OrbitingItemManager.addOrbitingItem(player.getUUID(), stack.getItem());
                    // Summon the orbiting visual
                    PacketDistributor.sendToAllPlayers(new S2CSpawnOrbitingItem(player.getUUID(), stack, getOrbitRadius(stack), getOrbitHeight(stack), getOrbitSpeed(stack)));
                }
            }
        }

        player.swing(hand);
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }
}
