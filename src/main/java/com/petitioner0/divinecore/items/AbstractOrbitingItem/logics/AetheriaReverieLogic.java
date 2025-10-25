package com.petitioner0.divinecore.items.AbstractOrbitingItem.logics;

import com.petitioner0.divinecore.DivineCore;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.common.NeoForgeMod;

public class AetheriaReverieLogic implements IOrbitingLogic {


    private static final ResourceLocation ENABLE_ID =
            ResourceLocation.fromNamespaceAndPath(DivineCore.MODID, "creative_flight_enable");

    @Override
    public void tick(Player player) {
        // Get the CREATIVE_FLIGHT attribute
        AttributeInstance attr = player.getAttribute(NeoForgeMod.CREATIVE_FLIGHT);
        if (attr == null) return; // If the player doesn't have the attribute, skip

        boolean inDream = player.level().dimension().location().equals(ResourceLocation.parse(DivineCore.MODID + ":" + "dream"));
        AttributeModifier enableMod = attr.getModifier(ENABLE_ID);

        if (inDream) {
            // If in the dream dimension and flight is not yet enabled, add the modifier
            if (enableMod == null) {
                attr.addTransientModifier(new AttributeModifier(
                        ENABLE_ID,
                        1.0,
                        AttributeModifier.Operation.ADD_VALUE
                ));
                player.onUpdateAbilities(); 
            }
        } else {
            // If leaving the dream dimension, remove the flight modifier
            if (enableMod != null) {
                attr.removeModifier(enableMod);
                player.onUpdateAbilities();
            }
        }
    }
}
