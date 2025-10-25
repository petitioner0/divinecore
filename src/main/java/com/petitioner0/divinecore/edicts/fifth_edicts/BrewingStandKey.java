package com.petitioner0.divinecore.edicts.fifth_edicts;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Objects;

/**
 * The unique identifier for the brewing stand, used as a key in the Map
 */
public final class BrewingStandKey {
    public final ResourceKey<Level> dimension;
    public final BlockPos pos;

    public BrewingStandKey(ResourceKey<Level> dimension, BlockPos pos) {
        this.dimension = dimension;
        this.pos = pos.immutable();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!(obj instanceof BrewingStandKey other))
            return false;
        return Objects.equals(dimension, other.dimension) && Objects.equals(pos, other.pos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(dimension, pos);
    }

    @Override
    public String toString() {
        return "BrewingStandKey{dimension=" + dimension + ", pos=" + pos + "}";
    }
}
