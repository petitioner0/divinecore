package com.petitioner0.divinecore.edicts.twentieth_edicts;

import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.edicts.nineteenth_edicts.ColdTracker;
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
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;

import java.util.HashMap;
import java.util.Map;

/**
 * 玩家炎热系统（移动叠加独立结算版）：
 * - 在地狱维度：在指定地狱生物群系中每tick按系数增加炎热值
 * - 在地狱维度：再根据本tick移动距离，独立增加炎热值（移动越快叠加越快）
 * - 在非地狱维度：每4tick降低1炎热值，逐渐冷却（除非有炎狱效果）
 * - 6个阈值：每个阈值给予【速度】等级，每两个阈值给予【急迫】等级
 * - 达到第三级（含）后点燃玩家
 * - 达到满级（6个阈值）时给予5分钟炎狱效果
 * - 具有炎狱效果时炎热值无法减少
 * - 使用玩家 PersistentData 存储
 */
@EventBusSubscriber(modid = DivineCore.MODID)
public class HeatTracker {

    // --- 可调参数 --- //
    /** 每tick基础叠加量（未乘系数前） */
    private static final double BASE_GAIN_PER_TICK = 1.0D;

    /** 每移动1格（水平距离）带来的炎热值额外叠加（独立于当tick基础增长） */
    private static final double MOVEMENT_GAIN_PER_BLOCK = 2.0D;

    /** 每tick最大增益值限制（上限可调） */
    private static final double MAX_GAIN_PER_TICK = 20.0D;

    /** 六个阈值（每个阈值给速度等级，每两个阈值给急迫等级） */
    private static final double[] THRESHOLDS = new double[]{50, 100, 150, 200, 250, 300};

    /** 给予速度&急迫的时长（tick），持续刷新 */
    private static final int EFFECT_DURATION_TICKS = 40; // 2秒

    /** 达到第三级后点燃玩家的时长（tick），持续刷新 */
    private static final int FIRE_DURATION_TICKS = 60; // 3秒

    /** 上限防护，防止无限膨胀 */
    private static final double MAX_HEAT_CAP = 400.0D;

    /** 火焰伤害倍数基础值（1级1.2，2级1.4，以此类推） */
    private static final double FIRE_DAMAGE_BASE_MULTIPLIER = 1.2D;

    /** 炎狱效果持续时间（5分钟 = 6000 ticks） */
    private static final int INFERNAL_SCORCH_DURATION_TICKS = 6000;

    // --- NBT keys --- //
    private static final String NBT_HEAT_VALUE = DivineCore.MODID + ".heat_value";

    // --- 运行时缓存 --- //
    // 使用 ColdTracker 的公共位置缓存

    // 生物群系系数表（仅地狱五大群系生效）
    private static final Map<ResourceKey<Biome>, Double> HEAT_COEFFICIENTS = new HashMap<>();
    static {
        HEAT_COEFFICIENTS.put(Biomes.NETHER_WASTES,    1.5); // 下界荒地
        HEAT_COEFFICIENTS.put(Biomes.SOUL_SAND_VALLEY, 1.2); // 灵魂沙峡谷
        HEAT_COEFFICIENTS.put(Biomes.CRIMSON_FOREST,   1.1); // 绯红森林
        HEAT_COEFFICIENTS.put(Biomes.WARPED_FOREST,    0.9); // 诡异森林（略低）
        HEAT_COEFFICIENTS.put(Biomes.BASALT_DELTAS,    1.4); // 玄武岩三角洲
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();

        // 只在服务端进行逻辑
        if (level.isClientSide()) return;
        
        // 读取当前炎热值
        double heat = getHeat(player);
        
        // 判断是否在地狱维度
        boolean isInNether = level.dimensionType().ultraWarm();
        
        if (isInNether) {
            // 在地狱：正常炎热追踪逻辑
            // 当前位置 & 生物群系
            BlockPos pos = player.blockPosition();
            ResourceKey<Biome> biomeKey = level.getBiome(pos).unwrapKey().orElse(null);

            // 使用 ColdTracker 已计算的移动距离
            double moved = ColdTracker.computeAndStoreMovement(player);

            // 1) 计算当tick炎热增长（仅在指定地狱群系）
            double gainThisTick = 0.0D;
            if (biomeKey != null) {
                Double coeff = HEAT_COEFFICIENTS.get(biomeKey);
                if (coeff != null) {
                    gainThisTick = BASE_GAIN_PER_TICK * coeff;
                }
            }

            // 2) 检查环境倍数（火中*2，岩浆中*3）
            double environmentMultiplier = 1.0D;
        
            if (player.isInLava()) {
                environmentMultiplier = 2.0D; // 岩浆中增长速度*2
            }
            
            // 应用环境倍数
            gainThisTick *= environmentMultiplier;

            // 3) 移动带来的额外叠加（移动越快叠加越多）
            double movementGain = 0.0D;
            if (moved > 0.0D && gainThisTick > 0.0D && biomeKey != null) {
                // 只有在有效地狱群系内才叠加移动热量，避免在其他群系靠刷位移涨热
                Double coeff = HEAT_COEFFICIENTS.get(biomeKey);
                if (coeff != null) {
                    double rawMovementGain = MOVEMENT_GAIN_PER_BLOCK * moved * environmentMultiplier * coeff;
                    movementGain = Math.min(MAX_GAIN_PER_TICK, rawMovementGain);
                }
            }

            // 累加并限幅
            heat = Math.max(0.0D, Math.min(MAX_HEAT_CAP, heat + gainThisTick + movementGain));
        } else {
            // 不在地狱：每4tick降低1炎热值
            if (heat > 0.0D && player.tickCount % 4 == 0) {
                heat -= 1.0D;
                if (heat < 0.0D) heat = 0.0D;
            }
        }

        // 写回
        setHeat(player, heat);

        boolean hasFrost = player.hasEffect(ModEffects.FrostCorrosion);
        boolean hasInfernal = player.hasEffect(ModEffects.InfernalScorch);
        boolean hasBothEffects = hasFrost && hasInfernal;

        // 如果同时具有霜蚀和炎狱效果，则不给予任何炎热相关的负面状态
        if (hasBothEffects) {
            return;
        }

        // 根据阈值施加 速度 & 急迫
        int stage = computeStage(heat); // 0~6
        
        // 如果玩家具有炎狱状态，则应用阈值6的效果（但不改变stage值）
        if (player.hasEffect(ModEffects.InfernalScorch)) {
            applySpeedAndHaste(player, 6); // 直接应用阈值6效果
        } else {
            applySpeedAndHaste(player, stage); // 正常应用当前阈值效果
        }
        
        // 检查是否达到最高阈值，如果是则给予炎狱效果
        if (stage >= 6 && !player.hasEffect(ModEffects.InfernalScorch)) {
            applyInfernalScorch(player);
        }

        // 第三级及以上：点燃玩家（每tick刷新）
        if (stage >= 3 || player.hasEffect(ModEffects.InfernalScorch)) {
            // 1.21+ 推荐用 setRemainingFireTicks 刷新燃烧时间
            player.setRemainingFireTicks(Math.max(player.getRemainingFireTicks(), FIRE_DURATION_TICKS));
        }
    }

