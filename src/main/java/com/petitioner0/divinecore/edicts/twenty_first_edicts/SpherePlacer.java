package com.petitioner0.divinecore.edicts.twenty_first_edicts;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public final class SpherePlacer {

    private SpherePlacer() {}

    public static void placeSolidMixedSphere(LevelAccessor level,
                                             BlockPos center,
                                             int radius,
                                             BlockState mixA,
                                             BlockState mixB,
                                             BlockState centerState,
                                             double chanceA,
                                             RandomSource random) {
        if (radius < 0) return;
        final int r2 = radius * radius;

        // Place the center first
        setBlockSafe(level, center, centerState);

        // Iterate over the cubic bounding box containing the sphere
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dy = -radius; dy <= radius; dy++) {
                for (int dz = -radius; dz <= radius; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue; // The center has been placed
                    if (dx * dx + dy * dy + dz * dz <= r2) {
                        BlockPos p = center.offset(dx, dy, dz);

                        BlockState chosen = (random.nextDouble() < chanceA) ? mixA : mixB;

                        setBlockSafe(level, p, chosen);
                    }
                }
            }
        }
    }

    public static void placeSolidMixedSphere(LevelAccessor level,
                                             BlockPos center,
                                             BlockState mixA,
                                             BlockState mixB,
                                             BlockState centerState) {
        placeSolidMixedSphere(level, center, 3, mixA, mixB, centerState, 0.5, level.getRandom());
    }

    private static void setBlockSafe(LevelAccessor level, BlockPos pos, BlockState state) {
        level.setBlock(pos, state, Block.UPDATE_ALL);
    }
}