package com.petitioner0.divinecore.items.AllItems;

import com.petitioner0.divinecore.items.AbstractFinalItem.AbstractFinalItem;
import com.petitioner0.divinecore.items.AbstractOrbitingItem.OrbitPreset;
import com.petitioner0.divinecore.items.ItemHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class GenesisOfLifeAndDeath extends AbstractFinalItem {
    public GenesisOfLifeAndDeath(Properties props) {
        super(props, OrbitPreset.STABLE);
    }

    @Override
    protected void onFloatingFinished(Level level, ItemEntity entity, ItemStack stack) {
        if (level.isClientSide) return;

        Vec3 pos = entity.position();
        ItemHelper.dropItemAt(level, pos, "malkuth", 1);

    }
}
