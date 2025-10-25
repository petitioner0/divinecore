package com.petitioner0.divinecore.edicts.fifteenth_edicts;

import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.items.ItemHelper;

import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.IronGolem;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@EventBusSubscriber(modid = DivineCore.MODID)
public class FifteenthEdicts {

    private static final class Flags {
        boolean villager;        // A branch condition: whether to kill a villager
        boolean golem;           // A branch condition: whether to kill a golem
        boolean readyA;          // A branch: condition met, waiting for night end结算
        boolean dealtDamage;     // B branch: whether to deal any damage to any entity in this night
        boolean tookDamage;      // B branch: whether to take any damage in this night
        boolean triggered;       // Already triggered at night end (to prevent repetition)

        Flags() { }
    }

    private static final Map<UUID, Flags> PROGRESS = new ConcurrentHashMap<>();

    //The previous tick's blood moon window state (for detecting the end of the blood moon window)
    private static final Map<ResourceKey<Level>, Boolean> LAST_BLOOD_MOON_STATE = new ConcurrentHashMap<>();



    /** A branch:击杀统计 during the blood moon window */
    @SubscribeEvent
    public static void onEntityDeath(LivingDeathEvent event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD) return;
        if (!BloodMoonEvents.isBloodMoonWindow(level)) return;

        // Determine the killer (including the projectile owner)
        Entity src = event.getSource().getEntity();
        ServerPlayer killer = null;
        if (src instanceof ServerPlayer sp) {
            killer = sp;
        } else if (src instanceof Projectile p && p.getOwner() instanceof ServerPlayer spOwner) {
            killer = spOwner;
        }
        if (killer == null) return;

        // Only count villagers/golems
        Entity victim = event.getEntity();
        boolean isVillager = victim.getType() == EntityType.VILLAGER || victim instanceof Villager;
        boolean isGolem    = victim.getType() == EntityType.IRON_GOLEM || victim instanceof IronGolem;
        if (!isVillager && !isGolem) return;

        Flags flags = getOrInitFlagsForPlayer(killer);

        if (isVillager) flags.villager = true;
        if (isGolem)    flags.golem    = true;
        if (flags.villager && flags.golem) {
            flags.readyA = true; 
        }
    }

    @SubscribeEvent
    public static void onLivingHurt(LivingDamageEvent.Post event) {
        if (!(event.getEntity().level() instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD) return;
        
        // Only record damage during the blood moon window, for B branch judgment
        if (!BloodMoonEvents.isBloodMoonWindow(level)) return;

        // The victim is a player → record tookDamage
        if (event.getEntity() instanceof ServerPlayer victim) {
            Flags vf = getOrInitFlagsForPlayer(victim);
            vf.tookDamage = true;
        }

        // The damage source is a player (or its projectile) → record dealtDamage
        Entity src = event.getSource().getEntity();
        ServerPlayer damager = null;
        if (src instanceof ServerPlayer sp) {
            damager = sp;
        } else if (src instanceof Projectile p && p.getOwner() instanceof ServerPlayer spOwner) {
            damager = spOwner;
        }
        if (damager != null) {
            Flags df = getOrInitFlagsForPlayer(damager);
            df.dealtDamage = true;
        }
    }

    private static Flags getOrInitFlagsForPlayer(ServerPlayer player) {
        return PROGRESS.computeIfAbsent(player.getUUID(), uuid -> new Flags());
    }

    @SubscribeEvent
    public static void onServerTick(ServerTickEvent.Post event) {
        // Iterate through all dimensions, check the blood moon window state change
        for (ServerLevel level : event.getServer().getAllLevels()) {
            if (level.dimension() != Level.OVERWORLD) continue;

            ResourceKey<Level> dim = level.dimension();

            // Check the blood moon window state change
            boolean currentBloodMoonState = BloodMoonEvents.isBloodMoonWindow(level);
            Boolean lastBloodMoonState = LAST_BLOOD_MOON_STATE.put(dim, currentBloodMoonState);

            // Check the end of the blood moon window: the previous tick is true, the current tick is false
            if (lastBloodMoonState != null && lastBloodMoonState && !currentBloodMoonState) {
                finalizeNight(level);
            }
        }
    }


    private static void finalizeNight(ServerLevel level) {
        for (ServerPlayer player : level.getServer().getPlayerList().getPlayers()) {
            // Get or create the player's Flags (if not exist, create a default Flags with all false)
            Flags flags = PROGRESS.computeIfAbsent(player.getUUID(), uuid -> new Flags());
            
            if (flags.triggered) continue;

            if (flags.readyA) {
                // A branch: blood moon window sacrifice successful (villager+golem)
                onBranchA_NightEnd(player, level);
                flags.triggered = true;
            } else if (!flags.dealtDamage && !flags.tookDamage) {
                // B branch: no damage or took damage all night
                onBranchB_NightEnd(player, level);
                flags.triggered = true;
            }
        }
        
        // After the settlement, clear all player data
        PROGRESS.clear();
    }


    private static void onBranchA_NightEnd(ServerPlayer player, ServerLevel level) {
        ItemHelper.giveItemToPlayer(player, "mimic_malediction", 1);
    }


    private static void onBranchB_NightEnd(ServerPlayer player, ServerLevel level) {
        ItemHelper.giveItemToPlayer(player, "crimson_bloodcrystal", 1);
    }
}