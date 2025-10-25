package com.petitioner0.divinecore.edicts.fifteenth_edicts;

import com.petitioner0.divinecore.DivineCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;

public class BloodMoonAttackGoal extends MeleeAttackGoal {

    private final PathfinderMob mob;
    private static final ResourceLocation BLOODMOON_AD_ID =
        ResourceLocation.fromNamespaceAndPath(DivineCore.MODID, "bloodmoon_attack_bonus");

    // Fixed attack bonus during blood moon
    private static final double BLOODMOON_ATTACK_BONUS = 4.5D;

    public BloodMoonAttackGoal(PathfinderMob mob, double speedModifier, boolean followingTargetEvenIfNotSeen) {
        super(mob, speedModifier, followingTargetEvenIfNotSeen);
        this.mob = mob;
    }

    @Override
    public boolean canUse() {
           return super.canUse() && BloodMoonEvents.isBloodMoonWindow(mob.level());
    }
    
    @Override
    public boolean canContinueToUse() {
        return super.canContinueToUse() && BloodMoonEvents.isBloodMoonWindow(mob.level());
    }

    @Override
    public void start() {
        if (BloodMoonEvents.isBloodMoonWindow(mob.level())) {
            var atk = mob.getAttribute(Attributes.ATTACK_DAMAGE);
            if (atk != null) {
                atk.removeModifier(BLOODMOON_AD_ID);

                var modifier = new AttributeModifier(
                    BLOODMOON_AD_ID,
                    BLOODMOON_ATTACK_BONUS,
                    AttributeModifier.Operation.ADD_VALUE
                );
                atk.addTransientModifier(modifier);
            }
        }
        super.start();
    }

    @Override
    public void stop() {
        var atk = mob.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) {
            atk.removeModifier(BLOODMOON_AD_ID);
        }
        super.stop();
    }
}