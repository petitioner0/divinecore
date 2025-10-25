package com.petitioner0.divinecore.edicts.eleventh_edicts;



import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

public class OneTimeEndWitherData extends SavedData {
    private static final String KEY = "divinecore_end_wither_once";
    private static final String TAG_TRIGGERED = "triggered";

    private boolean triggered = false;

    // When there is no data, create a new instance (provided to Factory)
    public static OneTimeEndWitherData create() {
        return new OneTimeEndWitherData();
    }

    // Load from save (signature must include HolderLookup.Provider)
    public static OneTimeEndWitherData load(CompoundTag tag, HolderLookup.Provider lookup) {
        OneTimeEndWitherData data = new OneTimeEndWitherData();
        data.triggered = tag.getBoolean(TAG_TRIGGERED);
        return data;
    }

    // Save to save (signature must include HolderLookup.Provider)
    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        tag.putBoolean(TAG_TRIGGERED, this.triggered);
        return tag;
    }

    public boolean isTriggered() {
        return triggered;
    }

    public void setTriggered(boolean triggered) {
        this.triggered = triggered;
        this.setDirty();
    }
    
    public static OneTimeEndWitherData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(OneTimeEndWitherData::create, OneTimeEndWitherData::load),
                KEY
        );
    }
}