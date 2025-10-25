package com.petitioner0.divinecore.edicts.third_edicts;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.resources.ResourceLocation;

public final class HealthData {
    private HealthData() {
    }

    // Fixed ResourceLocation: used to identify/replace our health limit penalty Modifier
    private static final ResourceLocation PENALTY_ID = ResourceLocation.fromNamespaceAndPath("divinecore",
            "health_penalty");

    private static final String TAG_ROOT = "divinecore";
    private static final String TAG_PENALTY = "health_penalty"; // The amount of blood that has been deducted

    // Minimum maximum health: not less than 1.0
    private static final double MIN_MAX_HEALTH = 1.0D;

    /** Get current accumulated penalty*/
    public static int getPenalty(Player player) {
        CompoundTag nbt = player.getPersistentData();
        CompoundTag root = nbt.getCompound(TAG_ROOT);
        return root.getInt(TAG_PENALTY);
    }

    /** Add one penalty and apply it immediately to the attribute. */
    public static void addPenaltyAndApply(ServerPlayer player) {
        int penalty = getPenalty(player) + 1;
        setPenalty(player, penalty);
        applyPenaltyModifier(player, penalty, true);
    }

    /** Set the penalty value */
    public static void setPenalty(Player player, int penalty) {
        CompoundTag nbt = player.getPersistentData();
        CompoundTag root = nbt.getCompound(TAG_ROOT);
        root.putInt(TAG_PENALTY, Math.max(0, penalty));
        nbt.put(TAG_ROOT, root);
    }

    public static void reapply(ServerPlayer player) {
        applyPenaltyModifier(player, getPenalty(player), false);
    }

    private static void applyPenaltyModifier(ServerPlayer player, int penalty, boolean clampHealthNow) {
        AttributeInstance inst = player.getAttribute(Attributes.MAX_HEALTH);
        if (inst == null) return;
    
        AttributeModifier old = inst.getModifier(PENALTY_ID);
        if (old != null) inst.removeModifier(old);
    
        double base = inst.getBaseValue();
        double computed = base - penalty;
    
        if (computed < MIN_MAX_HEALTH) {
            penalty = (int) Math.floor(base - MIN_MAX_HEALTH);
            if (penalty < 0) penalty = 0;
            setPenalty(player, penalty); 
        }
    
        if (penalty > 0) {
            AttributeModifier mod = new AttributeModifier(
                PENALTY_ID,
                -penalty,
                AttributeModifier.Operation.ADD_VALUE);
            inst.addPermanentModifier(mod);
        }
    
        if (clampHealthNow) {
            double maxNow = inst.getValue();
            if (player.getHealth() > (float) maxNow) {
                player.setHealth((float) maxNow);
            }
        }
    }
}