    /** 处理火焰伤害倍数 */
    @SubscribeEvent
    public static void onLivingHurt(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        // 如果玩家有防火效果则直接返回
        if (player.hasEffect(MobEffects.FIRE_RESISTANCE)) return;

        DamageSource source = event.getSource();

        // 判断是否是火焰相关的伤害
        if (source.is(DamageTypes.IN_FIRE) || 
            source.is(DamageTypes.ON_FIRE) || 
            source.is(DamageTypes.LAVA) || 
            source.is(DamageTypes.HOT_FLOOR)) {

            double multiplier = calculateFireDamageMultiplier(player);
            if (multiplier > 1.0D) {
                float original = event.getAmount();
                float modified = (float) (original * multiplier);
                event.setAmount(modified);
            }
        }
    }

    /** 玩家死亡时重置炎热值为0 */
    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.level().isClientSide()) return;

        // 重置炎热值为0
        setHeat(player, 0.0D);
    }

    // --- 内部方法 --- //

    private static double getHeat(Player player) {
        return player.getPersistentData().getDouble(NBT_HEAT_VALUE);
    }

    private static void setHeat(Player player, double value) {
        player.getPersistentData().putDouble(NBT_HEAT_VALUE, value);
    }

    /**
     * 给予玩家炎狱效果，持续5分钟
     */
    private static void applyInfernalScorch(Player player) {
        MobEffectInstance infernalScorch = new MobEffectInstance(
                ModEffects.InfernalScorch,
                INFERNAL_SCORCH_DURATION_TICKS,
                0,
                false,
                true,
                true
        );
        player.addEffect(infernalScorch);
    }


    /** 计算当前炎热值对应的阶段（0~6） */
    private static int computeStage(double heat) {
        int stage = 0;
        for (double th : THRESHOLDS) {
            if (heat >= th) stage++;
            else break;
        }
        return stage; // stage 最大为 6，对应 6 个阈值
    }

    /** 根据阶段施加 速度&急迫（持续刷新）。stage=0 不加效果。 */
    private static void applySpeedAndHaste(Player player, int stage) {
        if (stage <= 0) return;

        // 速度：每个阈值都增加一级
        int speedAmplifier = stage - 1; // 0~6 -> 0~6
        
        // 急迫：每两个阈值才增加一级
        int hasteAmplifier = (stage - 1) / 2; // 0~6 -> 0~3
        
        // 速度（Speed）
        player.addEffect(new MobEffectInstance(
                MobEffects.MOVEMENT_SPEED,
                EFFECT_DURATION_TICKS,
                speedAmplifier,
                false,  // ambient
                false,  // visible
                false   // showIcon
        ));
        
        // 急迫（Haste）- 每两个阈值增加一级
        if (hasteAmplifier > 0) {
            player.addEffect(new MobEffectInstance(
                    MobEffects.DIG_SPEED,
                    EFFECT_DURATION_TICKS,
                    hasteAmplifier, // 转换为0~3
                    false,
                    false,
                    false
            ));
        }
    }

    /** 计算火焰伤害倍数（基于炎热阶段） */
    private static double calculateFireDamageMultiplier(Player player) {
        double heat = getHeat(player);
        int stage = computeStage(heat);
        
        // 如果玩家具有炎狱状态，则使用阈值6的伤害倍数
        if (player.hasEffect(ModEffects.InfernalScorch)) {
            stage = 6; // 仅用于计算伤害倍数
        }
        
        if (stage <= 0) return 1.0D; // 无炎热效果时无倍数
        
        // 1级1.2，2级1.4，3级1.6，以此类推
        return FIRE_DAMAGE_BASE_MULTIPLIER + (stage - 1) * 0.4D;
    }
}