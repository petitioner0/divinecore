package com.petitioner0.divinecore.edicts.fifteenth_edicts;

import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.horse.AbstractHorse;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.EntityAttributeModificationEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.player.CanPlayerSleepEvent;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.Set;

import com.petitioner0.divinecore.DivineCore;

@EventBusSubscriber(modid = DivineCore.MODID) 
public class BloodMoonEvents {

    private static final String NBT_KEY_BUFFED = "divinecore:buffed_once";
    
    private static final int START_TICK = 11000;
    private static final int END_TICK = 23000;

    private static final Set<EntityType<?>> NORMAL_ENTITY = Set.of(
            EntityType.COW, EntityType.SHEEP, EntityType.PIG, EntityType.RABBIT, EntityType.FOX, EntityType.CHICKEN, EntityType.OCELOT, EntityType.CAT, EntityType.IRON_GOLEM, EntityType.WANDERING_TRADER
    );
    
    public static boolean isBloodMoonWindow(Level level) {
        if (level == null) return false;
        if (level.dimension() != Level.OVERWORLD) return false;
    
        long day = level.getDayTime() / 24000L;
        boolean fullMoon = level.getMoonPhase() == 0; // 满月
        long time = level.getDayTime() % 24000L;
        boolean inWindow = time >= START_TICK && time <= END_TICK;
    
        // Skip the first full moon (day==8)
        if (fullMoon && day == 8 || day == 1 || day == 0) {
            return false;
        }
    
        return fullMoon && inWindow;
    }
    

    /** When a monster is spawned, double the attack power and movement speed (only process monsters, and only once) */
    @SubscribeEvent
    public static void onMonsterJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;

        if (!(event.getEntity() instanceof Monster monster)) return;
        // Only trigger during the blood moon
        if (!isBloodMoonWindow(monster.level())) return;

        // Avoid duplicate processing (e.g. due to chunk unload/load causing re-joining the world)
        if (monster.getPersistentData().getBoolean(NBT_KEY_BUFFED)) return;

        // Double the attack power
        AttributeInstance atk = monster.getAttribute(Attributes.ATTACK_DAMAGE);
        if (atk != null) {
            atk.setBaseValue(atk.getBaseValue() * 2.0D);
        }

        // Double the movement speed
        AttributeInstance ms = monster.getAttribute(Attributes.MOVEMENT_SPEED);
        if (ms != null) {
            ms.setBaseValue(ms.getBaseValue() * 2.0D);
        }
        monster.getPersistentData().putBoolean(NBT_KEY_BUFFED, true);
    }

    @SubscribeEvent
    public static void onOtherEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!(event.getLevel() instanceof ServerLevel)) return;
        if (event.loadedFromDisk()) return;

        Entity entity = event.getEntity();
        if (!(entity instanceof Mob mob)) return;
        CompoundTag data = mob.getPersistentData();
        if (data.getBoolean("divinecore:ai_injected")) return;
        data.putBoolean("divinecore:ai_injected", true);

        if (mob instanceof AbstractHorse horse) {
            horse.targetSelector.getAvailableGoals().removeIf(w ->
                w.getGoal() instanceof NearestAttackableTargetGoal<?>);

            horse.targetSelector.addGoal(0,
                new BloodMoonTargetGoal(horse, Player.class, 16, false, false,
                        p -> p instanceof Player pl && !pl.isSpectator() && !pl.isCreative()));

            horse.goalSelector.addGoal(0, new BloodMoonHorseAttackGoal(horse, 1.2D, false));
            
            AttributeInstance follow = mob.getAttribute(Attributes.FOLLOW_RANGE);
            if (follow != null && follow.getBaseValue() < 32.0D) {
                follow.setBaseValue(32.0D);
            }
        }
        if (NORMAL_ENTITY.contains(mob.getType()) && mob instanceof PathfinderMob pathfinderMob) {
            mob.targetSelector.addGoal(0, new BloodMoonTargetGoal(mob, Player.class, 16, false, false,
                        p -> p instanceof Player pl && !pl.isSpectator() && !pl.isCreative()));

            mob.goalSelector.addGoal(0, new BloodMoonAttackGoal(pathfinderMob, 1.2D, false));
            
            AttributeInstance follow = mob.getAttribute(Attributes.FOLLOW_RANGE);
            if (follow != null && follow.getBaseValue() < 32.0D) {
                follow.setBaseValue(32.0D);
            }
        }
        if (mob instanceof Wolf wolf) {
            mob.goalSelector.addGoal(0, new BloodMoonWolfAttackGoal(wolf, 1.2D, false));
            mob.targetSelector.addGoal(0, new BloodMoonTargetGoal(mob, Player.class, 16, false, false,
                        p -> p instanceof Player pl && !pl.isSpectator() && !pl.isCreative()));
            
            AttributeInstance follow = mob.getAttribute(Attributes.FOLLOW_RANGE);
            if (follow != null && follow.getBaseValue() < 32.0D) {
                follow.setBaseValue(32.0D);
            }
        }
    }

    /** When hurt, if the damage source is a creature, heal half of the damage taken by the source */
    @SubscribeEvent
    public static void onLivingHurt(LivingDamageEvent.Post event) {
        if (!isBloodMoonWindow(event.getEntity().level())) return;
        
        Entity directOrOwner = event.getSource().getEntity();

        // If the direct source is null (e.g. environmental damage), try to get from the indirect source (e.g. the owner of the projectile)
        if (directOrOwner == null) {
            directOrOwner = event.getSource().getDirectEntity();
        }

        // Only heal from creatures (players/monsters/animals etc.)
        if (directOrOwner instanceof LivingEntity attacker) {
            float damage = event.getNewDamage(); 
            if (damage > 0f && attacker.isAlive()) {
                attacker.heal(damage * 0.5f);
            }
        }
    }
    
    /**
     * Prevent players from sleeping during the blood moon
     */
    @SubscribeEvent
    public static void onCanPlayerSleep(CanPlayerSleepEvent event) {
        if (!isBloodMoonWindow(event.getEntity().level())) return;

        event.setProblem(Player.BedSleepingProblem.OTHER_PROBLEM);

        event.getEntity().displayClientMessage(
            Component.translatable("message.divinecore.bloodmoon_sleep_blocked"), true
        );
    }
    @SubscribeEvent
    public static void onEntityAttributeModification(EntityAttributeModificationEvent event) {
        add(event, EntityType.COW);
        add(event, EntityType.SHEEP);
        add(event, EntityType.PIG);
        add(event, EntityType.RABBIT);
        add(event, EntityType.FOX);
        add(event, EntityType.CHICKEN);
        add(event, EntityType.OCELOT);
        add(event, EntityType.CAT);
        add(event, EntityType.IRON_GOLEM);
        add(event, EntityType.WANDERING_TRADER);
        add(event, EntityType.WOLF);
        add(event, EntityType.HORSE);
        add(event, EntityType.DONKEY);
        add(event, EntityType.ZOMBIE_HORSE);
        add(event, EntityType.SKELETON_HORSE);
        add(event, EntityType.MULE);
    }

    private static void add(EntityAttributeModificationEvent event,
                            EntityType<? extends LivingEntity> type) {
        if (!event.has(type, Attributes.ATTACK_DAMAGE)) {
            event.add(type, Attributes.ATTACK_DAMAGE, 2.0D);
        }
        if (!event.has(type, Attributes.FOLLOW_RANGE)) {
            event.add(type, Attributes.FOLLOW_RANGE, 32.0D);
        }
    }
        
}