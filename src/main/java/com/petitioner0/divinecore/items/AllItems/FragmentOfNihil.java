package com.petitioner0.divinecore.items.AllItems;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import com.petitioner0.divinecore.items.AbstractOrbitingItem.AbstractOrbitingItem;
import com.petitioner0.divinecore.items.AbstractOrbitingItem.OrbitPreset;

import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.component.CustomData;

public class FragmentOfNihil extends AbstractOrbitingItem {

    public static final String TAG_EXPIRE_AT = "dc_expire_at_tick";
    public static final long LIFETIME_TICKS = 600L; 

    public FragmentOfNihil(Properties props) {
        super(props, OrbitPreset.VOID);
    }

    /** Called every tick in the inventory (when the player/creature is holding it) */
    @Override
    public void inventoryTick(ItemStack stack, Level level, Entity holder, int slot, boolean selected) {
        if (level.isClientSide)
            return;
        ServerLevel serverLevel = (ServerLevel) level;

        ensureExpireTag(stack, serverLevel);
        if (isExpired(stack, serverLevel)) {
            if (holder instanceof Player p) {
                // Directly clear the slot
                p.getInventory().setItem(slot, ItemStack.EMPTY);
                p.containerMenu.broadcastChanges();
            } else {
                // Other creature's inventory: clear the stack count
                stack.shrink(stack.getCount());
            }
        }
    }

    @Override
    public boolean onEntityItemUpdate(ItemStack stack, ItemEntity entity) {
        Level level = entity.level();
        if (!level.isClientSide) {
            ServerLevel serverLevel = (ServerLevel) level;
            ensureExpireTag(stack, serverLevel);
            if (isExpired(stack, serverLevel)) {
                entity.discard(); // Directly destroy the item entity
                return true; 
            }
        }
        return false;
    }

    private static void ensureExpireTag(ItemStack stack, ServerLevel level) {
        long nowTick = level.getGameTime();
        long expire = nowTick + LIFETIME_TICKS;

        // If it hasn't been set, write it once
        stack.update(
                DataComponents.CUSTOM_DATA,
                CustomData.EMPTY,
                cd -> cd.update(tag -> {
                    if (!tag.contains(TAG_EXPIRE_AT)) {
                        tag.putLong(TAG_EXPIRE_AT, expire);
                    }
                }));
    }

    private static boolean isExpired(ItemStack stack, ServerLevel level) {
        CustomData cd = stack.get(DataComponents.CUSTOM_DATA);
        if (cd == null || cd.isEmpty())
            return false;

        long expireAt = cd.copyTag().getLong(TAG_EXPIRE_AT);
        long nowTick = level.getGameTime();
        return nowTick >= expireAt;
    }

}