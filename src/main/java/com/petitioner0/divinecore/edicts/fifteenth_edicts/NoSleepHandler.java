package com.petitioner0.divinecore.edicts.fifteenth_edicts;


import com.petitioner0.divinecore.DivineCore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.BlockPosTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.ai.memory.WalkTarget;
import net.minecraft.world.entity.ai.navigation.GroundPathNavigation;
import net.minecraft.world.entity.npc.Villager;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

@EventBusSubscriber(modid = DivineCore.MODID)
public class NoSleepHandler {
    //control the villager's behavior during the blood moon window
    @SubscribeEvent
    public static void onVillagerTick(EntityTickEvent.Pre e) {
        if (!(e.getEntity() instanceof Villager villager)) return;
        if (villager.level().isClientSide()) return;

        ServerLevel level = (ServerLevel) villager.level();
        Brain<Villager> brain = villager.getBrain();

        boolean bloodMoonBlocked = BloodMoonEvents.isBloodMoonWindow(level);

        if (bloodMoonBlocked) {
            if (villager.isSleeping()) villager.stopSleeping();
        
            brain.eraseMemory(MemoryModuleType.NEAREST_BED);
            brain.eraseMemory(MemoryModuleType.WALK_TARGET);
            brain.eraseMemory(MemoryModuleType.PATH);
            brain.eraseMemory(MemoryModuleType.DOORS_TO_CLOSE);
        
            brain.eraseMemory(MemoryModuleType.CANT_REACH_WALK_TARGET_SINCE);

            BlockPos above = villager.blockPosition().above(10);
            brain.setMemory(MemoryModuleType.LOOK_TARGET, new BlockPosTracker(above));
        
            // Important: make them walk to the town center
            brain.getMemory(MemoryModuleType.MEETING_POINT).ifPresent(globalPos -> {
                BlockPos pos = globalPos.pos();
                brain.setMemory(MemoryModuleType.WALK_TARGET, new WalkTarget(pos, 1.0f, 0));
            });
        
            brain.updateActivityFromSchedule(level.getDayTime(), level.getGameTime());
        
            if (villager.getNavigation() instanceof GroundPathNavigation nav) {
                nav.setCanOpenDoors(true);
            }
        }
    }
}