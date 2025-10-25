package com.petitioner0.divinecore.items.AbstractOrbitingItem;

import net.minecraft.world.item.Item;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.HashMap;
import java.util.HashSet;

public class OrbitingItemManager {
    private static final Map<UUID, Set<Item>> orbitingItems = new HashMap<>();

    public static boolean hasOrbitingItem(UUID playerId, Item item) {
        return orbitingItems.getOrDefault(playerId, Set.of()).contains(item);
    }

    public static void addOrbitingItem(UUID playerId, Item item) {
        orbitingItems.computeIfAbsent(playerId, id -> new HashSet<>()).add(item);
    }

    public static void removeOrbitingItem(UUID playerId, Item item) {
        orbitingItems.computeIfAbsent(playerId, id -> new HashSet<>()).remove(item);
    }

    public static void clearOrbitingItems(UUID playerId) {
        orbitingItems.remove(playerId);
    }

    public static Set<Item> getItemsFor(UUID playerId) {
        return orbitingItems.get(playerId);
    }
}
