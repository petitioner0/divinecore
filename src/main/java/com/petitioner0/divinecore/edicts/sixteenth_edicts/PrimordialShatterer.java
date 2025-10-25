package com.petitioner0.divinecore.edicts.sixteenth_edicts;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import com.petitioner0.divinecore.DivineCore;
import javax.annotation.Nonnull;

@EventBusSubscriber(modid = DivineCore.MODID)
public class PrimordialShatterer extends Item {

    private static final String TAG_TARGET_POS = "TargetPos";
    private static final String TAG_PROGRESS   = "ChargeProgress";
    private static final String TAG_LAST_TICK  = "LastClickGameTime";

    private static final int DURATION_TICKS = 400;           // Total charging duration
    private static final int SOUND_INTERVAL = 20;            // Sound interval for progress indication

    public PrimordialShatterer(Properties props) { super(props); }

    @SubscribeEvent
    public static void onLeftClickBlock(PlayerInteractEvent.LeftClickBlock evt) {
        Player player = evt.getEntity();
        Level level = player.level();
        BlockPos pos = evt.getPos();
        BlockState state = level.getBlockState(pos);
        InteractionHand hand = evt.getHand();

        ItemStack held = player.getItemInHand(hand);
        if (!(held.getItem() instanceof PrimordialShatterer)) return;

        // Only for bedrock: other blocks use default logic
        if (!state.is(Blocks.BEDROCK)) return;

        // Cancel vanilla breaking pipeline, no vanilla mining progress; cancel on both client and server
        evt.setCanceled(true);

        // Logic only executes on server side; client only needs to cancel the event
        if (level.isClientSide) return;

        ServerLevel sLevel = (ServerLevel) level;

        // Target is still bedrock
        if (!level.getBlockState(pos).is(Blocks.BEDROCK)) {
            clearAll(held);
            return;
        }

        // Timer in item NBT
        CustomData.update(DataComponents.CUSTOM_DATA, held, nbt -> {
            long now = sLevel.getGameTime();

            boolean targetChanged = !nbt.contains(TAG_TARGET_POS) || nbt.getLong(TAG_TARGET_POS) != pos.asLong();
            int progress = nbt.getInt(TAG_PROGRESS);
            long last = nbt.getLong(TAG_LAST_TICK);

            boolean timedOut = last != 0L && (now - last) > 3L;
            if (targetChanged || timedOut) {
                nbt.putLong(TAG_TARGET_POS, pos.asLong());
                nbt.putInt(TAG_PROGRESS, 0);
                player.swing(hand, true);
                level.playSound(null, pos, SoundEvents.ANVIL_PLACE, SoundSource.PLAYERS, 0.6f, 1.2f);
                progress = 0;
            }

            // Increment once per tick
            progress++;
            nbt.putInt(TAG_PROGRESS, progress);
            nbt.putLong(TAG_LAST_TICK, now);

            // Play progress sound every 20 ticks
            if (progress > 0 && progress % SOUND_INTERVAL == 0) {
                sLevel.playSound(null, pos, SoundEvents.GRINDSTONE_USE, SoundSource.PLAYERS, 0.3f, 1.0f);
            }

            // Fully charged → destroy bedrock and lose durability
            if (progress >= DURATION_TICKS) {
                boolean destroyed = sLevel.destroyBlock(pos, false, player); // No block drops
                if (destroyed) {
                    held.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);   // Lose 1 durability
                }
                // Clear timer/target
                nbt.remove(TAG_TARGET_POS);
                nbt.remove(TAG_PROGRESS);
                nbt.remove(TAG_LAST_TICK);
            }
        });
    }


    private static void clearAll(@Nonnull ItemStack stack) {
        if (!stack.has(DataComponents.CUSTOM_DATA)) return;
        CustomData.update(DataComponents.CUSTOM_DATA, stack, nbt -> {
            nbt.remove(TAG_TARGET_POS);
            nbt.remove(TAG_PROGRESS);
            nbt.remove(TAG_LAST_TICK);
        });
        var data = stack.get(DataComponents.CUSTOM_DATA);
        if (data != null && data.isEmpty()) stack.remove(DataComponents.CUSTOM_DATA);
    }
}