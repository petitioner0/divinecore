package com.petitioner0.divinecore.edicts.first_edicts;

import com.petitioner0.divinecore.DivineCore;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class DreamWorldMirrorUtil {

    /** 读取玩家周围 3×3 chunk 的方块数据 */
    public static List<BlockState> read3x3(ServerLevel level, BlockPos center) {
        List<BlockState> list = new ArrayList<>();

        ChunkPos cpos = new ChunkPos(center);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {

                // 确保 chunk 已加载
                level.getChunkSource().getChunk(cpos.x + dx, cpos.z + dz, true);

                for (int y = minY; y < maxY; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {
                            pos.set((cpos.x + dx) * 16 + x, y, (cpos.z + dz) * 16 + z);
                            list.add(level.getBlockState(pos));
                        }
                    }
                }

            }
        }

        return list;
    }

    /** 将缓存写入 dream 维度 */
    public static void write3x3(ServerLevel level, BlockPos center, List<BlockState> data) {
        ChunkPos cpos = new ChunkPos(center);

        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int minY = level.getMinBuildHeight();
        int maxY = level.getMaxBuildHeight();

        int index = 0;

        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {

                level.getChunkSource().getChunk(cpos.x + dx, cpos.z + dz, true);

                for (int y = minY; y < maxY; y++) {
                    for (int z = 0; z < 16; z++) {
                        for (int x = 0; x < 16; x++) {

                            pos.set((cpos.x + dx) * 16 + x, y, (cpos.z + dz) * 16 + z);

                            BlockState original = data.get(index++);

                            level.setBlock(
                                    pos,
                                    original.isAir()
                                            ? Blocks.AIR.defaultBlockState()
                                            : Blocks.GLASS.defaultBlockState(),
                                    2
                            );
                        }
                    }
                }

            }
        }
    }
    public static void clearEntireDreamDimension(ServerLevel level) {
        try {
            // 1. 存档根目录：.minecraft/saves/<world>/
            Path worldDir = level.getServer().getWorldPath(LevelResource.ROOT);

            // 2. 你的梦境维度目录：world/dimensions/divinecore/dream/
            Path dimPath = worldDir
                    .resolve("dimensions")
                    .resolve(DivineCore.MODID)
                    .resolve("dream")
                    .normalize();

            DivineCore.LOGGER.info("准备清空梦境维度目录: {}", dimPath);

            if (!Files.exists(dimPath)) {
                DivineCore.LOGGER.warn("梦境维度目录不存在: {}", dimPath);
                return;
            }

            // 3. 递归删除整个目录（先子文件后父目录）
            Files.walk(dimPath)
                    .sorted(Comparator.reverseOrder())
                    .forEach(p -> {
                        try {
                            Files.deleteIfExists(p);
                        } catch (Exception e) {
                            DivineCore.LOGGER.error("删除路径失败: {}", p, e);
                        }
                    });

            Files.createDirectories(dimPath);
            Files.createDirectories(dimPath.resolve("data"));

            DivineCore.LOGGER.info("梦境维度目录已被完全删除，下次加载将重新生成。");

        } catch (Exception e) {
            DivineCore.LOGGER.error("清空梦境维度时发生错误", e);
        }
    }
}
