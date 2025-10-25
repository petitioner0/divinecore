package com.petitioner0.divinecore.edicts.second_edicts;

import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.items.ItemHelper;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingEntityUseItemEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.item.UseAnim;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class SecondEdicts {

    // Timer for tracking player hunger status
    private static final Map<UUID, Integer> playerHungerTimers = new HashMap<>();
    // Track whether player has been punished
    private static final Map<UUID, Boolean> playerPunished = new HashMap<>();

    // Punishment duration: 10 seconds = 200 ticks
    private static final int PUNISHMENT_DURATION = 200;

    public static void init(IEventBus modEventBus, IEventBus gameEventBus) {
        SecondEdicts instance = new SecondEdicts();

        gameEventBus.register(instance);
    }

    /**
     * Listen to server tick events, check player hunger every tick
     */
    @SubscribeEvent
    public void onServerTick(ServerTickEvent.Post event) {
        MinecraftServer server = event.getServer();
        if (server != null) {
            // Iterate through all online players
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                checkPlayerHunger(player);
            }
        }
    }

    /*Check player hunger status*/
    private void checkPlayerHunger(ServerPlayer player) {
        UUID playerUUID = player.getUUID();

        // Get player's current food level
        int foodLevel = player.getFoodData().getFoodLevel();

        // If food level is 0 (hungry state)
        if (foodLevel <= 0) {
            // If player hasn't been punished yet, apply punishment immediately
            if (!playerPunished.getOrDefault(playerUUID, false)) {
                applyHungerPunishment(player);
                // Mark player as punished
                playerPunished.put(playerUUID, true);
                // Set punishment start time
                playerHungerTimers.put(playerUUID, 0);
            } else {
                int timer = playerHungerTimers.getOrDefault(playerUUID, 0) + 1;
                playerHungerTimers.put(playerUUID, timer);
            }
        } else {
            // If food level is not 0, reset punishment status
            playerPunished.put(playerUUID, false);
        }

        // Check if punishment effect needs to be removed
        if (playerPunished.getOrDefault(playerUUID, false)) {
            int timer = playerHungerTimers.getOrDefault(playerUUID, 0);
            if (timer >= PUNISHMENT_DURATION) {
                // Punishment time ended, reset status
                playerHungerTimers.put(playerUUID, 0);
                playerPunished.put(playerUUID, false);
            }
        }
    }

    /**
     * Apply hunger punishment effects
     */
    private void applyHungerPunishment(ServerPlayer player) {
        // Weakness III - 10 seconds
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 2, false, false, true));

        // Wither I - 10 seconds
        player.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 0, false, false, true));

        // Slowness I - 10 seconds
        player.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, 200, 0, false, false, true));

        DivineCore.LOGGER.info("玩家 {} 因饥饿受到第二法令惩罚", player.getName().getString());
    }

    /**
     * Listen to player eating completion event
     */
    @SubscribeEvent
    public void onEatFinish(LivingEntityUseItemEvent.Finish event) {
        if (!(event.getEntity() instanceof ServerPlayer player))
            return;

        ItemStack stack = event.getItem();
        UseAnim anim = stack.getUseAnimation(); 
        if (anim == UseAnim.EAT) {
            if (stack.getItem().getFoodProperties(stack, player) != null) {
                checkFoodReward(player, stack);
            }
        }
    }

    /**
     * Check food reward conditions
     */
    private void checkFoodReward(ServerPlayer player, ItemStack itemStack) {
        UUID playerUUID = player.getUUID();

        // Check if player is in punishment state
        if (!playerPunished.getOrDefault(playerUUID, false)) {
            return;
        }

        // Check if player still has hunger effect
        if (!player.hasEffect(MobEffects.HUNGER)) {
            return;
        }

        // Get food nutrition and saturation
        FoodProperties food = itemStack.getItem().getFoodProperties(itemStack, player);
        if (food == null) {
            return;
        }

        int nutrition = food.nutrition();
        float saturationModifier = food.saturation();

        // Calculate total saturation (saturation = saturation modifier * nutrition)
        float totalSaturation = saturationModifier * nutrition * 2.0f;

        // Check if sum of nutrition and saturation is greater than 25
        if (nutrition + totalSaturation > 25) {
            // Give reward item
            giveRewardItem(player);
        }
    }

    /*Give reward item*/
    private void giveRewardItem(ServerPlayer player) {
        if (ItemHelper.giveItemToPlayer(player, "eldritch_fulfillment", 1)) {
            DivineCore.LOGGER.info("玩家 {} 因在饥饿状态下食用高价值食物获得奖励物品", player.getName().getString());
        }
    }
    @SubscribeEvent
    public void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        UUID playerUUID = player.getUUID();

        playerHungerTimers.remove(playerUUID);
        playerPunished.remove(playerUUID);
    }
}
