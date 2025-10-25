package com.petitioner0.divinecore.edicts.eleventh_edicts;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.level.levelgen.Heightmap;

public final class WitherSpawner {
    private WitherSpawner() {}

    /** On the surface near (0,0) at the top, randomly generate two withers */
    public static void spawnTwoWithersNearOriginTop(ServerLevel endLevel) {
        RandomSource rnd = endLevel.getRandom();

        for (int i = 0; i < 2; i++) {
            int dx = rnd.nextIntBetweenInclusive(-3, 3);
            int dz = rnd.nextIntBetweenInclusive(-3, 3);


            int topY = endLevel.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, dx, dz);
            int dy = rnd.nextIntBetweenInclusive(0, 5);

            BlockPos spawnPos = new BlockPos(dx, topY + dy, dz);

            WitherBoss wither = EntityType.WITHER.create(endLevel);
            if (wither != null) {
                wither.moveTo(spawnPos.getX() + 0.5, spawnPos.getY() + 0.1, spawnPos.getZ() + 0.5,
                        rnd.nextFloat() * 360.0F, 0.0F);
                endLevel.addFreshEntity(wither);
            }
        }
    }
}