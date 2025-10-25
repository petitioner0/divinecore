package com.petitioner0.divinecore.edicts.third_edicts;

import com.petitioner0.divinecore.items.ItemHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;

public class ThirdEdicts {

    public static void init(IEventBus modEventBus, IEventBus gameEventBus) {
        // 创建实例并注册游戏事件监听器
        ThirdEdicts instance = new ThirdEdicts();
        gameEventBus.register(instance);
    }

    @SubscribeEvent
    public void onEatFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        ItemStack stack = event.getItem();
        UseAnim anim = stack.getUseAnimation();

        // Check if it is an eating action
        if (anim == UseAnim.EAT) {
            // Check if it is a golden apple or enchanted golden apple
            if (stack.is(Items.GOLDEN_APPLE)) {
                checkGoldenAppleReward(player);
            }
        }
    }

    private void checkGoldenAppleReward(ServerPlayer player) {
        // Check if the player has a weakness effect
        if (!player.hasEffect(MobEffects.WEAKNESS)) {
            return;
        }

        // Check if the player's maximum health is only half a heart
        AttributeInstance maxHealthAttr = player.getAttribute(Attributes.MAX_HEALTH);
        if (maxHealthAttr == null) {
            return;
        }

        double maxHealth = maxHealthAttr.getValue();

        // Check if the player's maximum health is equal to 1.0 (half a heart)
        if (Math.abs(maxHealth - 1.0) < 0.001) { // Use a small error value to compare floating point numbers
            // Conditions met, give reward item
            giveGoldenAppleReward(player);
        }
    }

    private void giveGoldenAppleReward(ServerPlayer player) {
        if (ItemHelper.giveItemToPlayer(player, "shard_of_the_soul", 1)) {
        }
    }
}
