package com.petitioner0.divinecore.edicts.nineteenth_edicts;


import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.effects.ModEffects;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.world.phys.Vec2;

/**
 * Player Cold System (Independent Movement Decay Version):
 * - Increases cold value by coefficient per tick in specified cold biomes
 * - Independently reduces cold value based on movement distance this tick (even if no growth occurred)
 * - 6 thresholds corresponding to Slowness I~VI
 * - Uses player PersistentData for storage
 */
@EventBusSubscriber(modid = DivineCore.MODID)
public class ColdTracker {

    /** Base gain per tick (before coefficient multiplication) */
    private static final double BASE_GAIN_PER_TICK = 1.0D;

    private static final double MOVEMENT_DECAY_PER_BLOCK = 2.0D;

    private static final double MAX_DECAY_PER_TICK = 20.0D;

    /** Six thresholds (reaching each corresponds to Slowness I~VI) */
    private static final double[] THRESHOLDS = new double[]{50, 100, 150, 200, 250, 300};

    /** Effect duration (ticks) when at threshold, continuously refreshed */
    private static final int EFFECT_DURATION_TICKS = 40; // 2秒

    /** Frost corrosion effect duration (ticks), 5 minutes */
    private static final int FROST_CORROSION_DURATION_TICKS = 6000; // 5分钟

    private static final double MAX_COLD_CAP = 400.0D;

    /** Freeze ticks added per tick when at maximum cold stage */
    private static final int FREEZE_ADD_PER_TICK_WHEN_MAX = 4;

    private static final double PASSIVE_WARMING_PER_TICK = 0.25D;

    private static final String NBT_COLD_VALUE = DivineCore.MODID + ".cold_value";
    
    /** Player position cache: stores each player's last position for movement distance calculation */
    public static final Map<Player, Vec2> PLAYER_LAST_POSITIONS = new WeakHashMap<>();

    // Biome coefficient table
    private static final Map<ResourceKey<Biome>, Double> COLD_COEFFICIENTS = new HashMap<>();
    static {
        // Ocean related
        COLD_COEFFICIENTS.put(Biomes.COLD_OCEAN,          1.2);
        COLD_COEFFICIENTS.put(Biomes.DEEP_COLD_OCEAN,     1.4);
        COLD_COEFFICIENTS.put(Biomes.FROZEN_OCEAN,        1.7);
        COLD_COEFFICIENTS.put(Biomes.DEEP_FROZEN_OCEAN,   1.9);

        // Mountains
        COLD_COEFFICIENTS.put(Biomes.JAGGED_PEAKS,        1.6); 
        COLD_COEFFICIENTS.put(Biomes.FROZEN_PEAKS,        2.0); 
        COLD_COEFFICIENTS.put(Biomes.SNOWY_SLOPES,        1.4); 

        // Forest/Taiga/Woodland
        COLD_COEFFICIENTS.put(Biomes.SNOWY_TAIGA,         0.8); 
        COLD_COEFFICIENTS.put(Biomes.GROVE,               1.0); 

        // River/Plains
        COLD_COEFFICIENTS.put(Biomes.FROZEN_RIVER,        1.2); 
        COLD_COEFFICIENTS.put(Biomes.SNOWY_BEACH,         0.6); 
        COLD_COEFFICIENTS.put(Biomes.SNOWY_PLAINS,        1.0); 
        COLD_COEFFICIENTS.put(Biomes.ICE_SPIKES,          1.6); 
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();

        if (level.isClientSide()) return;

        boolean hasFrost = player.hasEffect(ModEffects.FrostCorrosion);
        boolean hasInfernal = player.hasEffect(ModEffects.InfernalScorch);
        boolean hasBothEffects = hasFrost && hasInfernal;

        // Current position & biome
        BlockPos pos = player.blockPosition();
        ResourceKey<Biome> biomeKey = level.getBiome(pos).unwrapKey().orElse(null);

        // Calculate horizontal movement distance for this tick
        double moved = computeAndStoreMovement(player);

        // 1) Calculate cold gain for this tick (only in specified biomes)
        double gainThisTick = 0.0D;
        if (biomeKey != null) {
            Double coeff = COLD_COEFFICIENTS.get(biomeKey);
            if (coeff != null) {
                gainThisTick = BASE_GAIN_PER_TICK * coeff;
                
                // When in water, cold value increases at 1.5x speed
                if (player.isInWater()) {
                    gainThisTick *= 1.5D;
                }
            }
        }

        // Read current cold value
        double cold = getCold(player);

        // Add growth
        cold += gainThisTick;

        // 2) Calculate independent decay caused by movement (even if there is no growth)
        // If player has frost effect, cannot reduce cold value by moving
        if (!hasFrost && moved > 0.0D) {
            double decay = Math.min(MAX_DECAY_PER_TICK, MOVEMENT_DECAY_PER_BLOCK * moved);
            cold -= decay;
        }

        // Passive warming logic (in non-cold biomes and without frost effect)
        boolean inColdBiome = (biomeKey != null && COLD_COEFFICIENTS.containsKey(biomeKey));
        if (!inColdBiome && !player.hasEffect(ModEffects.FrostCorrosion)) {
            cold -= PASSIVE_WARMING_PER_TICK;
        }

        // Boundary: not lower than 0, not higher than MAX_COLD_CAP
        if (cold < 0.0D) cold = 0.0D;
        if (cold > MAX_COLD_CAP) cold = MAX_COLD_CAP;
        setCold(player, cold);

        // If player has both frost and infernal effects, do not apply any cold-related negative states
        if (hasBothEffects) {
            return;
        }

        // Apply slowness effect based on thresholds
        int stage = computeStage(cold); // 0~6
        applySlowness(player, stage);
        
        // Check if maximum threshold is reached, if so apply frost effect
        if (stage >= 6 && !player.hasEffect(ModEffects.FrostCorrosion)) {
            applyFrostCorrosion(player);
        }

        applyFreezingAtMaxStage(player, stage);
    }

