package com.petitioner0.divinecore.mixin;

import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.FTBHelper;
import com.petitioner0.divinecore.items.ItemHelper;
import com.petitioner0.divinecore.edicts.fifth_edicts.DivineWorldPotion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Random;

@Mixin(BrewingStandBlockEntity.class)
public abstract class FifthEdicts {

    @Inject(method = "doBrew", at = @At("TAIL"))
    private static void divinecore$onBrewFinished(Level level, BlockPos pos,
                                                  NonNullList<ItemStack> items,
                                                  CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) return;
        MinecraftServer server = serverLevel.getServer();

        // 所有玩家完成基础任务
        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            FTBHelper.completeTask(player, "53C8B1EA7CDBF967");
        }

        // 统计药水瓶数量
        int potionCount = 0;
        for (int i = 0; i < 3; i++) {
            ItemStack s = items.get(i);
            if (s.get(DataComponents.POTION_CONTENTS) != null)
                potionCount++;
        }

        // 判断世界维度
        boolean isNether = serverLevel.dimension().location().toString().equals("minecraft:the_nether");
        boolean shouldExplode = isNether;
        Random random = new Random();

        // 若非下界，有 10% 概率触发爆炸
        if (!isNether) {
            for (int i = 0; i < potionCount; i++) {
                if (random.nextDouble() < 0.1) {
                    shouldExplode = true;
                    break;
                }
            }
        }

        // 下界额外任务
        if (isNether) {
            for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
                FTBHelper.completeTask(player, "675E0209B1642617");
            }
        }

        if (!shouldExplode) return;

        // 检测是否包含被选中的药水（改进版）
        boolean containsChosenPotion = false;
        ResourceLocation chosenId = DivineWorldPotion.getChosenPotionId(serverLevel);
        if (chosenId != null) {
            for (int i = 0; i < 3; i++) {
                ItemStack potionStack = items.get(i);
                var contents = potionStack.get(DataComponents.POTION_CONTENTS);
                if (contents == null) continue;

                var holderOpt = contents.potion();
                if (holderOpt.isEmpty()) continue;

                var holder = holderOpt.get();
                var keyOpt = holder.unwrapKey();

                if (keyOpt.isPresent()) {
                    ResourceLocation potionId = keyOpt.get().location();
                    if (chosenId.equals(potionId)) {
                        containsChosenPotion = true;
                        DivineCore.LOGGER.debug("[DivineCore] 检测到被选中的药水: {}", potionId);
                        break;
                    }
                } else {
                    DivineCore.LOGGER.debug("[DivineCore] 无注册键药水: {}", holder.value());
                }
            }
        } else {
            DivineCore.LOGGER.warn("[DivineCore] 世界中未找到选中的药水 ID");
        }

        // 清空酿造台内容（立即清空）
        for (int i = 0; i < 5; i++) items.set(i, ItemStack.EMPTY);

        final boolean chosen = containsChosenPotion;


        // 延迟一 tick 后替换方块为空气并爆炸，防止被原版逻辑覆盖
        serverLevel.getServer().execute(() -> {
            if (!serverLevel.isLoaded(pos)) return;

            // 若此时方块仍为酿造台，则替换为空气
            if (serverLevel.getBlockState(pos).getBlock() == Blocks.BREWING_STAND) {
                serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }

            // 播放音效与爆炸效果
            serverLevel.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            serverLevel.explode(null,
                    pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                    1.0F, Level.ExplosionInteraction.BLOCK);

            // 完成任务
            for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
                FTBHelper.completeTask(player, "24D5AAF7C16C26BF");
            }

            // 若包含被选中的药水，掉落 mystic_essence
            if (chosen) {
                serverLevel.getServer().execute(() -> ItemHelper.dropItemAt(
                        serverLevel,
                        new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
                        "mystic_essence", 1));
            } else {
                DivineCore.LOGGER.info("[DivineCore] 药水被摧毁，但不包含被选中的药水");
            }
        });
    }
}
