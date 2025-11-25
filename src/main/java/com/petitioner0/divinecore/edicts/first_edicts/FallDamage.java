package com.petitioner0.divinecore.edicts.first_edicts;

import com.petitioner0.divinecore.DivineCore;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DivineCore.MODID)
public class FallDamage {

    @SubscribeEvent
    public static void onPlayerFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        ResourceKey<Level> dreamDimension =
                ResourceKey.create(Registries.DIMENSION,
                        ResourceLocation.parse(DivineCore.MODID + ":dream"));

        ServerLevel dreamLevel = player.getServer().getLevel(dreamDimension);

        if (dreamLevel != null && player.level().dimension().equals(dreamDimension)) {
            event.setCanceled(true); // 取消摔落伤害
        }
    }
}