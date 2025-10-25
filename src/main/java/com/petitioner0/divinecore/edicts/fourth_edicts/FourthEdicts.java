package com.petitioner0.divinecore.edicts.fourth_edicts;

import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.damage_type.ModDamageTypes;
import com.petitioner0.divinecore.damage_type.ModDamageSources;
import com.petitioner0.divinecore.items.ItemHelper;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.effect.MobEffects;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * After running for more than 5 seconds: For every 1 point of saturation or 1 point of hunger consumed, lose 1 point of health (0.5 hearts)
 */
@EventBusSubscriber(modid = DivineCore.MODID)
public class FourthEdicts {

    private static final class RunState {
        int runningTicks;
        int lastFood;
        float lastSaturation;
        int damageCount; // Damage counter

        RunState(int food, float sat) {
            this.lastFood = food;
            this.lastSaturation = sat;
            this.damageCount = 0;
        }
    }

    private static final Map<UUID, RunState> STATE = new HashMap<>();
    private static final int THRESHOLD_TICKS = 100;

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide)
            return;
        if (!(player instanceof ServerPlayer))
            return;
        if (player.isCreative() || player.isSpectator())
            return;

        FoodData food = player.getFoodData();
        UUID id = player.getUUID();

        RunState st = STATE.computeIfAbsent(id, k -> new RunState(food.getFoodLevel(), food.getSaturationLevel()));

        boolean sprinting = player.isSprinting();

        if (sprinting) {
            st.runningTicks++;
        } else {
            st.runningTicks = 0;
            // Reset damage counter when stopping running
            st.damageCount = 0;
        }

        if (sprinting && st.runningTicks >= THRESHOLD_TICKS) {
            int curFood = food.getFoodLevel();
            float curSat = food.getSaturationLevel();

            int foodLoss = Math.max(0, st.lastFood - curFood);
            int satLoss = 0;
            if (st.lastSaturation > curSat) {
                satLoss = (int) Math.floor((st.lastSaturation - curSat) + 1e-4f);
            }

            int totalLoss = foodLoss + satLoss;
            if (totalLoss > 0) {
                // Check if player meets vulnerability conditions (no armor, no absorption, no resistance)
                if (!isPlayerVulnerable(player)) {
                    // Conditions not met, reset damage counter
                    st.damageCount = 0;
                } else {
                    // Conditions met, increase damage counter
                    st.damageCount += totalLoss;

                    // Check if damage counter reaches 20
                    if (st.damageCount >= 20) {
                        checkSpecialReward((ServerPlayer) player);
                    }
                }

                player.hurt(
                        ModDamageSources.of(player.level(), ModDamageTypes.RUN_EXHAUSTION),
                        (float) totalLoss);
            }

            st.lastFood = curFood;
            st.lastSaturation = curSat;
        } else {
            st.lastFood = food.getFoodLevel();
            st.lastSaturation = food.getSaturationLevel();
        }
    }

    private static boolean isPlayerVulnerable(Player player) {
        // Check if player has any armor value
        if (hasArmor(player)) {
            return false; 
        }

        // Check if player has absorption effect
        if (player.hasEffect(MobEffects.ABSORPTION)) {
            return false; 
        }

        // Check if player has resistance effect
        if (player.hasEffect(MobEffects.DAMAGE_RESISTANCE)) {
            return false; 
        }

        return true; 
    }

    private static void checkSpecialReward(ServerPlayer player) {
        ItemStack mainHand = player.getMainHandItem();
        ItemStack offHand = player.getOffhandItem();

        boolean hasGlassBottle = false;
        ItemStack glassBottleStack = null;

        if (mainHand.getItem() == Items.GLASS_BOTTLE) {
            hasGlassBottle = true;
            glassBottleStack = mainHand;
        } else if (offHand.getItem() == Items.GLASS_BOTTLE) {
            hasGlassBottle = true;
            glassBottleStack = offHand;
        }

        if (hasGlassBottle && glassBottleStack != null) {
            // Remove one glass bottle
            glassBottleStack.shrink(1);

            // Give mod item
            ItemHelper.giveItemToPlayer(player, "shard_of_the_soul", 1);

            DivineCore.LOGGER.info("玩家 {} 因跑步疲劳达到20点伤害并满足特殊条件获得奖励物品",
                    player.getName().getString());
        }
    }

    private static boolean hasArmor(Player player) {
        return player.getArmorValue() > 0;
    }

}