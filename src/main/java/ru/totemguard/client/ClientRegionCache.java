package ru.totemguard.client;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Stores region boundary data received from the server for rendering.
 */
public class ClientRegionCache {
    private static final List<Region> regions = new ArrayList<>();
    private static long lastUpdate = 0;
    private static final long CACHE_TTL = 30_000; // 30 seconds

    public static void update(List<Region> newRegions) {
        regions.clear();
        regions.addAll(newRegions);
        lastUpdate = System.currentTimeMillis();
    }

    public static List<Region> getRegions() {
        if (System.currentTimeMillis() - lastUpdate > CACHE_TTL) {
            regions.clear();
        }
        return regions;
    }

    public static class Region {
        public final UUID owner;
        public final boolean isOwner;
        public final float[][] points; // x, z pairs

        public Region(UUID owner, boolean isOwner, float[][] points) {
            this.owner = owner;
            this.isOwner = isOwner;
            this.points = points;
        }
    }
}
