package com.petitioner0.divinecore.items.AllItems;

import net.minecraft.world.item.ItemStack;

import com.petitioner0.divinecore.items.AbstractOrbitingItem.AbstractOrbitingItem;
import com.petitioner0.divinecore.items.AbstractOrbitingItem.OrbitPreset;

import net.minecraft.world.entity.item.ItemEntity;

public class AetherialReverie extends AbstractOrbitingItem {
    public AetherialReverie(Properties properties) {
        super(properties, OrbitPreset.ETHEREAL);
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        if (!entity.level().isClientSide()) {
            entity.setNoGravity(true);
        }

        return false;
    }

}