package com.petitioner0.divinecore.edicts.eleventh_edicts;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class WitherHealFromCrystalGoal extends net.minecraft.world.entity.ai.goal.Goal {
    private static final double RANGE = 96.0D;
    private static final int HEAL_PERIOD_TICKS = 10;
    private static final float HEAL_AMOUNT = 1.0F;

    // Global occupation table: avoid multiple withers occupying the same crystal
    private static final Map<UUID, UUID> CRYSTAL_TO_WITHER = new HashMap<>();

    private final net.minecraft.world.entity.boss.wither.WitherBoss wither;
    private net.minecraft.world.entity.boss.enderdragon.EndCrystal bound;
    private int healCd = 0;

    public WitherHealFromCrystalGoal(net.minecraft.world.entity.boss.wither.WitherBoss wither) {
        this.wither = wither;
        this.setFlags(java.util.EnumSet.noneOf(Flag.class));
    }

    @Override
    public boolean canUse() {
        if (!(wither.level() instanceof net.minecraft.server.level.ServerLevel level)) return false;
        if (!wither.isAlive()) return false;
        if (wither.getHealth() >= wither.getMaxHealth()) return false;
        if (!wither.level().dimension().equals(net.minecraft.world.level.Level.END)) return false;

        var nearest = pickNearestFreeCrystal(level, wither);
        // If there is already a binding and it is usable, continue; otherwise, if a nearest free crystal can be found, start the Goal
        return (bound != null && isUsableFor(level, wither, bound)) || nearest != null;
    }

    @Override
    public void start() {
        if (!(wither.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        // If there is no or invalid binding, pick the nearest free crystal
        if (!isUsableFor(level, wither, bound)) {
            release(level);
            bound = pickNearestFreeCrystal(level, wither);
            if (bound != null) bind(level, bound);
        }
        healCd = 0;
    }

    @Override
    public boolean canContinueToUse() {
        if (!(wither.level() instanceof net.minecraft.server.level.ServerLevel level)) return false;
        if (!wither.isAlive()) return false;
        if (wither.getHealth() >= wither.getMaxHealth()) return bound != null && isUsableFor(level, wither, bound); // 允许满血保持短暂绑定，避免抖动；也可直接返回 false 让它停
        return bound != null && isUsableFor(level, wither, bound);
    }

    @Override
    public void tick() {
        if (!(wither.level() instanceof net.minecraft.server.level.ServerLevel level)) return;
        //same as the dragon: occasionally reselect the nearest crystal
        if (wither.getRandom().nextInt(10) == 0) {
            var nearest = pickNearestFreeCrystal(level, wither);
            if (nearest != null && (bound == null || nearest.distanceToSqr(wither) < bound.distanceToSqr(wither))) {
                release(level);
                bind(level, nearest);
            }
        }

        if (bound != null) {
            // The beam points to the wither's upper body
            var beam = net.minecraft.core.BlockPos.containing(
                wither.getX(), wither.getY() + wither.getBbHeight() * 0.6, wither.getZ());
            bound.setBeamTarget(beam);

            // Every 10 ticks, heal 1 health
            if (wither.getHealth() < wither.getMaxHealth()) {
                if (healCd-- <= 0) {
                    wither.heal(HEAL_AMOUNT);
                    healCd = HEAL_PERIOD_TICKS;
                }
            }
        }
    }

    @Override
    public void stop() {
        if (wither.level() instanceof net.minecraft.server.level.ServerLevel level) {
            release(level);
        }
    }

    // —— Pick the nearest free crystal —— //
    private net.minecraft.world.entity.boss.enderdragon.EndCrystal pickNearestFreeCrystal(
            net.minecraft.server.level.ServerLevel level,
            net.minecraft.world.entity.boss.wither.WitherBoss wither
    ) {
        var box = wither.getBoundingBox().inflate(RANGE);
        var list = level.getEntitiesOfClass(net.minecraft.world.entity.boss.enderdragon.EndCrystal.class, box,
            c -> c != null && !c.isRemoved() && isCrystalFree(level, c));
        if (list.isEmpty()) return null;
        list.sort(java.util.Comparator.comparingDouble(c -> c.distanceToSqr(wither)));
        return list.getFirst();
    }

    private boolean isCrystalFree(net.minecraft.server.level.ServerLevel level,
                                  net.minecraft.world.entity.boss.enderdragon.EndCrystal c) {
        if (reservedByDragon(level, c)) return false;
        UUID occ = CRYSTAL_TO_WITHER.get(c.getUUID());
        return occ == null || occ.equals(wither.getUUID());
    }

    // Same as the dragon: is this crystal the "nearest crystal" of some dragon nearby?
    private boolean reservedByDragon(net.minecraft.server.level.ServerLevel level,
                                     net.minecraft.world.entity.boss.enderdragon.EndCrystal crystal) {
        var dragons = level.getEntitiesOfClass(net.minecraft.world.entity.boss.enderdragon.EnderDragon.class,
                crystal.getBoundingBox().inflate(RANGE));
        if (dragons.isEmpty()) return false;

        for (var dragon : dragons) {
            var nearby = level.getEntitiesOfClass(net.minecraft.world.entity.boss.enderdragon.EndCrystal.class,
                    dragon.getBoundingBox().inflate(32.0));
            if (nearby.isEmpty()) continue;
            net.minecraft.world.entity.boss.enderdragon.EndCrystal nearest = null;
            double best = Double.MAX_VALUE;
            for (var c : nearby) {
                double d = c.distanceToSqr(dragon);
                if (d < best) { best = d; nearest = c; }
            }
            if (nearest != null && nearest.getUUID().equals(crystal.getUUID())) return true;
        }
        return false;
    }

    // Bind/Release
    private void bind(net.minecraft.server.level.ServerLevel level,
                      net.minecraft.world.entity.boss.enderdragon.EndCrystal crystal) {
        // Release the old one first
        release(level);
        CRYSTAL_TO_WITHER.put(crystal.getUUID(), wither.getUUID());
        bound = crystal;
        var beam = net.minecraft.core.BlockPos.containing(
                wither.getX(), wither.getY() + wither.getBbHeight() * 0.6, wither.getZ());
        crystal.setBeamTarget(beam);
    }

    private void release(net.minecraft.server.level.ServerLevel level) {
        if (bound != null) {
            CRYSTAL_TO_WITHER.remove(bound.getUUID(), wither.getUUID());
            bound.setBeamTarget(null);
            bound = null;
        }
    }

    private boolean isUsableFor(net.minecraft.server.level.ServerLevel level,
                                net.minecraft.world.entity.boss.wither.WitherBoss wither,
                                net.minecraft.world.entity.boss.enderdragon.EndCrystal c) {
        if (c == null || c.isRemoved()) return false;
        if (!java.util.Objects.equals(c.level().dimension(), wither.level().dimension())) return false;
        if (c.distanceToSqr(wither) > RANGE * RANGE) return false;
        if (reservedByDragon(level, c)) return false;
        UUID occ = CRYSTAL_TO_WITHER.get(c.getUUID());
        return occ == null || occ.equals(wither.getUUID());
    }
}