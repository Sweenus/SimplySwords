package net.sweenus.simplyswords.loot;

import net.minecraft.nbt.NbtCompound;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class PlayerPityState {
    private int uniqueMisses;
    private int tabletMisses;
    // Operator testing flag. Intentionally omitted from NBT so reconnecting or
    // restarting the server always restores normal regional pity behavior.
    private boolean ignoreRegionRestriction;
    private final Map<String, Set<Long>> uniqueRegions = new HashMap<>();
    private final Map<String, Set<Long>> tabletRegions = new HashMap<>();

    public int uniqueMisses() { return uniqueMisses; }
    public int tabletMisses() { return tabletMisses; }
    public boolean ignoresRegionRestriction() { return ignoreRegionRestriction; }
    public void setUniqueMisses(int misses) { uniqueMisses = Math.max(0, misses); }
    public void setTabletMisses(int misses) { tabletMisses = Math.max(0, misses); }
    public void setIgnoreRegionRestriction(boolean ignore) { ignoreRegionRestriction = ignore; }
    public void resetUnique() { uniqueMisses = 0; }
    public void resetTablet() { tabletMisses = 0; }
    public void missUnique() { uniqueMisses++; }
    public void missTablet() { tabletMisses++; }

    public void clearUniqueProgress() {
        resetUnique();
        uniqueRegions.clear();
    }

    public void clearTabletProgress() {
        resetTablet();
        tabletRegions.clear();
    }

    public boolean creditUnique(String dimension, long region) {
        return uniqueRegions.computeIfAbsent(dimension, key -> new HashSet<>()).add(region);
    }

    public boolean creditTablet(String dimension, long region) {
        return tabletRegions.computeIfAbsent(dimension, key -> new HashSet<>()).add(region);
    }

    public boolean hasCreditedUnique(String dimension, long region) {
        Set<Long> regions = uniqueRegions.get(dimension);
        return regions != null && regions.contains(region);
    }

    public boolean hasCreditedTablet(String dimension, long region) {
        Set<Long> regions = tabletRegions.get(dimension);
        return regions != null && regions.contains(region);
    }

    public PlayerPityState copy() {
        PlayerPityState copy = new PlayerPityState();
        copy.uniqueMisses = uniqueMisses;
        copy.tabletMisses = tabletMisses;
        copy.ignoreRegionRestriction = ignoreRegionRestriction;
        uniqueRegions.forEach((key, value) -> copy.uniqueRegions.put(key, new HashSet<>(value)));
        tabletRegions.forEach((key, value) -> copy.tabletRegions.put(key, new HashSet<>(value)));
        return copy;
    }

    public NbtCompound writeNbt() {
        NbtCompound root = new NbtCompound();
        root.putInt("UniqueMisses", uniqueMisses);
        root.putInt("TabletMisses", tabletMisses);
        root.put("UniqueRegions", writeRegions(uniqueRegions));
        root.put("TabletRegions", writeRegions(tabletRegions));
        return root;
    }

    public static PlayerPityState readNbt(NbtCompound root) {
        PlayerPityState state = new PlayerPityState();
        state.uniqueMisses = Math.max(0, root.getInt("UniqueMisses"));
        state.tabletMisses = Math.max(0, root.getInt("TabletMisses"));
        readRegions(root.getCompound("UniqueRegions"), state.uniqueRegions);
        readRegions(root.getCompound("TabletRegions"), state.tabletRegions);
        return state;
    }

    private static NbtCompound writeRegions(Map<String, Set<Long>> regions) {
        NbtCompound compound = new NbtCompound();
        regions.forEach((dimension, values) -> {
            long[] packed = new long[values.size()];
            int index = 0;
            for (long value : values) packed[index++] = value;
            compound.putLongArray(dimension, packed);
        });
        return compound;
    }

    private static void readRegions(NbtCompound compound, Map<String, Set<Long>> destination) {
        for (String key : compound.getKeys()) {
            Set<Long> values = new HashSet<>();
            for (long packed : compound.getLongArray(key)) values.add(packed);
            destination.put(key, values);
        }
    }
}
