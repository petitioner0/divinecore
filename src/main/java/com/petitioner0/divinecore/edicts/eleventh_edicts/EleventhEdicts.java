package com.petitioner0.divinecore.edicts.eleventh_edicts;

import com.petitioner0.divinecore.items.ItemHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.petitioner0.divinecore.DivineCore;

@EventBusSubscriber(modid = DivineCore.MODID)
public class EleventhEdicts {

    private static final String TAG_PATCHED = "dc_wither_player_target_patched";

    /** When generating/entering a cross-dimensional: change the nearest target to "only players" (only in the End) */
    @SubscribeEvent
    public static void onJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof WitherBoss wither)) return;
        if (wither.level().dimension() != Level.END) return; // Only in the End
        var tag = wither.getPersistentData();
        if (tag.getBoolean(TAG_PATCHED)) return;

        wither.targetSelector.getAvailableGoals().removeIf(w ->
                w.getGoal() instanceof NearestAttackableTargetGoal<?>);

        wither.targetSelector.addGoal(2,
                new NearestAttackableTargetGoal<>(wither, Player.class, 10, true, false,
                        p -> p instanceof Player pl && !pl.isSpectator() && !pl.isCreative()));
        wither.goalSelector.addGoal(1, new WitherHealFromCrystalGoal(wither));

        tag.putBoolean(TAG_PATCHED, true);
    }

    @SubscribeEvent
    public static void onEntityTick(net.neoforged.neoforge.event.tick.EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof WitherBoss wither)) return;
        if (wither.level().isClientSide) return; 
        if (wither.level().dimension() != Level.END) return; 

        LivingEntity t = wither.getTarget();
        if (t != null && !(t instanceof Player)) {
            Player nearest = wither.level().getNearestPlayer(wither, 64.0);
            wither.setTarget(nearest); 
        }

        if (wither.level() instanceof ServerLevel sl) {
            for (int i = 0; i < 3; i++) {
                int id = wither.getAlternativeTarget(i);
                if (id <= 0) continue;
                Entity e = sl.getEntity(id);
                if (!(e instanceof Player)) {
                    wither.setAlternativeTarget(i, 0);
                }
            }
        }
    }

    /** When the wither dies, drop reward item (only in the End and has a specific tag and the ender dragon is alive) */
    @SubscribeEvent
    public static void onWitherDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof WitherBoss wither)) return;
        if (wither.level().isClientSide) return; 
        if (wither.level().dimension() != Level.END) return; 
        
        // Check if the wither has our tag
        var tag = wither.getPersistentData();
        if (!tag.getBoolean(TAG_PATCHED)) return;
        
        // Drop reward item
        ItemHelper.dropItemAt(wither.level(), 
            new net.minecraft.world.phys.Vec3(wither.getX(), wither.getY(), wither.getZ()), 
            "netherwyrm_covenant_core", 1);
    }
}