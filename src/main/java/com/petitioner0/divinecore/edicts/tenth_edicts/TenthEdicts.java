package com.petitioner0.divinecore.edicts.tenth_edicts;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.items.ItemHelper;
import com.petitioner0.divinecore.damage_type.ModDamageSources;
import com.petitioner0.divinecore.damage_type.ModDamageTypes;
import com.petitioner0.divinecore.net.ServerSend;
import com.simibubi.create.AllItems;

@EventBusSubscriber(modid = DivineCore.MODID)
public class TenthEdicts {
    private static final String NBT_KEY = "divinecore_sun_gaze_counter";
    private static final int THRESHOLD = 100;
    private static final double ACCEPT_DEG = 10.0;      // Allow angle deviation ±10°
    private static final double ACCEPT_RAD = Math.toRadians(ACCEPT_DEG);
    private static final int START_TICK = 1000;         // Only calculate between 1000~12000 ticks
    private static final int END_TICK   = 12000;
    // Maximum distance for ray towards sun direction
    private static final int BLINDNESS_TICKS = 10 * 20; // 10 seconds
    
    private static final int MAX_LEVEL = 3; 
    private static final String LEVEL_NBT_KEY = "divinecore_sun_gaze_level";
    
    // Sun direction calculation constants
    private static final double DEG_PER_TICK = 0.01154; // Angle change per tick

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        Level level = player.level();

        if (level.isClientSide()) return;
        if (!(player instanceof ServerPlayer sp)) return;
        if (!sp.isAlive()) return;

        // Only trigger in overworld
        if (level.dimension() != Level.OVERWORLD) return;
        
        if (level.isRaining() || level.isThundering()) return;
        
        // Only calculate between 1000~12000 ticks during daytime
        long dayTime = level.getDayTime() % 24000L;
        if (dayTime < START_TICK || dayTime > END_TICK) return;

        // Check if player is blind or wearing pumpkin
        if (sp.hasEffect(MobEffects.BLINDNESS) || isWearingPumpkin(sp)) {
            // Send 0 progress to reset client display
            ServerSend.sendSunGazeProgress(sp, 0f);
            return;
        }

        // Calculate sun direction
        Vec3 sunDir = computeSunDirection(dayTime);

        // Player's look direction unit vector
        Vec3 look = sp.getLookAngle().normalize();

        // Get current counter value and level
        var tag = sp.getPersistentData();
        int counter = tag.getInt(NBT_KEY);
        int currentLevel = tag.getInt(LEVEL_NBT_KEY);

        // Check if angle with sun direction is ≤ 10°
        double dot = look.dot(sunDir);
        dot = Math.max(-1.0, Math.min(1.0, dot));
        double angle = Math.acos(dot);
        
        boolean angleValid = angle <= ACCEPT_RAD;
        
        // Check if there are no blocks blocking line of sight to sun (cast long ray from player eyes towards sun)
        Vec3 eye = sp.getEyePosition();
        double rayDist = Math.max(128.0, level.getMaxBuildHeight() - player.getY());
        boolean visibleEnough = angleValid && isLineClearOrGlassOnly(level, sp, eye, sunDir, rayDist);
        
        if (visibleEnough) {
            if (counter < THRESHOLD) {
                counter++;
                tag.putInt(NBT_KEY, counter);
            }

            if (counter >= THRESHOLD) {
                // Only detect level and apply corresponding effects when reaching threshold
                int newLevel = calculateLevel(sp, level, eye, sunDir, rayDist);
                applyLevelEffect(sp, newLevel);
                tag.putInt(NBT_KEY, 0); 
                
                if (currentLevel != newLevel) {
                    tag.putInt(LEVEL_NBT_KEY, newLevel);
                }
            }
        } else {
            if (counter > 0) {
                counter--;
                tag.putInt(NBT_KEY, counter);
            }
        }
        
