package com.petitioner0.divinecore.edicts.ninth_edicts;

import com.petitioner0.divinecore.items.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntitySelector;

import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.level.ExplosionEvent;
import com.petitioner0.divinecore.DivineCore;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.Set;
import java.util.HashSet;

@EventBusSubscriber(modid = DivineCore.MODID)
public class NinthEdicts {

    private static final int CHECK_INTERVAL_TICKS = 20;      // Check every 1 second
    private static final int REQUIRED_SECONDS     = 30;      // 30 consecutive seconds
    private static final double SEARCH_RADIUS     = 32.0D;   // Monster search radius
    private static final double TELEPORT_OFFSET_Y = 0.1D;    
    

    private static final Map<UUID, Integer> secondsInStrictDark = new HashMap<>();
    private static int tickAccumulator = 0;
    // Tracked creeper information
    private static final Map<UUID, CreeperTrackingData> trackedCreepers = new HashMap<>();
    // Block positions around players that need monitoring
    private static final Map<UUID, Set<BlockPos>> playerMonitoredBlocks = new HashMap<>();

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post evt) {
        // Use global tick accumulator for "once per second" sampling
        tickAccumulator++;
        if (tickAccumulator % CHECK_INTERVAL_TICKS != 0) return;

        for (ServerLevel level : evt.getServer().getAllLevels()) {
            for (Player player : level.players()) {
            if (player.isSpectator()) {
                secondsInStrictDark.remove(player.getUUID());
                continue;
            }

            BlockPos head = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());

            boolean noLight = isNoLight(level, head);
            boolean notOpenAir = !isOpenAirStrict(level, head); 

            boolean condition = noLight && notOpenAir;

            int sec = secondsInStrictDark.getOrDefault(player.getUUID(), 0);
            if (condition) {
                sec += 1; 
                
                if (sec >= REQUIRED_SECONDS) {
                    pullNearestMonster(level, player);
                    sec = 0;
                }
            } else {
                sec = 0;
            }
            secondsInStrictDark.put(player.getUUID(), sec);
            }
        }
    }

    private static boolean isOpenAirStrict(Level level, BlockPos headPos) {
        if (!level.dimensionType().hasSkyLight()) return false; // Dimensions without sky light are not considered open air
        int topYPlusOne = level.getHeight(Heightmap.Types.MOTION_BLOCKING, headPos.getX(), headPos.getZ());
        // If head position >= (topYPlusOne - 1), there are no blocking blocks above
        return headPos.getY() >= topYPlusOne - 1;
    }

    // Check: No light source (block light <= 5 and sky light <= 5)
    private static boolean isNoLight(Level level, BlockPos pos) {
        int blockLight = level.getBrightness(LightLayer.BLOCK, pos);
        int skyLight   = level.getBrightness(LightLayer.SKY, pos);
        return blockLight <=5 && skyLight <=5;
    }

    // Behavior: Pull nearest monster to player
    private static boolean pullNearestMonster(ServerLevel level, Player player) {
        AABB box = AABB.unitCubeFromLowerCorner(player.position()).inflate(SEARCH_RADIUS);

        var candidates = level.getEntitiesOfClass(Mob.class, box,
                EntitySelector.NO_SPECTATORS.and(mob -> mob.isAlive() && mob.isEffectiveAi() && mob instanceof net.minecraft.world.entity.monster.Enemy));

        if (candidates.isEmpty()) return false;

        Mob nearest = candidates.stream()
                .min(Comparator.comparingDouble(e -> e.distanceToSqr(player)))
                .orElse(null);

        if (nearest == null) return false;

        // Teleport to player's feet (slightly elevated) and face the player
        double x = player.getX();
        double y = player.getY() + TELEPORT_OFFSET_Y;
        double z = player.getZ();

        // To avoid getting stuck in blocks: if target block is solid, try +1Y
        BlockPos tpPos = BlockPos.containing(x, y, z);
        if (!level.getBlockState(tpPos).getCollisionShape(level, tpPos).isEmpty()) {
            y += 1.0D;
        }

        nearest.teleportTo(x, y, z);
        nearest.resetFallDistance();

        // If it's a creeper, start tracking
        if (nearest instanceof Creeper creeper) {
            startCreeperTracking(creeper, player);
        }

        return true;
    }

    /**
     * Start tracking creeper
     */
    private static void startCreeperTracking(Creeper creeper, Player player) {
        UUID creeperId = creeper.getUUID();
        
        // Create tracking data
        CreeperTrackingData trackingData = new CreeperTrackingData(creeper, player);
        trackedCreepers.put(creeperId, trackingData);
        
        // Set up block monitoring around player
        setupPlayerBlockMonitoring(player);
    }
    
    /**
     * Set up block monitoring around player
     */
    private static void setupPlayerBlockMonitoring(Player player) {
        UUID playerId = player.getUUID();
        Set<BlockPos> monitoredBlocks = new HashSet<>();
        
        // Get player eye position coordinates
        BlockPos eyePos = BlockPos.containing(player.getX(), player.getEyeY(), player.getZ());
        
        // Check front, back, left, right (same height)
        monitoredBlocks.add(eyePos.north());
        monitoredBlocks.add(eyePos.south());
        monitoredBlocks.add(eyePos.east());
        monitoredBlocks.add(eyePos.west());
        
        // Check one block lower front, back, left, right
        BlockPos lowerPos = eyePos.below();
        monitoredBlocks.add(lowerPos.north());
        monitoredBlocks.add(lowerPos.south());
        monitoredBlocks.add(lowerPos.east());
        monitoredBlocks.add(lowerPos.west());
        
        // Check directly above
        monitoredBlocks.add(eyePos.above());
        
        // Check two blocks below
        monitoredBlocks.add(eyePos.below(2));
        
        playerMonitoredBlocks.put(playerId, monitoredBlocks);
    }
    
    /**
     * Check if all blocks around player are solid blocks
     */
    private static boolean areAllBlocksSolid(Level level, Set<BlockPos> blockPositions) {
        for (BlockPos pos : blockPositions) {
            BlockState state = level.getBlockState(pos);
            // Check if it's a solid block (has collision shape)
            if (state.getCollisionShape(level, pos).isEmpty()) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Creeper explosion event handling
     */
    @SubscribeEvent
    public static void onExplosionDetonate(ExplosionEvent.Detonate evt) {
        var explosion = evt.getExplosion();
        var level = (ServerLevel) evt.getLevel();
        var cause = explosion.getDirectSourceEntity(); // May be null
        if (!(cause instanceof Creeper creeper)) return;

        UUID id = creeper.getUUID();
        CreeperTrackingData data = trackedCreepers.get(id);
        if (data == null) return; // Not a tracked object

        Player p = data.getPlayer();
        if (p == null || p.isRemoved()) { 
            cleanupTracking(id); 
            return; 
        }
        
        Set<BlockPos> monitoredBlocks = playerMonitoredBlocks.get(p.getUUID());
        if (monitoredBlocks == null || !areAllBlocksSolid(p.level(), monitoredBlocks)) {
            cleanupTracking(id);
            return;
        }

        ItemHelper.dropItemAt(level, 
            new net.minecraft.world.phys.Vec3(explosion.center().x + 0.5, explosion.center().y + 0.5, explosion.center().z + 0.5), 
            "sundered_gloom", 1);
        
        cleanupTracking(id);
    }
    
    /**
     * Block break event handling
     */
    @SubscribeEvent
    public static void onBlockBreak(BlockEvent.BreakEvent event) {
        if (event.isCanceled()) {
            return;
        }
        
        BlockPos brokenPos = event.getPos();
        
        // Check if any player-monitored blocks were broken
        for (Map.Entry<UUID, Set<BlockPos>> entry : playerMonitoredBlocks.entrySet()) {
            Set<BlockPos> monitoredBlocks = entry.getValue();
            if (monitoredBlocks.contains(brokenPos)) {
                // Clean up tracking data for this player
                cleanupPlayerTracking(entry.getKey());
                break;
            }
        }
    }
    
    
    /**
     * Clean up tracking data
     */
    private static void cleanupTracking(UUID creeperId) {
        CreeperTrackingData trackingData = trackedCreepers.remove(creeperId);
        if (trackingData != null && trackingData.getPlayer() != null) {
            cleanupPlayerTracking(trackingData.getPlayer().getUUID());
        }
    }
    
    /**
     * Clean up player tracking data
     */
    private static void cleanupPlayerTracking(UUID playerId) {
        playerMonitoredBlocks.remove(playerId);
        
        // Clean up all creeper tracking associated with this player
        trackedCreepers.entrySet().removeIf(entry -> 
            entry.getValue().getPlayer() != null && 
            entry.getValue().getPlayer().getUUID().equals(playerId));
    }
    
    
    /**
     * Creeper tracking data class
     */
    private static class CreeperTrackingData {
        private final Player player;
        
        public CreeperTrackingData(Creeper creeper, Player player) {
            this.player = player;
        }
        
        public Player getPlayer() {
            return player;
        }
    }
}