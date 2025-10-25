package com.petitioner0.divinecore.edicts.fifth_edicts;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerPlayer;

public final class TrackedBrew {
    public final ServerPlayer player;
    public final ResourceKey<Level> dimension;
    public final BlockPos pos;

    public int lastBrewTime = 0;
    public boolean brewingStarted = false;

    public TrackedBrew(ServerPlayer player, ResourceKey<Level> dimension, BlockPos pos) {
        this.player = player;
        this.dimension = dimension;
        this.pos = pos.immutable();
    }
}