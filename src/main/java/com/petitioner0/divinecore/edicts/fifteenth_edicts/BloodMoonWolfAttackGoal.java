package com.petitioner0.divinecore.edicts.fifteenth_edicts;

import net.minecraft.world.entity.animal.Wolf;

public class BloodMoonWolfAttackGoal extends BloodMoonAttackGoal {

    private final Wolf wolf;

    public BloodMoonWolfAttackGoal(Wolf wolf, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(wolf, speedModifier, followingTargetEvenIfNotSeen);
        this.wolf = wolf;
    }

    @Override
    public void tick() {
        wolf.setAggressive(true);
        // Every tick, set the anger time to a large value, like 6000 ticks (5 minutes)
        if (wolf.getRemainingPersistentAngerTime() < 100) {
            wolf.setRemainingPersistentAngerTime(6000);
        }
        super.tick();
    }

    @Override
    public void stop() {
        wolf.setAggressive(false);
        super.stop();
        wolf.setRemainingPersistentAngerTime(0);
    }
    
}
