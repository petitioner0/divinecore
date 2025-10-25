package com.petitioner0.divinecore.edicts.twelfth_edicts;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.effects.ModEffects;

@EventBusSubscriber(modid = DivineCore.MODID)
public class ChorusFruitEatHandler {

    @SubscribeEvent
    public static void onEatChorusFruit(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof Player player)) return;

        ItemStack stack = event.getItem();

        if (stack.is(Items.CHORUS_FRUIT)) {
            TwelfthEdicts.CheckAndGiveReward(player);
            player.addEffect(new MobEffectInstance(ModEffects.Spatiolysis, 800, 0, false, true, true));
        }
    }
}