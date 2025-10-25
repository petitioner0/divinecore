package com.petitioner0.divinecore.edicts.eighth_edicts;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class BlockUtils {
    private BlockUtils() {
    }


    public static boolean safeSetBlock(ServerLevel level, BlockPos pos, BlockState newState, int flags) {
        if (level.isClientSide)
            return false;

        BlockState old = level.getBlockState(pos);

        if (old.hasBlockEntity() && !newState.hasBlockEntity()) {
            level.removeBlockEntity(pos);
        }

        boolean placed = level.setBlock(pos, newState, flags);
        if (!placed)
            return false;

        if (newState.hasBlockEntity() && newState.getBlock() instanceof EntityBlock entityBlock) {
            BlockEntity existing = level.getBlockEntity(pos);

            BlockEntity expected = entityBlock.newBlockEntity(pos, newState);

            if (expected != null) {
                boolean needReplace = (existing == null) ||
                        (existing.getType() != expected.getType());

                if (needReplace) {
                    if (existing != null) {
                        level.removeBlockEntity(pos);
                    }
                    level.setBlockEntity(expected);
                    expected.setChanged();
                }
            }
        }

        return true;
    }

    // Normal: notify neighbors + sync client
    public static boolean safeSetBlockNormal(ServerLevel level, BlockPos pos, BlockState newState) {
        final int flags = Block.UPDATE_ALL; 
        return safeSetBlock(level, pos, newState, flags);
    }
}