package com.petitioner0.divinecore.edicts.sixteenth_edicts;

import com.petitioner0.divinecore.DivineCore;
import com.petitioner0.divinecore.damage_type.ModDamageSources;
import com.petitioner0.divinecore.damage_type.ModDamageTypes;
import com.petitioner0.divinecore.items.ItemHelper;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

@EventBusSubscriber(modid = DivineCore.MODID)
public class SixteenthEdicts {

    // Target depth and NBT tag keys
    private static final int TARGET_Y = -666;
    private static final String NBT_ABYSS_DEATH = "divinecore.died_at_abyss";
    private static final String NBT_ABYSS_LOCK  = "divinecore.abyss_lock";

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        BlockPos pos = player.blockPosition();

        if (player.isDeadOrDying()) return;

        if (player.getPersistentData().getBoolean(NBT_ABYSS_LOCK)) return;

        if (pos.getY() <= TARGET_Y) {
            // Put lock to ensure it only triggers once before resurrection
            player.getPersistentData().putBoolean(NBT_ABYSS_LOCK, true);

            player.getPersistentData().putBoolean(NBT_ABYSS_DEATH, true);

            player.hurt(ModDamageSources.of(player.level(), ModDamageTypes.WHISPER_OF_THE_VOID), 1000000000);

        }
    }

    @SubscribeEvent
    public static void onPlayerClone(PlayerEvent.Clone event) {
        if (!event.isWasDeath()) return;
        if (!(event.getEntity() instanceof ServerPlayer newPlayer)) return;

        // Read from old instance
        boolean abyssDeath = event.getOriginal().getPersistentData().getBoolean(NBT_ABYSS_DEATH);

        // Whether abyss death or not, clear the lock on new instance to avoid being stuck in the next life
        newPlayer.getPersistentData().remove(NBT_ABYSS_LOCK);

        if (!abyssDeath) {
            // If there is no abyss death tag, also ensure to clear the leftover tag
            newPlayer.getPersistentData().remove(NBT_ABYSS_DEATH);

            return;
        }

        // Clear the tag on new instance to avoid being triggered in the next life
        newPlayer.getPersistentData().remove(NBT_ABYSS_DEATH);

        // Trigger your resurrection logic
        ItemHelper.giveItemToPlayer(newPlayer, "whisper_of_the_void", 1);
    }
}