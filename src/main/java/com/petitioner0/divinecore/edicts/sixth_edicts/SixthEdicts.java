package com.petitioner0.divinecore.edicts.sixth_edicts;

import com.petitioner0.divinecore.FTBHelper;
import com.petitioner0.divinecore.items.ItemHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class SixthEdicts {

    public static void init(IEventBus modEventBus, IEventBus gameEventBus) {
        gameEventBus.register(SixthEdicts.class);
    }

    private static final Map<UUID, ArrayDeque<Long>> KILL_TIMESTAMPS = new HashMap<>();
    private static final Map<UUID, Long> IGNORE_UNTIL_TICK = new HashMap<>();
    private static final Map<UUID, Integer> NAUSEA_KILL_COUNT = new HashMap<>();

    private static final Map<UUID, Map<EntityType<?>, Integer>> WINDOW_KILL_TYPES = new HashMap<>();
    private static final Map<UUID, Map<EntityType<?>, Integer>> FROZEN_POOL = new HashMap<>();

    // 新增：击杀累积窗口结束 tick
    private static final Map<UUID, Long> WINDOW_END_TICK = new HashMap<>();

    private static final int WINDOW_SECONDS = 20;
    private static final int WINDOW_TICKS = WINDOW_SECONDS * 20;
    private static final int EXTRA_TIME_PER_KILL = 60; // 3 秒

    private static final int REQUIRED_KILLS = 10;
    private static final int NAUSEA_SECONDS = 30;
    private static final int NAUSEA_TICKS = NAUSEA_SECONDS * 20;
    private static final int REWARD_THRESHOLD = 15;

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

        ServerPlayer player = getKillerAsPlayer(event.getSource().getEntity());
        if (player == null) return;

        Level level = player.level();
        if (level.isClientSide()) return;

        var server = level.getServer();
        if (server == null) return;

        long now = server.overworld().getGameTime();
        UUID id = player.getUUID();

        // --- 处于反胃期间，只计数 ---
        long ignoreUntil = IGNORE_UNTIL_TICK.getOrDefault(id, 0L);
        if (now < ignoreUntil) {
            int newCount = NAUSEA_KILL_COUNT.merge(id, 1, Integer::sum);
            if (newCount == REWARD_THRESHOLD) {
                FTBHelper.completeTask(player, "35B5FC632628CFF7");
            }
            return;
        }

        // --- 正常击杀（用于 10 杀进入反胃） ---
        ArrayDeque<Long> deque = KILL_TIMESTAMPS.computeIfAbsent(id, k -> new ArrayDeque<>());
        deque.addLast(now);

        // ---- 窗口结束 tick 初始化或延长 ----
        long windowEnd = WINDOW_END_TICK.getOrDefault(id, now + WINDOW_TICKS);
        windowEnd += EXTRA_TIME_PER_KILL;
        WINDOW_END_TICK.put(id, windowEnd);

        // --- 如果窗口已过期，清空 ---
        if (now > windowEnd) {
            deque.clear();
            WINDOW_KILL_TYPES.remove(id);
            return;
        }

        // --- 记录怪物类型（窗口有效时） ---
        WINDOW_KILL_TYPES
                .computeIfAbsent(id, k -> new HashMap<>())
                .merge(victim.getType(), 1, Integer::sum);

        // --- 达成 10 杀，进入反胃 ---
        if (deque.size() >= REQUIRED_KILLS) {

            // 冻结池
            Map<EntityType<?>, Integer> types = WINDOW_KILL_TYPES.remove(id);
            if (types == null || types.isEmpty()) {
                FROZEN_POOL.put(id, Map.of());
            } else {
                FROZEN_POOL.put(id, new HashMap<>(types));
            }

            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, NAUSEA_TICKS, 0, false, true, true));
            player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, NAUSEA_TICKS, 2, false, true, true));

            IGNORE_UNTIL_TICK.put(id, now + NAUSEA_TICKS);
            NAUSEA_KILL_COUNT.put(id, 0);

            // 清理窗口
            WINDOW_END_TICK.remove(id);
            deque.clear();

            FTBHelper.completeTask(player, "1BCD2EA2DB9C7253");
        }
    }

    @SubscribeEvent
    public static void onPlayerLogout(final PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getEntity() instanceof ServerPlayer p) {
            UUID id = p.getUUID();
            KILL_TIMESTAMPS.remove(id);
            IGNORE_UNTIL_TICK.remove(id);
            NAUSEA_KILL_COUNT.remove(id);
            WINDOW_KILL_TYPES.remove(id);
            FROZEN_POOL.remove(id);
            WINDOW_END_TICK.remove(id);
        }
    }

    @SubscribeEvent
    public static void onPlayerClone(final PlayerEvent.Clone event) {
        if (event.getOriginal() instanceof ServerPlayer oldP) {
            UUID id = oldP.getUUID();
            KILL_TIMESTAMPS.remove(id);
            IGNORE_UNTIL_TICK.remove(id);
            NAUSEA_KILL_COUNT.remove(id);
            WINDOW_KILL_TYPES.remove(id);
            FROZEN_POOL.remove(id);
            WINDOW_END_TICK.remove(id);
        }
    }

    @SubscribeEvent
    public static void onServerTick(final ServerTickEvent.Post event) {
        long currentTick = event.getServer().overworld().getGameTime();

        // --- 反胃期间刷怪 ---
        for (ServerPlayer player : event.getServer().getPlayerList().getPlayers()) {
            UUID id = player.getUUID();
            long ignoreUntil = IGNORE_UNTIL_TICK.getOrDefault(id, 0L);
            if (currentTick >= ignoreUntil) continue;

            Map<EntityType<?>, Integer> pool = FROZEN_POOL.get(id);
            if (pool == null || pool.isEmpty()) continue;

            ServerLevel level = player.serverLevel();

            if (level.getRandom().nextDouble() > 0.10) continue;

            EntityType<?> type = chooseWeighted(pool, level.getRandom());
            if (type == null) continue;

            int px = player.blockPosition().getX();
            int py = player.blockPosition().getY();
            int pz = player.blockPosition().getZ();

            int x = px + level.getRandom().nextInt(5) - 2;
            int z = pz + level.getRandom().nextInt(5) - 2;

            for (int i = 0; i < 3; i++) {
                BlockPos pos = new BlockPos(x, py + i, z);
                if (level.isEmptyBlock(pos)) {
                    Entity mob = type.create(level);
                    if (mob != null) {
                        mob.moveTo(x + 0.5, py + i, z + 0.5, level.getRandom().nextFloat() * 360F, 0);
                        level.addFreshEntity(mob);
                    }
                    break;
                }
            }
        }

        // --- 反胃结束、结算奖励 ---
        Iterator<Map.Entry<UUID, Long>> iterator = IGNORE_UNTIL_TICK.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            UUID playerId = entry.getKey();
            long expireTick = entry.getValue();

            if (currentTick >= expireTick) {
                ServerPlayer player = event.getServer().getPlayerList().getPlayer(playerId);

                FTBHelper.completeTask(player, "3A08009261C94769");

                Integer killCount = NAUSEA_KILL_COUNT.remove(playerId);
                if (killCount != null && killCount >= REWARD_THRESHOLD && player != null) {
                    ItemHelper.giveItemToPlayer(player, "wailing_wraith", 1);
                }

                FROZEN_POOL.remove(playerId);
                iterator.remove();
            }
        }
    }

    private static EntityType<?> chooseWeighted(Map<EntityType<?>, Integer> pool, RandomSource rnd) {
        int total = pool.values().stream().mapToInt(i -> i).sum();
        if (total <= 0) return null;

        int r = rnd.nextInt(total);
        for (var e : pool.entrySet()) {
            r -= e.getValue();
            if (r < 0) return e.getKey();
        }
        return null;
    }
}
