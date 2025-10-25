package com.petitioner0.divinecore.edicts.seventeenth_edicts;

import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.items.ItemHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = DivineCore.MODID)
public class SeventeenthEdicts {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        // Only care about Elder Guardian deaths
        if (!(event.getEntity() instanceof ElderGuardian)) {
            return;
        }

        Entity attacker = event.getSource().getEntity();
        if (!(attacker instanceof ServerPlayer player)) {
            return; 
        }

        // Check if player has Conduit Power effect
        if (!player.hasEffect(MobEffects.CONDUIT_POWER)) {
            return;
        }

        // Drop item at Elder Guardian death location
        dropItemAtEntity(event.getEntity());

    }

    /**
     * Drop item at entity location
     */
    private static void dropItemAtEntity(Entity entity) {
        String itemName = "heart_of_the_abyssal_maw";
        ItemHelper.dropItemAt(entity.level(), 
            new net.minecraft.world.phys.Vec3(entity.getX(), entity.getY(), entity.getZ()), 
            itemName, 1);
    }
}