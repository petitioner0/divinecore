package com.petitioner0.divinecore.edicts.nineteenth_edicts;

import com.petitioner0.divinecore.FTBHelper;
import com.simibubi.create.AllDamageTypes;
import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.effects.ModEffects;
import com.petitioner0.divinecore.items.ItemHelper;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;

@EventBusSubscriber(modid = DivineCore.MODID)
public class NineteenthEdicts {

    @SubscribeEvent
    public static void onIncomingDamage(LivingDamageEvent.Post event) {
        // Check if it is saw damage
        if (event.getSource().is(AllDamageTypes.SAW)) {
            if (event.getEntity() instanceof Player player) {
                // Check if player has frost effect
                if (player.hasEffect(ModEffects.FrostCorrosion)) {
                    player.level().playSound(
                            null, 
                            player.blockPosition(), 
                            SoundEvents.GLASS_BREAK, 
                            SoundSource.BLOCKS, 
                            1.0F, 
                            1.0F 
                    );
                    FTBHelper.completeTask((ServerPlayer) player, "13DA357CE26F1EA0");
                    // Add special logic for frost effect
                    ItemHelper.dropItemAt(player.level(), player.position(), "blood_of_the_frost_corrosion", 1, true);
                    // Remove frost effect after giving item
                    player.removeEffect(ModEffects.FrostCorrosion);
                }
                if (player.hasEffect(ModEffects.InfernalScorch)) {
                    player.level().playSound(
                            null, 
                            player.blockPosition(), 
                            SoundEvents.FIRE_AMBIENT, 
                            SoundSource.BLOCKS, 
                            1.0F, 
                            1.0F 
                    );
                    FTBHelper.completeTask((ServerPlayer) player, "53EBACC099330042");
                    // Add special logic for infernal scorch effect
                    ItemHelper.dropItemAt(player.level(), player.position(), "blood_of_the_infernal_scorch", 1, true);
                    // Remove infernal scorch effect after giving item
                    player.removeEffect(ModEffects.InfernalScorch);
                }
            }
        }
    }
}