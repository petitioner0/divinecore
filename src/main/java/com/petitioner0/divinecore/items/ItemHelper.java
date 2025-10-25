package com.petitioner0.divinecore.items;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.registries.DeferredItem;

import java.util.HashMap;
import java.util.Map;

public class ItemHelper {

    private static final Map<String, DeferredItem<Item>> ITEM_MAP = new HashMap<>();

    public static void register(String name, DeferredItem<Item> item) {
        ITEM_MAP.put(name, item);
    }

    public static boolean giveItemToPlayer(ServerPlayer player, String name, int count) {
        DeferredItem<Item> deferred = getItemByName(name);
        if (deferred == null) {
            return false;
        }

        ItemStack stack = new ItemStack(deferred.get(), count);

        boolean added = player.addItem(stack);
        if (!added) {
            // If the inventory is full, drop it to the player's feet
            player.drop(stack, false);
        }
        return true;
    }


    public static boolean dropItemAt(Level level, Vec3 pos, String name, int count, boolean withVelocity) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return false; 
        }

        DeferredItem<Item> deferred = getItemByName(name);
        if (deferred == null) {
            return false; 
        }

        ItemStack stack = new ItemStack(deferred.get(), count);

        ItemEntity itemEntity = new ItemEntity(serverLevel, pos.x, pos.y, pos.z, stack);
        
        if (withVelocity) {
            // 45 degree angle, random rotation angle initial speed
            double angle = serverLevel.random.nextDouble() * 2 * Math.PI; 
            double speed = 0.2 + serverLevel.random.nextDouble() * 0.3; 
            double horizontalSpeed = speed * Math.cos(Math.PI / 4); 
            double verticalSpeed = speed * Math.sin(Math.PI / 4); 
            
            Vec3 velocity = new Vec3(
                horizontalSpeed * Math.cos(angle),
                verticalSpeed,
                horizontalSpeed * Math.sin(angle)
            );
            itemEntity.setDeltaMovement(velocity);
        } else {
            itemEntity.setDeltaMovement(Vec3.ZERO); 
        }
        
        itemEntity.setDefaultPickUpDelay(); 

        serverLevel.addFreshEntity(itemEntity);
        return true;
    }

    public static boolean dropItemAt(Level level, Vec3 pos, String name, int count) {
        return dropItemAt(level, pos, name, count, false);
    }

    public static DeferredItem<Item> getItemByName(String name) {
        return ITEM_MAP.get(name);
    }

}
