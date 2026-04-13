package ru.totemguard.storage;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import ru.totemguard.geometry.CuboidRegion;
import ru.totemguard.geometry.UnionedRegion;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Server-wide persistent storage for all totem regions.
 * Uses a Singleton pattern per server instance to ensure data consistency
 * without reloading from disk on every access.
 */
public class RegionState {
    public static final String KEY = "totemguard_regions";

    // Singleton cache: Server -> Instance
    private static final Map<MinecraftServer, RegionState> INSTANCES = new HashMap<>();

    // Reference to the server this state belongs to
    private MinecraftServer server;

    /**
     * Represents a single totem: position, owner, and current radius.
     */
    public static class TotemEntry {
        public final BlockPos pos;
        public final UUID owner;
        public int radius;

        public TotemEntry(BlockPos pos, UUID owner, int radius) {
            this.pos = pos;
            this.owner = owner;
            this.radius = radius;
        }

        public NbtCompound toNbt() {
            NbtCompound tag = new NbtCompound();
            tag.putIntArray("pos", new int[]{pos.getX(), pos.getY(), pos.getZ()});
            tag.putUuid("owner", owner);
            tag.putInt("radius", radius);
            return tag;
        }

        public static TotemEntry fromNbt(NbtCompound tag) {
            int[] posArr = tag.getIntArray("pos");
            BlockPos pos = new BlockPos(posArr[0], posArr[1], posArr[2]);
            UUID owner = tag.getUuid("owner");
            int radius = tag.getInt("radius");
            return new TotemEntry(pos, owner, radius);
        }
    }

    // All totems grouped by owner UUID
    private final Map<UUID, List<TotemEntry>> totemsByOwner = new HashMap<>();
    // Pre-computed unioned regions per owner
    private final Map<UUID, UnionedRegion> regionsByOwner = new HashMap<>();

    // Flag to check if data has changed since last save
    private boolean dirty = false;

    /**
     * Gets the cached RegionState for the server, or loads it if missing.
     */
    public static synchronized RegionState getOrCreate(MinecraftServer server) {
        return INSTANCES.computeIfAbsent(server, RegionState::loadOrCreate);
    }

    private static RegionState loadOrCreate(MinecraftServer server) {
        RegionState state = new RegionState();
        state.server = server; // Store reference for auto-saving
        Path savePath = server.getSavePath(WorldSavePath.ROOT);
        File dataFile = savePath.resolve(KEY + ".dat").toFile();
        
        if (dataFile.exists()) {
            try {
                NbtCompound nbt = NbtIo.read(dataFile);
                if (nbt != null && nbt.contains("Data", 10)) {
                    state.readNbt(nbt.getCompound("Data"));
                    System.out.println("[TotemGuard] Loaded region state from disk.");
                    return state;
                }
            } catch (IOException e) {
                System.err.println("[TotemGuard] Failed to read region file: " + e.getMessage());
            }
        }
        System.out.println("[TotemGuard] Created new region state.");
        return state;
    }

    /**
     * Clears the cache (e.g. on server stop).
     */
    public static void clearCache(MinecraftServer server) {
        INSTANCES.remove(server);
    }

    // === Public API ===

    public void addTotem(BlockPos pos, UUID owner, int radius) {
        totemsByOwner.computeIfAbsent(owner, k -> new ArrayList<>())
                .add(new TotemEntry(pos, owner, radius));
        rebuildRegion(owner);
        markDirty();
    }

    public boolean removeTotem(BlockPos pos) {
        for (Iterator<Map.Entry<UUID, List<TotemEntry>>> it = totemsByOwner.entrySet().iterator(); it.hasNext(); ) {
            Map.Entry<UUID, List<TotemEntry>> entry = it.next();
            List<TotemEntry> totems = entry.getValue();
            for (Iterator<TotemEntry> tit = totems.iterator(); tit.hasNext(); ) {
                TotemEntry totem = tit.next();
                if (totem.pos.equals(pos)) {
                    tit.remove();
                    if (totems.isEmpty()) {
                        it.remove();
                        regionsByOwner.remove(entry.getKey());
                    } else {
                        rebuildRegion(entry.getKey());
                    }
                    markDirty();
                    return true;
                }
            }
        }
        return false;
    }

