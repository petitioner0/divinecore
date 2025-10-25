package com.petitioner0.divinecore.edicts.sixteenth_edicts;

import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.items.ItemHelper;
import com.petitioner0.divinecore.items.ModItems;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Everbloom effect handler
 * Detects if the player is holding an Everbloom item and is below y=-128, and gives them a regeneration 6 effect
 */
@EventBusSubscriber(modid = DivineCore.MODID)
public class EverbloomEffectHandler {
    
    private static final int TARGET_Y = -128;
    private static final int REGENERATION_AMPLIFIER = 7; 
    private static final int EFFECT_DURATION = 40; // 2 seconds duration (40 ticks)
    
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        
        // Check if the player is below the target depth
        BlockPos pos = player.blockPosition();
        if (pos.getY() > TARGET_Y) return;
        
        // Check if the player is holding an Everbloom item
        ItemStack mainHandItem = player.getMainHandItem();
        if (mainHandItem.isEmpty()) return;
        
        // Check if the item is an Everbloom item
        if (!isEverbloomItem(mainHandItem)) return;
        
        // Give the regeneration 6 effect
        MobEffectInstance regenerationEffect = new MobEffectInstance(
            MobEffects.REGENERATION, 
            EFFECT_DURATION, 
            REGENERATION_AMPLIFIER,
            false, 
            false,  
            true 
        );
        
        player.addEffect(regenerationEffect);
    }
    
    private static boolean isEverbloomItem(ItemStack itemStack) {
        var everbloomItem = ItemHelper.getItemByName("everbloom");
        if (everbloomItem == null) return false;
        
        return itemStack.is(everbloomItem.get());
    }
}
