package com.petitioner0.divinecore.edicts.fifteenth_edicts;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import java.util.function.Predicate;

public class BloodMoonTargetGoal extends NearestAttackableTargetGoal<LivingEntity> {

    @SuppressWarnings("unchecked")
    public BloodMoonTargetGoal(Mob mob, Class<? extends LivingEntity> targetClass, int range, boolean checkVisibility, boolean checkCanNavigate, Predicate<LivingEntity> predicate) {
        super(mob, (Class<LivingEntity>) targetClass, range, checkVisibility, checkCanNavigate, predicate);
    }

    @Override
    public boolean canUse() {
        return super.canUse() && BloodMoonEvents.isBloodMoonWindow(mob.level());
    }

    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse() && BloodMoonEvents.isBloodMoonWindow(mob.level());
    }
}