    public void updateTotemRadius(BlockPos pos, int newRadius) {
        for (List<TotemEntry> totems : totemsByOwner.values()) {
            for (TotemEntry totem : totems) {
                if (totem.pos.equals(pos)) {
                    totem.radius = newRadius;
                    rebuildRegion(totem.owner);
                    markDirty();
                    return;
                }
            }
        }
    }

    private void rebuildRegion(UUID owner) {
        List<TotemEntry> totems = totemsByOwner.get(owner);
        if (totems == null || totems.isEmpty()) {
            regionsByOwner.remove(owner);
            return;
        }
        UnionedRegion region = new UnionedRegion();
        for (TotemEntry totem : totems) {
            region.add(CuboidRegion.fromCenter(totem.pos, totem.radius));
        }
        region.merge();
        regionsByOwner.put(owner, region);
    }

    public UUID getOwner(BlockPos pos) {
        for (Map.Entry<UUID, UnionedRegion> entry : regionsByOwner.entrySet()) {
            if (entry.getValue().contains(pos)) {
                return entry.getKey();
            }
        }
        return null;
    }

    public UnionedRegion getRegion(UUID owner) {
        return regionsByOwner.get(owner);
    }

    public Set<UUID> getAllOwners() {
        return Collections.unmodifiableSet(totemsByOwner.keySet());
    }

    public List<TotemEntry> getTotems(UUID owner) {
        return Collections.unmodifiableList(totemsByOwner.getOrDefault(owner, Collections.emptyList()));
    }

    /**
     * Marks state as dirty and saves immediately.
     */
    public void markDirty() {
        this.dirty = true;
        if (server != null) {
            save(); 
        }
    }

    /**
     * Saves the state to disk.
     */
    public void save() {
        if (server == null) return;
        if (!dirty) return;

        Path savePath = server.getSavePath(WorldSavePath.ROOT);
        File dataFile = savePath.resolve(KEY + ".dat").toFile();
        try {
            dataFile.getParentFile().mkdirs(); // Ensure directory exists
            NbtCompound root = new NbtCompound();
            root.put("Data", writeNbt(new NbtCompound()));
            NbtIo.write(root, dataFile);
            dirty = false;
        } catch (IOException e) {
            System.err.println("[TotemGuard] Failed to save region state: " + e.getMessage());
        }
    }

    // === NBT ===

    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtList ownersList = new NbtList();
        for (Map.Entry<UUID, List<TotemEntry>> entry : totemsByOwner.entrySet()) {
            NbtCompound ownerTag = new NbtCompound();
            ownerTag.putUuid("owner", entry.getKey());
            NbtList totemsList = new NbtList();
            for (TotemEntry totem : entry.getValue()) {
                totemsList.add(totem.toNbt());
            }
            ownerTag.put("totems", totemsList);
            ownersList.add(ownerTag);
        }
        nbt.put("owners", ownersList);
        return nbt;
    }

    public void readNbt(NbtCompound nbt) {
        totemsByOwner.clear();
        regionsByOwner.clear();
        NbtList ownersList = nbt.getList("owners", 10);
        for (int i = 0; i < ownersList.size(); i++) {
            NbtCompound ownerTag = ownersList.getCompound(i);
            UUID owner = ownerTag.getUuid("owner");
            NbtList totemsList = ownerTag.getList("totems", 10);
            List<TotemEntry> totems = new ArrayList<>();
            for (int j = 0; j < totemsList.size(); j++) {
                totems.add(TotemEntry.fromNbt(totemsList.getCompound(j)));
            }
            totemsByOwner.put(owner, totems);
            rebuildRegion(owner);
        }
    }
}
