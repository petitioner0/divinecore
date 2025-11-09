package com.petitioner0.divinecore.edicts.eleventh_edicts;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import java.util.*;

public class EndBossSharedHealthData extends SavedData {
    private static final String KEY = "dc_end_shared_health";
    private float pool = 500f;
    private final List<UUID> members = new ArrayList<>();

    public EndBossSharedHealthData() {}

    public static EndBossSharedHealthData load(CompoundTag tag, HolderLookup.Provider lookup) {
        EndBossSharedHealthData d = new EndBossSharedHealthData();
        d.pool = tag.getFloat("pool");
        var list = tag.getList("ids", 8);
        for (int i = 0; i < list.size(); i++) {
            d.members.add(UUID.fromString(list.getString(i)));
        }
        return d;
    }

    @Override
    public CompoundTag save(@javax.annotation.Nonnull CompoundTag tag, @javax.annotation.Nonnull HolderLookup.Provider registries) {
        tag.putFloat("pool", pool);
        var list = new net.minecraft.nbt.ListTag();
        for (var id : members) list.add(net.minecraft.nbt.StringTag.valueOf(id.toString()));
        tag.put("ids", list);
        return tag;
    }

    public static EndBossSharedHealthData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(
            new SavedData.Factory<>(EndBossSharedHealthData::new, EndBossSharedHealthData::load),
            KEY
        );
    }

    public void add(UUID id) {
        if (!members.contains(id)) {
            members.add(id);
            setDirty();
        }
    }

    public void remove(UUID id) {
        if (members.remove(id)) {
            setDirty();
        }
    }

    public boolean contains(UUID id) { return members.contains(id); }
    public float get() { return pool; }
    public void set(float v) { pool = Math.max(0, v); setDirty(); }
    public List<UUID> ids() { return members; }
}
