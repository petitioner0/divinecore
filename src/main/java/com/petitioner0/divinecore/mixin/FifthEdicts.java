package com.petitioner0.divinecore.mixin;


import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.FTBHelper;
import com.petitioner0.divinecore.items.ItemHelper;
import com.petitioner0.divinecore.edicts.fifth_edicts.DivineWorldPotion;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BrewingStandBlockEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.alchemy.Potion;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.Random;

@Mixin(BrewingStandBlockEntity.class)
public abstract class FifthEdicts {

    @Inject(method = "doBrew", at = @At("TAIL"))
    private static void divinecore$onBrewFinished(Level level, BlockPos pos,
                                                  java.util.List<ItemStack> items,
                                                  CallbackInfo ci) {
        if (!(level instanceof ServerLevel serverLevel)) return;

        MinecraftServer server = serverLevel.getServer();

        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            FTBHelper.completeTask(player, "53C8B1EA7CDBF967");
        }

        int potionCount = 0;
        for (int i = 0; i < 3; i++) {
            ItemStack s = items.get(i);
            if (s.get(DataComponents.POTION_CONTENTS) != null)
                potionCount++;
        }

        boolean isNether = serverLevel.dimension().location().toString().equals("minecraft:the_nether");
        boolean shouldExplode = isNether;
        Random random = new Random();

        if (!isNether) {
            for (int i = 0; i < potionCount; i++) {
                if (random.nextDouble() < 0.1) {
                    shouldExplode = true;
                    break;
                }
            }
        }

        if (isNether) {
            for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
                FTBHelper.completeTask(player, "675E0209B1642617");
            }
        }

        if (!shouldExplode) return;

        boolean containsChosenPotion = false;
        for (int i = 0; i < 3; i++) {
            ItemStack potionStack = items.get(i);
            var contents = potionStack.get(DataComponents.POTION_CONTENTS);
            if (contents != null && contents.potion().isPresent()) {
                Potion potion = contents.potion().get().value();
                if (DivineWorldPotion.isChosenPotion((ServerLevel) level, potion)) {
                    containsChosenPotion = true;
                    break;
                }
            }
        }

        // 清空酿造台内容
        for (int i = 0; i < 5; i++) items.set(i, ItemStack.EMPTY);

        serverLevel.playSound(null, pos, SoundEvents.GLASS_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
        serverLevel.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        serverLevel.explode(null, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                1.0F, Level.ExplosionInteraction.BLOCK);

        for (ServerPlayer player : serverLevel.getServer().getPlayerList().getPlayers()) {
            FTBHelper.completeTask(player, "24D5AAF7C16C26BF");
        }

        if (containsChosenPotion) {
            // 延迟 1 tick 生成物品
            serverLevel.getServer().execute(() -> ItemHelper.dropItemAt(
                    serverLevel, new Vec3(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5),
                    "mystic_essence", 1));
        } else {
            DivineCore.LOGGER.info("[DivineCore] 药水被摧毁，但不包含被选中的药水");
        }
    }
}