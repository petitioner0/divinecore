package com.petitioner0.divinecore.edicts.fifteenth_edicts;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import javax.annotation.Nonnull;

public class BloodMoonHorseAttackGoal extends BloodMoonAttackGoal {

    private final AbstractHorse horse;

    public BloodMoonHorseAttackGoal(AbstractHorse horse, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(horse, speedModifier, followingTargetEvenIfNotSeen);
        this.horse = horse;
    }

    @Override
    protected void checkAndPerformAttack(@Nonnull LivingEntity target) {
        super.checkAndPerformAttack(target);

        // When the attack actually hits, call the horse's action
        if (this.isTimeToAttack()) {
            if (this.horse.doHurtTarget(target)) {
                this.horse.makeMad();
            }
        }
    }
}