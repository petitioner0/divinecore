package com.petitioner0.divinecore.edicts.fifth_edicts;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.storage.DimensionDataStorage;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.level.LevelEvent;

import com.petitioner0.divinecore.DivineCore;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@EventBusSubscriber(modid = "divinecore")
public class DivineWorldPotion {

    private static final Set<ResourceLocation> EXCLUDED = Set.of(
            ResourceLocation.tryParse("minecraft:empty"),
            ResourceLocation.tryParse("minecraft:water"),
            ResourceLocation.tryParse("minecraft:mundane"),
            ResourceLocation.tryParse("minecraft:thick"),
            ResourceLocation.tryParse("minecraft:awkward"));

    /** The public method to get the chosen potion ID from any ServerLevel */
    public static ResourceLocation getChosenPotionId(ServerLevel level) {
        DivineSaved data = getOrCreateData(level);
        return data.chosenPotionId;
    }

    /** Check if a Potion is the chosen one */
    public static boolean isChosenPotion(ServerLevel level, Potion potion) {
        ResourceLocation chosen = getChosenPotionId(level);
        if (chosen == null)
            return false;
        ResourceLocation thisId = level.registryAccess()
                .registryOrThrow(Registries.POTION)
                .getKey(potion);
        return chosen.equals(thisId);
    }

    /** Pick a random potion, excluding basic potions */
    public static ResourceLocation pickRandomPotion(ServerLevel level) {
        Registry<Potion> reg = level.registryAccess().registryOrThrow(Registries.POTION);
        List<ResourceLocation> ids = new ArrayList<>(reg.keySet());
        ids.removeIf(EXCLUDED::contains);

        if (ids.isEmpty())
            return null;
        return ids.get(level.getRandom().nextInt(ids.size()));
    }

    /** When the world is loaded: if it has not been written, pick one randomly and save it; otherwise, only read it. */
    @SubscribeEvent
    public static void onLevelLoad(LevelEvent.Load event) {
        if (!(event.getLevel() instanceof ServerLevel level))
            return;
        // Only initialize in the overworld, to avoid duplication in multiple dimensions
        if (!level.dimension().equals(Level.OVERWORLD))
            return;

        DivineSaved data = getOrCreateData(level);

        if (data.chosenPotionId == null) {
            // Use the new picking method, excluding basic potions
            ResourceLocation chosen = pickRandomPotion(level);
            if (chosen != null) {
                data.setChosenPotionId(chosen); // Mark dirty and save
                DivineCore.LOGGER.info("[DivineCore] 已选择药水: " + chosen);
            }
        } else {
            DivineCore.LOGGER.info("[DivineCore] 已存在选中的药水: " + data.chosenPotionId);
        }
    }

    private static DivineSaved getOrCreateData(ServerLevel level) {
        DimensionDataStorage storage = level.getDataStorage();
        return storage.computeIfAbsent(
                new SavedData.Factory<>(
                        DivineSaved::new, 
                        (tag, provider) -> DivineSaved.load(tag) 
                ),
                DivineSaved.KEY);
    }

    public static class DivineSaved extends SavedData {
        public static final String KEY = "divinecore_world_potion";
        private ResourceLocation chosenPotionId;

        public DivineSaved() {
        } 

        public void setChosenPotionId(ResourceLocation id) {
            this.chosenPotionId = id;
            this.setDirty();
        }

        @Override
        public net.minecraft.nbt.CompoundTag save(@javax.annotation.Nonnull net.minecraft.nbt.CompoundTag tag,
                @javax.annotation.Nonnull net.minecraft.core.HolderLookup.Provider provider) {
            if (chosenPotionId != null) {
                tag.putString("Potion", chosenPotionId.toString());
            }
            return tag;
        }

        public static DivineSaved load(net.minecraft.nbt.CompoundTag tag) {
            DivineSaved data = new DivineSaved();
            String s = tag.getString("Potion");
            data.chosenPotionId = s.isEmpty() ? null : ResourceLocation.tryParse(s);
            return data;
        }
    }
}