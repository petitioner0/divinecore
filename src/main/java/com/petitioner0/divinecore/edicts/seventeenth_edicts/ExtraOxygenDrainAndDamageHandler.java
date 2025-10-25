package com.petitioner0.divinecore.edicts.seventeenth_edicts;

import net.minecraft.core.BlockPos;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.EntityTickEvent;
import com.petitioner0.divinecore.DivineCore;

import java.util.Map;
import java.util.WeakHashMap;

@EventBusSubscriber(modid = DivineCore.MODID)
public class ExtraOxygenDrainAndDamageHandler {

    private static final int    EXTRA_AIR_DRAIN_BASE   = 0;      // Additional air drain points per vanilla air loss
    private static final double EXTRA_AIR_DRAIN_PER_M  = 0.25;   // Additional air drain points per 1 block of water column


    private static final int WATER_SCAN_CAP = 128; 
    private static final int TICK_STRIDE    = 1;   

    private static final Map<Player, Integer> lastAir = new WeakHashMap<>();

    @SubscribeEvent
    public static void onLivingTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (player.isSpectator() || player.isCreative()) return;

        Level level = player.level();
        if (level.isClientSide) return;
        if ((player.tickCount % TICK_STRIDE) != 0) return;

        int prev = lastAir.getOrDefault(player, player.getAirSupply());
        int now  = player.getAirSupply();

        double waterColumn = 0.0D;
        if (player.isUnderWater()) {
            waterColumn = computeWaterColumnAbove(level, player);
        }

        if (now < prev) {
            if (player.isUnderWater()) {
                if (!player.hasEffect(MobEffects.WATER_BREATHING) && !player.hasEffect(MobEffects.CONDUIT_POWER)) {
                    int extra = (int)Math.ceil(EXTRA_AIR_DRAIN_BASE + waterColumn * EXTRA_AIR_DRAIN_PER_M);

                    player.setAirSupply(now - Math.max(0, extra));
                    now = player.getAirSupply();
                }
            }
        }

        lastAir.put(player, now);

    }

    @SubscribeEvent
    public static void onDrownDamage(net.neoforged.neoforge.event.entity.living.LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof Player player)) return;
        if (!event.getSource().is(net.minecraft.tags.DamageTypeTags.IS_DROWNING)) return; 

        int depth = computeWaterDepth(player);
        float extra = Math.min(6.0F, 0.5F + depth * 0.05F);

        var nbt = player.getPersistentData();
        if (nbt.getBoolean("deep_pressure$drowning_extra")) return;
        nbt.putBoolean("deep_pressure$drowning_extra", true);
        try {
            player.hurt(player.damageSources().drown(), extra);
        } finally {
            nbt.remove("deep_pressure$drowning_extra");
        }
    }



    private static int computeWaterDepth(Player player) {
        Level level = player.level();
        if (!player.isUnderWater()) return 0;
        
        double waterColumn = computeWaterColumnAbove(level, player);
        return (int) Math.floor(waterColumn);
    }

    private static double computeWaterColumnAbove(Level level, Player player) {
        if (!player.isUnderWater()) return 0.0D;

        Vec3 eye = new Vec3(player.getX(), player.getEyeY(), player.getZ());
        int x = Mth.floor(eye.x);
        int y = Mth.floor(eye.y);
        int z = Mth.floor(eye.z);

        double partial = (y + 1) - eye.y;

        BlockPos pos = new BlockPos(x, y, z);
        FluidState fs = level.getFluidState(pos);
        if (!fs.is(FluidTags.WATER)) return 0.0D;

        double total = partial;

        int maxY = level.getMaxBuildHeight();
        int scanned = 0;
        BlockPos.MutableBlockPos up = new BlockPos.MutableBlockPos(x, y + 1, z);
        for (int yy = y + 1; yy < maxY && scanned < WATER_SCAN_CAP; yy++, scanned++) {
            up.setY(yy);
            FluidState upFs = level.getFluidState(up);
            if (upFs.is(FluidTags.WATER)) {
                total += 1.0D;
            } else if (upFs.isEmpty()) {
                break;
            }
        }
        return total;
    }
}