        if (counter > 0) {
            float progress = Math.min(1f, Math.max(0f, (float) counter / THRESHOLD));
            ServerSend.sendSunGazeProgress(sp, progress);
        } else {
            ServerSend.sendSunGazeProgress(sp, 0f);
        }
    }
    
    private static Vec3 computeSunDirection(long timeOfDay) {
        double delta = (timeOfDay - 6000L) * DEG_PER_TICK; // Angle offset from zenith (degrees)
        double elevDeg = 90.0 - Math.abs(delta); // Elevation angle
        
        if (elevDeg < 0) elevDeg = 0;
        if (elevDeg > 90) elevDeg = 90;
        
        double elevRad = Math.toRadians(elevDeg);
        
        double signX = (timeOfDay <= 6000L) ? 1.0 : -1.0;
        double x = Math.cos(elevRad) * signX;
        double y = Math.sin(elevRad);
        double z = 0.0;
        
        Vec3 v = new Vec3(x, y, z);
        return v.normalize();
    }
    
    private static int calculateLevel(ServerPlayer player, Level world, Vec3 eye, Vec3 sunDir, double rayDist) {
        int level = 0;
        
        // Condition 1: Line of sight only blocked by glass
        if (isOnlyGlassObstructionStrict(world, player, eye, sunDir, rayDist)) {
            level++;
        }
        
        // Condition 2: Using telescope
        if (isUsingTelescope(player)) {
            level++;
        }
        
        // Condition 3: Wearing engineer's goggles on head
        if (isWearingEngineersGoggles(player)) {
            level++;
        }
        
        return Math.min(level, MAX_LEVEL);
    }
    
    /**
     * Whether line is unobstructed or only glass (unobstructed true; only glass also true; other obstructions false)
     * Used for counting phase
     */
    private static boolean isLineClearOrGlassOnly(Level level, ServerPlayer sp, Vec3 eye, Vec3 dir, double rayDist) {
        Vec3 start = eye;
        Vec3 end = eye.add(dir.scale(rayDist));
        for (int i = 0; i < 16; i++) {
            ClipContext ctx = new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp);
            BlockHitResult hit = level.clip(ctx);
            if (hit == null || hit.getType() == HitResult.Type.MISS) {
                return true; // Can count
            }
            BlockPos pos = hit.getBlockPos();
            Block block = level.getBlockState(pos).getBlock();
            if (isGlassBlock(block)) {
                start = hit.getLocation().add(dir.scale(0.01));
                continue;
            } else {
                return false;
            }
        }
        return true;
    }

    private static boolean isOnlyGlassObstructionStrict(Level level, ServerPlayer sp, Vec3 eye, Vec3 dir, double rayDist) {
        Vec3 start = eye;
        Vec3 end = eye.add(dir.scale(rayDist));
        boolean sawGlass = false;
        for (int i = 0; i < 16; i++) {
            ClipContext ctx = new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, sp);
            BlockHitResult hit = level.clip(ctx);
            if (hit == null || hit.getType() == HitResult.Type.MISS) {
                // As long as glass was seen and no other obstructions encountered after, count as "only glass obstruction"
                return sawGlass;
            }
            BlockPos pos = hit.getBlockPos();
            Block block = level.getBlockState(pos).getBlock();
            if (isGlassBlock(block)) {
                sawGlass = true;
                start = hit.getLocation().add(dir.scale(0.01));
            } else {
                return false;
            }
        }
        return sawGlass;
    }
    
    /**
     * Check if block is glass type
     */
    private static boolean isGlassBlock(Block block) {
        return block == Blocks.GLASS ||
               block == Blocks.GLASS_PANE ||
               block == Blocks.WHITE_STAINED_GLASS ||
               block == Blocks.ORANGE_STAINED_GLASS ||
               block == Blocks.MAGENTA_STAINED_GLASS ||
               block == Blocks.LIGHT_BLUE_STAINED_GLASS ||
               block == Blocks.YELLOW_STAINED_GLASS ||
               block == Blocks.LIME_STAINED_GLASS ||
               block == Blocks.PINK_STAINED_GLASS ||
               block == Blocks.GRAY_STAINED_GLASS ||
               block == Blocks.LIGHT_GRAY_STAINED_GLASS ||
               block == Blocks.CYAN_STAINED_GLASS ||
               block == Blocks.PURPLE_STAINED_GLASS ||
               block == Blocks.BLUE_STAINED_GLASS ||
               block == Blocks.BROWN_STAINED_GLASS ||
               block == Blocks.GREEN_STAINED_GLASS ||
               block == Blocks.RED_STAINED_GLASS ||
               block == Blocks.BLACK_STAINED_GLASS ||
               block == Blocks.WHITE_STAINED_GLASS_PANE ||
               block == Blocks.ORANGE_STAINED_GLASS_PANE ||
               block == Blocks.MAGENTA_STAINED_GLASS_PANE ||
               block == Blocks.LIGHT_BLUE_STAINED_GLASS_PANE ||
               block == Blocks.YELLOW_STAINED_GLASS_PANE ||
               block == Blocks.LIME_STAINED_GLASS_PANE ||
               block == Blocks.PINK_STAINED_GLASS_PANE ||
               block == Blocks.GRAY_STAINED_GLASS_PANE ||
               block == Blocks.LIGHT_GRAY_STAINED_GLASS_PANE ||
               block == Blocks.CYAN_STAINED_GLASS_PANE ||
               block == Blocks.PURPLE_STAINED_GLASS_PANE ||
               block == Blocks.BLUE_STAINED_GLASS_PANE ||
               block == Blocks.BROWN_STAINED_GLASS_PANE ||
               block == Blocks.GREEN_STAINED_GLASS_PANE ||
               block == Blocks.RED_STAINED_GLASS_PANE ||
               block == Blocks.BLACK_STAINED_GLASS_PANE;
    }
    
    /**
     * Check if player is using telescope
     */
    private static boolean isUsingTelescope(ServerPlayer player) {
        // Currently holding right-click to use an item + that item is telescope
        return player.isUsingItem() && player.getUseItem().is(net.minecraft.world.item.Items.SPYGLASS);
    }
    
    /**
     * Check if player is wearing engineer's goggles on head
     */
    public static boolean isWearingEngineersGoggles(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        return !head.isEmpty() && head.is(AllItems.GOGGLES.get());
    }
    
    /**
     * Check if player is wearing pumpkin on head
     */
    private static boolean isWearingPumpkin(Player player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        return !head.isEmpty() && head.is(net.minecraft.world.item.Items.CARVED_PUMPKIN);
    }
    
    /**
     * Apply effects for corresponding level
     */
    private static void applyLevelEffect(ServerPlayer player, int level) {
        switch (level) {
            case 0:
                // Base effect: blindness
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_TICKS, 0, false, true, true));
                break;
            case 1:
                // Level 1 effect: blindness x3
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_TICKS*3, 0, false, true, true));
                break;
            case 2:
                // Level 2 effect: blindness + continuous burning
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, BLINDNESS_TICKS*3, 0, false, true, true));
                // Set player fire time (continuous burning effect)
                player.setRemainingFireTicks(1000 * 10); // 1000 seconds burning
                break;
            case 3:
                // Level 3 effect: deal 80% max health damage, burning and permanent blindness
                // Calculate damage: 80% of max health, rounded down
                float maxHealth = player.getMaxHealth();
                float damageAmount = (float) Math.floor(maxHealth * 0.8f);
                
                player.level().playSound(null, player.getX(), player.getY(), player.getZ(), 
                    SoundEvents.GLASS_BREAK, SoundSource.PLAYERS, 1.0f, 1.2f);
                
                
                // Break telescope in hand
                if (isUsingTelescope(player)) {
                    ItemStack telescope = player.getUseItem();
                    if (telescope.getCount() > 1) {
                        telescope.shrink(1);
                    }
                }
                
                // Deal damage
                player.hurt(ModDamageSources.of(player.level(), ModDamageTypes.SUN_GAZE), damageAmount);
                
                // Permanent blindness (set to very long duration)
                player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, Integer.MAX_VALUE, 0, false, true, true));
                
                // Set fire time (1000 seconds)
                player.setRemainingFireTicks(1000 * 20);
                ItemHelper.giveItemToPlayer(player, "ember_of_the_sunforged", 1);
                break;
        }
    }
}