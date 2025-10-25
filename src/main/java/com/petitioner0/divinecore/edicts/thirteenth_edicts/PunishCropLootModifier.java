package com.petitioner0.divinecore.edicts.thirteenth_edicts;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.common.loot.LootModifier;

import com.petitioner0.divinecore.items.ItemHelper;
import javax.annotation.Nonnull;

public class PunishCropLootModifier extends LootModifier {

    public static final MapCodec<PunishCropLootModifier> CODEC =
            RecordCodecBuilder.mapCodec(inst ->
                    LootModifier.codecStart(inst).apply(inst, PunishCropLootModifier::new)
            );

    public PunishCropLootModifier(net.minecraft.world.level.storage.loot.predicates.LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    public MapCodec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(@Nonnull ObjectArrayList<ItemStack> generatedLoot, @Nonnull LootContext ctx) {
        BlockState state = ctx.getParamOrNull(LootContextParams.BLOCK_STATE);
        if (state == null) return generatedLoot;

        if (!(state.getBlock() instanceof CropBlock crop)) return generatedLoot;

        if (!state.hasProperty(CropBlock.AGE)) return generatedLoot;
        int age = state.getValue(CropBlock.AGE);
        if (age < crop.getMaxAge()) return generatedLoot;

        ServerLevel level = ctx.getLevel();
        if (level == null) return generatedLoot;

        var origin = ctx.getParamOrNull(LootContextParams.ORIGIN);
        if (origin == null) return generatedLoot;
        BlockPos pos = BlockPos.containing(origin);

        var levelMap = CropPunisher.GROWN_TRACKER.get(level);
        if (levelMap == null) return generatedLoot;

        Integer times = levelMap.get(pos);
        if (times == null || times <= 0) return generatedLoot;

        double p = Math.min(1.0, 0.03 * times);
        RandomSource rand = ctx.getRandom();
        if (rand.nextDouble() < p) {
            generatedLoot.add(new ItemStack(ItemHelper.getItemByName("sacrificial_verdant").get(), 1)); 
        }
        
        levelMap.remove(pos);

        return generatedLoot;
    }
}
