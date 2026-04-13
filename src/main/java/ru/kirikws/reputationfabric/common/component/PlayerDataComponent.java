package ru.kirikws.reputationfabric.common.component;

import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.entity.Entity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.WorldSavePath;
import ru.kirikws.reputationfabric.ReputationFabricMod;
import ru.kirikws.reputationfabric.common.component.data.PlayerData;
import ru.kirikws.reputationfabric.common.network.packet.SyncKarmaS2CPacket;
import ru.kirikws.reputationfabric.common.network.packet.SyncModeS2CPacket;

/**
 * Unified player data component combining karma and mode systems.
 * Replaces the previous separate KarmaComponent and ModeComponent.
 */
public class PlayerDataComponent implements AutoSyncedComponent {
    private static final String MOD_DATA_KEY = ReputationFabricMod.MOD_ID;
    private static final String KARMA_KEY = "karma";
    private static final String MODE_KEY = "mode";
    private static final String LAST_MODE_CHANGE_KEY = "last_mode_change";
    private static final String LAST_DAMAGE_TAKEN_KEY = "last_damage_taken";

    // Player data fields
    private final Entity provider;
    private int karma = 0;
    private PlayerData.PlayerMode mode = PlayerData.PlayerMode.PASSIVE;
    private long lastModeChangeTime = 0;
    private long lastDamageTakenTime = 0;

    public PlayerDataComponent(Entity provider) {
        this.provider = provider;
    }

    // === Karma Operations ===

    public int getKarma() {
        return karma;
    }

    public void setKarma(int karma) {
        this.karma = karma;
    }

    public void modifyKarma(int delta) {
        this.karma += delta;
    }

    // === Mode Operations ===

    public PlayerData.PlayerMode getMode() {
        return mode;
    }

    public void setMode(PlayerData.PlayerMode mode) {
        this.mode = mode;
        this.lastModeChangeTime = System.currentTimeMillis();
    }

    public long getLastModeChangeTime() {
        return lastModeChangeTime;
    }

    public long getLastDamageTakenTime() {
        return lastDamageTakenTime;
    }

    public void setLastDamageTakenTime(long timestamp) {
        this.lastDamageTakenTime = timestamp;
    }

    // === Sync ===

    public void syncToClient() {
        if (provider instanceof ServerPlayerEntity sp) {
            SyncKarmaS2CPacket.send(sp, karma);
            SyncModeS2CPacket.send(sp, mode);
        }
    }

    // === NBT Serialization ===

    @Override
    public void readFromNbt(NbtCompound tag) {
        if (tag.contains(MOD_DATA_KEY)) {
            NbtCompound modTag = tag.getCompound(MOD_DATA_KEY);
            if (modTag.contains(KARMA_KEY)) {
                this.karma = modTag.getInt(KARMA_KEY);
            }
            if (modTag.contains(MODE_KEY)) {
                this.mode = PlayerData.PlayerMode.valueOf(modTag.getString(MODE_KEY));
            }
            if (modTag.contains(LAST_MODE_CHANGE_KEY)) {
                this.lastModeChangeTime = modTag.getLong(LAST_MODE_CHANGE_KEY);
            }
            if (modTag.contains(LAST_DAMAGE_TAKEN_KEY)) {
                this.lastDamageTakenTime = modTag.getLong(LAST_DAMAGE_TAKEN_KEY);
            }
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        NbtCompound modTag = new NbtCompound();
        modTag.putInt(KARMA_KEY, this.karma);
        modTag.putString(MODE_KEY, this.mode.name());
        modTag.putLong(LAST_MODE_CHANGE_KEY, this.lastModeChangeTime);
        modTag.putLong(LAST_DAMAGE_TAKEN_KEY, this.lastDamageTakenTime);
        tag.put(MOD_DATA_KEY, modTag);
    }
}
