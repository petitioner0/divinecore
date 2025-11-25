package com.petitioner0.divinecore.edicts.first_edicts;

import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

public class DreamWorldCache {

    /** 缓存玩家跨维度时的 3×3 chunk 方块数据 */
    public static final Map<UUID, List<BlockState>> BLOCK_CACHE = new HashMap<>();

}