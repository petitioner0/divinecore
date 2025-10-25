package com.petitioner0.divinecore.edicts.twenty_first_edicts;

import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import static com.petitioner0.divinecore.blocks.ModBlockEntities.ANTIENTROPY_CATALYST_BE;

import com.petitioner0.divinecore.blocks.ModBlocks;

public class AntientropyCatalystBlockEntity extends BlockEntity {

    private static final double BASE_TRY_CHANCE = 1.0 / 480.0;
    private static final double NON_AIR_PENALTY = 0.25; 
    private static final int RADIUS = 3;
    private static final int MAX_SAMPLE_RETRY = 24;

    // Record the positions that have been tried in the current attempt (to avoid repeating the same position in a single attempt)
    private java.util.Set<BlockPos> triedPositions = new java.util.HashSet<>();

    public AntientropyCatalystBlockEntity(BlockPos pos, BlockState state) {
        super(ANTIENTROPY_CATALYST_BE.get(), pos, state);
    }

    public void serverTick() {
        Level level = getLevel();
        if (level == null || level.isClientSide) return;

        RandomSource random = level.random;

        if (!randomChance(random, BASE_TRY_CHANCE)) return;

        triedPositions.clear();

        // Randomly select a point in the sphere of radius RADIUS (on the grid)
        BlockPos target = sampleTargetPos(random);
        if (target == null) return; // Didn't get a qualified target

        // Don't replace pyro/cry itself; if you get them, re-draw
        BlockState targetState = level.getBlockState(target);
        if (isForbiddenTarget(targetState)) return; // Theoretically filtered, here is a fallback

        // If the target is not air, an additional probability determination is needed
        if (!targetState.isAir() && !randomChance(random, NON_AIR_PENALTY)) {
            return;
        }

        // Randomly select the block to place (pyro / cryo)
        BlockState toPlace = random.nextBoolean()
                ? ModBlocks.PYROSTATIC_BLOCK.get().defaultBlockState()
                : ModBlocks.CRYOSTATIC_BLOCK.get().defaultBlockState();

        // Final placement
        level.setBlock(target, toPlace, 3);
    }

    private BlockPos sampleTargetPos(RandomSource random) {
        if (level == null) return null;
        BlockPos base = getBlockPos();

        for (int i = 0; i < MAX_SAMPLE_RETRY; i++) {
            int dx = random.nextInt(RADIUS * 2 + 1) - RADIUS;
            int dy = random.nextInt(RADIUS * 2 + 1) - RADIUS;
            int dz = random.nextInt(RADIUS * 2 + 1) - RADIUS;

            if (dx == 0 && dy == 0 && dz == 0) continue;
            if (dx * dx + dy * dy + dz * dz > RADIUS * RADIUS) continue; // 落在球外

            BlockPos candidate = base.offset(dx, dy, dz);

            // Avoid repeating the same position in the current attempt
            if (triedPositions.contains(candidate)) continue;
            triedPositions.add(candidate);

            BlockState st = level.getBlockState(candidate);
            // Don't replace pyro/cry itself
            if (isForbiddenTarget(st)) continue;

            return candidate;
        }
        return null;
    }

    private boolean isForbiddenTarget(BlockState st) {
        return st.is(ModBlocks.PYROSTATIC_BLOCK.get()) || st.is(ModBlocks.CRYOSTATIC_BLOCK.get());
    }

    private static boolean randomChance(RandomSource random, double p) {
        if (p <= 0) return false;
        if (p >= 1) return true;
        return random.nextDouble() < p;
    }

}