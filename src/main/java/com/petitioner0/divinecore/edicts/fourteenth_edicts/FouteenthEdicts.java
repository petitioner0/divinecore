package com.petitioner0.divinecore.edicts.fourteenth_edicts;

import com.petitioner0.divinecore.DivineCore;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Phantom;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

/**
 * High-altitude Phantom spawning module (NeoForge 1.21.1)
 */
@EventBusSubscriber(modid = DivineCore.MODID)
public final class FouteenthEdicts {
    private static final int CHECK_Y_MIN = 251;                      // Trigger threshold: player Y > 250
    private static final int CENTER_Y_OFFSET = 28;                  // Center Y offset
    private static final int HALF_XZ = 10;                          // Half XZ range
    private static final int HALF_Y = 7;                            // Half Y range
    private static final int FIND_SPOT_TRIES = 128;                  // Random point attempt count

    private static final int PERIOD_TICKS = 100;                     // Period in ticks
    private static final int COOLDOWN_TICKS = 100;                   // Cooldown in ticks
    private static final int MAX_WAVE = 5;                           // Maximum 5 phantoms per wave

    private static final int PHANTOM_SIZE = 50;                      // Phantom size
    private static final double PHANTOM_ATTACK = 100000.0D;          // Attack damage
    private static final double PHANTOM_FLYING_SPEED = 1.5D;         // Flying speed
    private static final double PHANTOM_MOVE_SPEED = 1.5D;           // Movement speed

    private static final String KEY_OWNER = ResourceLocation.fromNamespaceAndPath("divinecore", "owner").toString();

    private static final Map<UUID, PlayerState> STATES = new HashMap<>();
    private static final Map<UUID, Set<UUID>> PLAYER_PHANTOMS = new HashMap<>();


    private record PlayerState(boolean inZone,
                               long nextTick,
                               int wave,
                               long cooldownUntil) {
        PlayerState enter(long now) {
            return new PlayerState(true, now, 1, this.cooldownUntil);
        }
        PlayerState stayAndAdvanceNext(long now) {
            return new PlayerState(true, now + PERIOD_TICKS, Math.min(MAX_WAVE, this.wave + 1), this.cooldownUntil);
        }
        PlayerState resetOnLeave(long now) {
            return new PlayerState(false, Long.MAX_VALUE, 1, now + COOLDOWN_TICKS);
        }
        PlayerState idleCooldown() { return this; }
    }


    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide()) return;
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;

        var player = event.getEntity();
        long now = level.getGameTime();
        UUID id = player.getUUID();
        PlayerState st = STATES.getOrDefault(id, new PlayerState(false, Long.MAX_VALUE, 1, 0));

        if (!level.dimension().equals(Level.OVERWORLD)) return;

        boolean above = player.getBlockY() >= CHECK_Y_MIN;
        boolean cooling = now < st.cooldownUntil;

        if (!above) {
            if (st.inZone) STATES.put(id, st.resetOnLeave(now));
            else STATES.put(id, st.idleCooldown());
            return;
        }
        if (cooling) {
            STATES.put(id, st.idleCooldown());
            return;
        }

        // First entry: spawn immediately
        if (!st.inZone) {
            st = st.enter(now);
            STATES.put(id, st);
            spawnWave(level, player.blockPosition(), st.wave, player);
            return;
        }

        // After staying for 5 seconds, spawn and increment
        if (now >= st.nextTick) {
            // Only spawn phantoms if not at max wave count
            if (st.wave <= MAX_WAVE) {
                spawnWave(level, player.blockPosition(), st.wave, player);
            }
            STATES.put(id, st.stayAndAdvanceNext(now));
        }
    }


    /** Phantom disappears after hitting once */
    @SubscribeEvent
    public static void onLivingDamage(LivingDamageEvent.Post event) {
        var src = event.getSource();

        Entity attacker = src.getDirectEntity();
        if (attacker == null) attacker = src.getEntity();
        if (!(attacker instanceof Phantom ph)) return;

        if (ph.getPersistentData().hasUUID(KEY_OWNER)) {
            UUID ownerId = ph.getPersistentData().getUUID(KEY_OWNER);
            // Remove from player's phantom list
            PLAYER_PHANTOMS.computeIfPresent(ownerId, (k, v) -> {
                v.remove(ph.getUUID());
                return v.isEmpty() ? null : v;
            });
            ph.discard();
        }
    }

    /** Clean up all phantoms when player dies */
    @SubscribeEvent
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof net.minecraft.world.entity.player.Player player)) return;
        if (player.level().isClientSide()) return;

        UUID playerId = player.getUUID();
        Set<UUID> phantoms = PLAYER_PHANTOMS.remove(playerId);
        if (phantoms == null || phantoms.isEmpty()) return;

        // Clean up all phantoms belonging to this player
        if (player.level() instanceof ServerLevel level) {
            for (UUID phantomId : phantoms) {
                Entity phantom = level.getEntity(phantomId);
                if (phantom instanceof Phantom && phantom.isAlive()) {
                    phantom.discard();
                }
            }
        }
    }

    private static void spawnWave(ServerLevel level, BlockPos playerPos, int count, Entity owner) {
        for (int i = 0; i < count; i++) {
            BlockPos pos = findAirBlock(level, playerPos);
            if (pos == null) continue;

            Phantom phantom = EntityType.PHANTOM.create(level);
            if (phantom == null) continue;

            phantom.moveTo(pos.getX() + 0.5, pos.getY() + 0.1, pos.getZ() + 0.5,
                    level.random.nextFloat() * 360f, 0);

            // Attributes and custom behavior
            phantom.setPhantomSize(PHANTOM_SIZE);
            var atk = phantom.getAttribute(Attributes.ATTACK_DAMAGE);
            if (atk != null) atk.setBaseValue(PHANTOM_ATTACK);
            var fly = phantom.getAttribute(Attributes.FLYING_SPEED);
            if (fly != null) fly.setBaseValue(PHANTOM_FLYING_SPEED);
            var move = phantom.getAttribute(Attributes.MOVEMENT_SPEED);
            if (move != null) move.setBaseValue(PHANTOM_MOVE_SPEED);
            
            var health = phantom.getAttribute(Attributes.MAX_HEALTH);
            if (health != null) health.setBaseValue(100000.0D);
            phantom.setHealth(phantom.getMaxHealth());

            // Mark ownership to player
            phantom.getPersistentData().putUUID(KEY_OWNER, owner.getUUID());

            if (level.addFreshEntity(phantom)) {
                // Add phantom to player's phantom list
                PLAYER_PHANTOMS.computeIfAbsent(owner.getUUID(), k -> new HashSet<>())
                        .add(phantom.getUUID());
            }
        }
    }

    /** Find air blocks in a 21×15×21 area centered 28 blocks above the player */
    private static BlockPos findAirBlock(ServerLevel level, BlockPos playerPos) {
        BlockPos center = playerPos.above(CENTER_Y_OFFSET);
        ThreadLocalRandom rnd = ThreadLocalRandom.current();

        for (int t = 0; t < FIND_SPOT_TRIES; t++) {
            int dx = rnd.nextInt(-HALF_XZ, HALF_XZ + 1);
            int dy = rnd.nextInt(-HALF_Y, HALF_Y + 1);
            int dz = rnd.nextInt(-HALF_XZ, HALF_XZ + 1);
            BlockPos p = center.offset(dx, dy, dz);

            if (level.isInWorldBounds(p)) {
                BlockState bs = level.getBlockState(p);
                if (bs.isAir()) return p;
            }
        }
        return level.isEmptyBlock(center) ? center : null;
    }
}