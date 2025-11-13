package com.petitioner0.divinecore.edicts.eighth_edicts;

import com.petitioner0.divinecore.FTBHelper;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.BeaconBlock;
import net.minecraft.world.level.block.entity.BeaconBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import java.util.*;

import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.blocks.ModBlocks;
import net.neoforged.fml.common.EventBusSubscriber;

@EventBusSubscriber(modid = DivineCore.MODID)
public class EighthEdicts {
    // Blacklist: blocks that don't want to be randomly replaced
    private static final Set<Block> BLACKLIST = Set.of(
            Blocks.AIR,
            Blocks.CAVE_AIR,
            Blocks.VOID_AIR,
            Blocks.BARRIER,
            Blocks.LIGHT,
            Blocks.STRUCTURE_BLOCK,
            Blocks.JIGSAW,
            Blocks.MOVING_PISTON,
            Blocks.END_PORTAL,
            Blocks.NETHER_PORTAL,
            Blocks.END_GATEWAY,
            Blocks.COMMAND_BLOCK,
            Blocks.REPEATING_COMMAND_BLOCK,
            Blocks.CHAIN_COMMAND_BLOCK,
            Blocks.SPAWNER,
            Blocks.STRUCTURE_VOID,
            Blocks.CONDUIT);

    // Pre-built pool: all vanilla blocks - blacklist
    private static final List<Block> VANILLA_POOL = buildVanillaPool();

    private static List<Block> buildVanillaPool() {
        List<Block> list = new ArrayList<>();
        for (Block b : BuiltInRegistries.BLOCK) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKey(b);
            if (id != null && "minecraft".equals(id.getNamespace()) && !BLACKLIST.contains(b)) {
                list.add(b);
            }
        }
        return list;
    }

    @SubscribeEvent
    public static void onLightning(EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (!(entity instanceof LightningBolt lightning))
            return;
        if (!(event.getLevel() instanceof ServerLevel level))
            return;


        // 1) Try the original lightning position
        BlockPos basePos = lightning.blockPosition();
        BlockPos strikePos = basePos;
        BlockState struck = level.getBlockState(strikePos);

        AABB range = new AABB(strikePos).inflate(16.0D);
        List<ServerPlayer> players = level.getEntitiesOfClass(ServerPlayer.class, range);

        for (ServerPlayer player : players) {
            FTBHelper.completeTask(player, "4F78D75E43AB5598");
        }

        // 2) If it's air, try the block below
        if (struck.isAir()) {
            BlockPos below = basePos.below();
            struck = level.getBlockState(below);
            if (struck.isAir()) return;
            strikePos = below;
        }

        // 3) Check and process the fully activated beacon
        if (struck.getBlock() instanceof BeaconBlock) {

            for (ServerPlayer player : players) {
                FTBHelper.completeTask(player, "7E8CCC4FED40A291");
            }

            if (isBeaconFullyActivated(level, strikePos)) {
                destroyBeaconAndSetBlock(level, strikePos);
                return;
            }
        }

        // 4) Randomly replace with a block from the vanilla block pool
        RandomSource rng = level.getRandom();
        if (VANILLA_POOL.isEmpty())
            return;

        Block pick = VANILLA_POOL.get(rng.nextInt(VANILLA_POOL.size()));
        BlockState candidate = pick.defaultBlockState();

        BlockUtils.safeSetBlockNormal(level, strikePos, candidate);
    }

    private static boolean isBeaconFullyActivated(Level level, BlockPos pos) {
        int levels = getBeaconLevels(level, pos);
        // 满级信标有4层金字塔结构，且等级为4表示已激活
        return levels == 4;
    }
    private static int getBeaconLevels(Level level, BlockPos pos) {
        BlockEntity be = level.getBlockEntity(pos);
        if (be instanceof BeaconBlockEntity beacon) {
            CompoundTag tag = beacon.getUpdateTag(level.registryAccess());
            return tag.getInt("Levels"); // 若未激活则为 0
        }
        return 0;
    }

    private static void destroyBeaconAndSetBlock(ServerLevel level, BlockPos pos) {
        level.removeBlockEntity(pos);
        level.setBlock(pos, ModBlocks.CINDER_OF_MIGHT_BLOCK.get().defaultBlockState(), 3);
    }
}