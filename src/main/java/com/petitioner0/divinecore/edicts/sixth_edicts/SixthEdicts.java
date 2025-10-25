package com.petitioner0.divinecore.edicts.sixth_edicts;

import com.petitioner0.divinecore.items.ItemHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.phys.Vec3;
import java.util.*;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;

public class SixthEdicts {

    public static void init(IEventBus modEventBus, IEventBus gameEventBus) {
        gameEventBus.register(SixthEdicts.class);
    }

    // Kill timestamps within the last 20 seconds
    private static final Map<UUID, ArrayDeque<Long>> KILL_TIMESTAMPS = new HashMap<>();
    // Cooldown expiration tick for "not counting during nausea period" (regardless of whether effect is removed early)
    private static final Map<UUID, Long> IGNORE_UNTIL_TICK = new HashMap<>();
    // Kill count statistics during nausea period
    private static final Map<UUID, Integer> NAUSEA_KILL_COUNT = new HashMap<>();

    private static final int WINDOW_SECONDS = 20;
    private static final int WINDOW_TICKS = WINDOW_SECONDS * 20;
    private static final int REQUIRED_KILLS = 10;
    private static final int NAUSEA_SECONDS = 30;
    private static final int NAUSEA_TICKS = NAUSEA_SECONDS * 20;
    private static final int REWARD_THRESHOLD = 15; // Kill 15 monsters during nausea period to get reward

    private static final double DUPLICATE_PROB = 0.7; // 70% probability
    private static final int DUP_RADIUS = 24; // Radius around player for detection (in blocks)
    private static final int MAX_DUP_DEPTH = 7; // Maximum level for iterative duplication (to avoid runaway)
    private static final double DUP_OFFSET_MAX = 2.0; // Maximum duplication offset
    private static final String NBT_DUP_DEPTH_KEY = "divinecore_dup_depth";

    private static @Nullable ServerPlayer getKillerAsPlayer(Entity direct) {
        if (direct instanceof ServerPlayer sp)
            return sp;

        if (direct instanceof net.minecraft.world.entity.projectile.Projectile proj) {
            Entity owner = proj.getOwner();
            if (owner instanceof ServerPlayer sp)
                return sp;
            if (owner instanceof net.minecraft.world.entity.OwnableEntity ownable
                    && ownable.getOwner() instanceof ServerPlayer sp2) {
                return sp2;
            }
        }

        return null;
    }

    @SubscribeEvent
    public static void onLivingDeath(final LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (!(victim instanceof Monster))
            return;

        Entity src = event.getSource().getEntity();
        ServerPlayer player = getKillerAsPlayer(src);
        if (player == null)
            return;

        Level level = player.level();
        if (level.isClientSide())
            return;

        var server = level.getServer();
        if (server == null)
            return;

        long now = server.overworld().getGameTime();
        UUID id = player.getUUID();

        long ignoreUntil = IGNORE_UNTIL_TICK.getOrDefault(id, 0L);
        if (now < ignoreUntil) {
            NAUSEA_KILL_COUNT.merge(id, 1, Integer::sum);
            return;
        }

        ArrayDeque<Long> deque = KILL_TIMESTAMPS.computeIfAbsent(id, k -> new ArrayDeque<>());

        deque.addLast(now);
        long cutoff = now - WINDOW_TICKS;
        while (!deque.isEmpty() && deque.peekFirst() < cutoff)
            deque.pollFirst();

        if (deque.size() >= REQUIRED_KILLS) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA_TICKS, 0, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, NAUSEA_TICKS, 2, false, true, true));
            IGNORE_UNTIL_TICK.put(id, now + NAUSEA_TICKS);
            NAUSEA_KILL_COUNT.put(id, 0); 
            deque.clear();
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            UUID id = p.getUUID();
            KILL_TIMESTAMPS.remove(id);
            IGNORE_UNTIL_TICK.remove(id);
            NAUSEA_KILL_COUNT.remove(id);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(final PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer oldP) {
            UUID id = oldP.getUUID();
            KILL_TIMESTAMPS.remove(id);
            IGNORE_UNTIL_TICK.remove(id);
            NAUSEA_KILL_COUNT.remove(id);
        }
    }

    @SubscribeEvent
    public static void onServerTick(final ServerTickEvent.Post event) {
        if (event.getServer().getTickCount() % 20 != 0)
            return; 

        long currentTick = event.getServer().overworld().getGameTime();

        // Check all players' timers
        Iterator<Map.Entry<UUID, Long>> iterator = IGNORE_UNTIL_TICK.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, Long> entry = iterator.next();
            UUID playerId = entry.getKey();
            long expireTick = entry.getValue(); 

            if (currentTick >= expireTick) {
                // Reached counting time, check kill count and give reward
                Integer killCount = NAUSEA_KILL_COUNT.remove(playerId);
                if (killCount != null && killCount >= REWARD_THRESHOLD) {
                    ServerPlayer player = event.getServer().getPlayerList().getPlayer(playerId);
                    if (player != null) {
                        ItemHelper.giveItemToPlayer(player, "wailing_wraith", 1);
                    }
                }
                iterator.remove();
            }
        }
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(final EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide())
            return;

        if (event.loadedFromDisk())
            return;
        if (!(event.getEntity() instanceof Enemy))
            return;

        Entity entity = event.getEntity();
        ServerLevel level = (ServerLevel) event.getLevel();

        // Only consider duplication when there are players in "nausea window" nearby
        long now = level.getServer().overworld().getGameTime(); 
        boolean nearActivePlayer = level.players().stream().anyMatch(p -> {
            if (!(p instanceof ServerPlayer sp))
                return false;
            if (sp.distanceToSqr(entity) > (double) DUP_RADIUS * DUP_RADIUS)
                return false;
            long ignoreUntil = IGNORE_UNTIL_TICK.getOrDefault(sp.getUUID(), 0L);
            return now < ignoreUntil;
        });
        if (!nearActivePlayer)
            return;

        // Read current entity's duplication level
        CompoundTag data = entity.getPersistentData();
        int depth = data.getInt(NBT_DUP_DEPTH_KEY);
        if (depth >= MAX_DUP_DEPTH)
            return;

        if (level.getRandom().nextDouble() >= DUPLICATE_PROB)
            return;

        EntityType<?> type = entity.getType();
        Entity dup = type.create(level);
        if (dup == null)
            return;

        // Random slight offset
        double dx = (level.getRandom().nextDouble() * 2 - 1) * DUP_OFFSET_MAX;
        double dz = (level.getRandom().nextDouble() * 2 - 1) * DUP_OFFSET_MAX;
        Vec3 pos = entity.position().add(dx, 0, dz);
        dup.moveTo(pos.x, pos.y, pos.z, entity.getYRot(), entity.getXRot());

        // Mark duplication depth +1, allow further iteration
        dup.getPersistentData().putInt(NBT_DUP_DEPTH_KEY, depth + 1);

        level.addFreshEntity(dup);
    }
}