    private static double getCold(Player player) {
        return player.getPersistentData().getDouble(NBT_COLD_VALUE);
    }

    private static void setCold(Player player, double value) {
        player.getPersistentData().putDouble(NBT_COLD_VALUE, value);
    }

    /**
     * Calculate player's horizontal movement distance for this tick (in blocks), and write back current position for next calculation
     */
    public static double computeAndStoreMovement(Player player) {
        Vec2 lastPos = PLAYER_LAST_POSITIONS.get(player);
        double x = player.getX();
        double z = player.getZ();

        double dist = 0.0D;
        if (lastPos != null) {
            double dx = x - lastPos.x;
            double dz = z - lastPos.y;
            dist = Math.sqrt(dx * dx + dz * dz);
        }

        // Write back current position to cache
        PLAYER_LAST_POSITIONS.put(player, new Vec2((float) x, (float) z));

        return dist;
    }

    /**
     * Calculate current cold value stage (0~6)
     * 0=no effect, >=1 corresponds to slowness I~VI
     */
    private static int computeStage(double cold) {
        int stage = 0;
        for (double th : THRESHOLDS) {
            if (cold >= th) stage++;
            else break;
        }
        return Math.min(stage, 6);
    }

    /**
     * Apply slowness effect based on stage (continuously refreshed). stage=0 does not add effect.
     */
    private static void applySlowness(Player player, int stage) {
        if (stage <= 0) return;

        if (!player.hasEffect(ModEffects.FrostCorrosion)) {

        int amplifier = stage - 1; 
        MobEffectInstance effect = new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                EFFECT_DURATION_TICKS,
                amplifier,
                false,
                false,
                false
        );
        player.addEffect(effect);
        }
        else {
            player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SLOWDOWN,
                EFFECT_DURATION_TICKS,
                5,
                false,
                false,
                false
            ));
        }
    }

    /**
     * Give player frost effect, duration 5 minutes
     */
    private static void applyFrostCorrosion(Player player) {
        MobEffectInstance frostCorrosion = new MobEffectInstance(
                ModEffects.FrostCorrosion,
                FROST_CORROSION_DURATION_TICKS,
                0,
                false,
                true,
                true
        );
        player.addEffect(frostCorrosion);
    }

    private static void applyFreezingAtMaxStage(Player player, int stage) {
        if (stage >= 6 || player.hasEffect(ModEffects.FrostCorrosion)) {
            int frozen = player.getTicksFrozen();
            int need = player.getTicksRequiredToFreeze();
            if (frozen < need) {
                player.setTicksFrozen(Math.min(need, frozen + FREEZE_ADD_PER_TICK_WHEN_MAX));
            }
        }
    }

    /**
     * Reset cold value to 0 when player dies
     */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof Player player) {
            // Only on server
            if (player.level().isClientSide()) return;
            
            // Reset cold value to 0
            setCold(player, 0.0D);
            
            // Clear position cache
            PLAYER_LAST_POSITIONS.remove(player);
        }
    }
}