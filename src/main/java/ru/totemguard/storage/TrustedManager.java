package ru.totemguard.storage;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtList;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.WorldSavePath;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Manages trusted (whitelisted) players per region owner.
 * Trusted players have the same permissions as the owner,
 * except they cannot destroy the totem or add new trusted players.
 *
 * Stored in 'totemguard_trust.dat' in the world save directory.
 */
public class TrustedManager {
    public static final String KEY = "totemguard_trust";

    // owner UUID -> set of trusted UUIDs
    private final Map<UUID, Set<UUID>> trustLists = new HashMap<>();

    /**
     * Adds a player to the owner's trusted list.
     */
    public void add(UUID owner, UUID trusted) {
        trustLists.computeIfAbsent(owner, k -> new HashSet<>()).add(trusted);
    }

    /**
     * Removes a player from the owner's trusted list.
     */
    public void remove(UUID owner, UUID trusted) {
        Set<UUID> list = trustLists.get(owner);
        if (list != null) {
            list.remove(trusted);
            if (list.isEmpty()) {
                trustLists.remove(owner);
            }
        }
    }

    /**
     * Checks if a player is trusted by a specific owner.
     */
    public boolean isTrusted(UUID owner, UUID player) {
        Set<UUID> list = trustLists.get(owner);
        return list != null && list.contains(player);
    }

    /**
     * Checks if a player is an owner or trusted by any owner.
     */
    public boolean isTrustedFor(RegionState regionState, UUID player, UUID regionOwner) {
        return player.equals(regionOwner) || isTrusted(regionOwner, player);
    }

    /**
     * Gets the trusted list for an owner.
     */
    public Set<UUID> getTrusted(UUID owner) {
        return Collections.unmodifiableSet(trustLists.getOrDefault(owner, Collections.emptySet()));
    }

    /**
     * Gets all trusted players across all owners.
     */
    public Set<UUID> getAllTrusted() {
        Set<UUID> all = new HashSet<>();
        for (Set<UUID> list : trustLists.values()) {
            all.addAll(list);
        }
        return all;
    }

    // === NBT ===

    public NbtCompound toNbt() {
        NbtCompound root = new NbtCompound();
        NbtList ownersList = new NbtList();
        for (Map.Entry<UUID, Set<UUID>> entry : trustLists.entrySet()) {
            NbtCompound tag = new NbtCompound();
            tag.putUuid("owner", entry.getKey());
            NbtList trustedList = new NbtList();
            for (UUID trusted : entry.getValue()) {
                NbtCompound t = new NbtCompound();
                t.putUuid("trusted", trusted);
                trustedList.add(t);
            }
            tag.put("trusted", trustedList);
            ownersList.add(tag);
        }
        root.put("owners", ownersList);
        return root;
    }

    public static TrustedManager fromNbt(NbtCompound nbt) {
        TrustedManager manager = new TrustedManager();
        NbtList ownersList = nbt.getList("owners", 10);
        for (int i = 0; i < ownersList.size(); i++) {
            NbtCompound ownerTag = ownersList.getCompound(i);
            UUID owner = ownerTag.getUuid("owner");
            NbtList trustedList = ownerTag.getList("trusted", 10);
            Set<UUID> trusted = new HashSet<>();
            for (int j = 0; j < trustedList.size(); j++) {
                trusted.add(trustedList.getCompound(j).getUuid("trusted"));
            }
            manager.trustLists.put(owner, trusted);
        }
        return manager;
    }

    public static TrustedManager getOrCreate(MinecraftServer server) {
        Path savePath = server.getSavePath(WorldSavePath.ROOT);
        File dataFile = savePath.resolve(KEY + ".dat").toFile();
        if (dataFile.exists()) {
            try {
                NbtCompound nbt = NbtIo.read(dataFile);
                if (nbt != null) {
                    return fromNbt(nbt);
                }
            } catch (IOException e) {
                // Fall through
            }
        }
        return new TrustedManager();
    }

    public void save(MinecraftServer server) {
        Path savePath = server.getSavePath(WorldSavePath.ROOT);
        File dataFile = savePath.resolve(KEY + ".dat").toFile();
        try {
            NbtIo.write(toNbt(), dataFile);
        } catch (IOException e) {
            // Silent
        }
    }
}
