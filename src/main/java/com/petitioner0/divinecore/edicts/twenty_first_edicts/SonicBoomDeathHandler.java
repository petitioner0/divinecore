package com.petitioner0.divinecore.edicts.twenty_first_edicts;

import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.blocks.ModBlocks;
import com.petitioner0.divinecore.effects.ModEffects;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

@EventBusSubscriber(modid = DivineCore.MODID) 
public class SonicBoomDeathHandler {

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide) return;

        DamageSource source = event.getSource();

        // Check if it is "sonic" damage (Warden's sonic_boom)
        if (source.is(DamageTypes.SONIC_BOOM)) {
            if (player.hasEffect(ModEffects.InfernalScorch) && player.hasEffect(ModEffects.FrostCorrosion)) {
                SpherePlacer.placeSolidMixedSphere(player.level(), player.blockPosition(), 
                ModBlocks.CRYOSTATIC_BLOCK.get().defaultBlockState(), 
                ModBlocks.PYROSTATIC_BLOCK.get().defaultBlockState(), 
                ModBlocks.ANTIENTROPY_CATALYST.get().defaultBlockState());

        }
    }
}
}