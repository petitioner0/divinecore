package com.petitioner0.divinecore.edicts.eighteenth_edicts;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import com.petitioner0.divinecore.items.ItemHelper;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.petitioner0.divinecore.DivineCore;

@EventBusSubscriber(modid = DivineCore.MODID)
public class EighteenthEdicts {
    
    // Track each player's duration in gravity blocks
    private static final java.util.Map<Player, Integer> playerDurationMap = new java.util.HashMap<>();
    private static final int REQUIRED_DURATION = 200;

    /** Every tick refresh: if the player's eyes are in the gravity block, add digging fatigue */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        // If the player's eyes are in the gravity block, add digging fatigue effect
        if (isEyeInGravityBlock(player)) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.DIG_SLOWDOWN, 40, 1, true, false, false
            ));
        }

        // Check 30 seconds reward conditions: eyes in gravity block + turtle helmet + water breathing
        boolean allRewardConditionsMet = isEyeInGravityBlock(player) && 
                                       hasTurtleOrWaterBreathing(player);

        if (allRewardConditionsMet) {
            // Increase the duration
            int currentDuration = playerDurationMap.getOrDefault(player, 0) + 1;
            playerDurationMap.put(player, currentDuration);
            
            if (currentDuration >= REQUIRED_DURATION) {
                // Break the eye block and give reward
                breakEyeBlockAndGiveReward(player);
                // Reset the timer
                playerDurationMap.remove(player);
            }
        } else {
            // Conditions not met, reset the timer
            playerDurationMap.remove(player);
        }
    }

    //Cancel "in wall suffocation" damage (only when turtle helmet or water breathing, and the eyes are in the gravity block)
    @SubscribeEvent
    public static void onLivingDamagePre(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!event.getSource().is(net.minecraft.world.damagesource.DamageTypes.IN_WALL)) return;

        // Need turtle helmet or water breathing, and the eyes are in the gravity block
        if (hasTurtleOrWaterBreathing(player) && isEyeInGravityBlock(player)) {
            event.setCanceled(true); // Immune suffocation damage
        }
    }

    /** Determine if the block the player's eyes are in is a FallingBlock (such as sand, gravel, concrete powder, etc.) */
    private static boolean isEyeInGravityBlock(Player player) {
        Vec3 eye = player.getEyePosition(); 
        BlockPos pos = BlockPos.containing(eye.x, eye.y, eye.z);
        BlockState state = player.level().getBlockState(pos);
        return state.getBlock() instanceof FallingBlock;
    }

    /** Whether the player is wearing a turtle helmet or has water breathing effect */
    private static boolean hasTurtleOrWaterBreathing(Player player) {
        boolean turtle = player.getItemBySlot(EquipmentSlot.HEAD).is(Items.TURTLE_HELMET);
        boolean waterBreathing = player.hasEffect(MobEffects.WATER_BREATHING);
        return turtle || waterBreathing;
    }


    //Break the eye block and give reward
    private static void breakEyeBlockAndGiveReward(Player player) {
        if (!(player.level() instanceof ServerLevel serverLevel)) return;
        if (!(player instanceof net.minecraft.server.level.ServerPlayer serverPlayer)) return;
        
        // Get the eye position
        Vec3 eye = player.getEyePosition();
        BlockPos eyePos = BlockPos.containing(eye.x, eye.y, eye.z);
        
        // Break the eye block
        serverLevel.destroyBlock(eyePos, true, player);
        ItemHelper.giveItemToPlayer(serverPlayer, "ashlung_relic", 1);
    }
}