package ru.kirikws.reputationfabric.common.locks;

import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * Represents a lock state stored in a chest's NBT data.
 */
public class LockData {
    private static final String LOCK_TYPE_KEY = "LockType";
    private static final String IS_LOCKED_KEY = "IsLocked";
    private static final String SEQUENCE_KEY = "LockSequence";
    private static final String LOCK_ID_KEY = "LockId";

    private LockType lockType;
    private boolean isLocked;
    private int[] lockSequence; // For lockpicking minigame
    private UUID lockId;

    public LockData() {
        this.lockType = null;
        this.isLocked = false;
        this.lockSequence = new int[0];
        this.lockId = null;
    }

    public LockData(LockType lockType) {
        this.lockType = lockType;
        this.isLocked = true;
        this.lockSequence = generateSequence(lockType.getSequenceLength());
        this.lockId = UUID.randomUUID();
    }

    /**
     * Generates a random sequence of directions for the lockpicking minigame.
     * 0=up, 1=down, 2=left, 3=right
     */
    private int[] generateSequence(int length) {
        int[] sequence = new int[length];
        for (int i = 0; i < length; i++) {
            sequence[i] = (int) (Math.random() * 4);
        }
        return sequence;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public boolean isLocked() {
        return isLocked;
    }

    @Nullable
    public LockType getLockType() {
        return lockType;
    }

    public void setLockType(LockType lockType) {
        this.lockType = lockType;
    }

    public int[] getLockSequence() {
        return lockSequence;
    }

    public void regenerateSequence() {
        if (lockType != null) {
            this.lockSequence = generateSequence(lockType.getSequenceLength());
        }
    }

    @Nullable
    public UUID getLockId() {
        return lockId;
    }

    public void setLockId(UUID lockId) {
        this.lockId = lockId;
    }

    /**
     * Saves lock data to NBT.
     */
    public NbtCompound toNbt(NbtCompound nbt) {
        if (lockType != null) {
            nbt.putString(LOCK_TYPE_KEY, lockType.asString());
        }
        nbt.putBoolean(IS_LOCKED_KEY, isLocked);

        int[] nbtArray = new int[lockSequence.length];
        System.arraycopy(lockSequence, 0, nbtArray, 0, lockSequence.length);
        nbt.putIntArray(SEQUENCE_KEY, nbtArray);

        if (lockId != null) {
            nbt.putUuid(LOCK_ID_KEY, lockId);
        }

        return nbt;
    }

    /**
     * Loads lock data from NBT.
     */
    public static LockData fromNbt(NbtCompound nbt) {
        LockData data = new LockData();

        if (nbt.contains(LOCK_TYPE_KEY)) {
            String lockTypeName = nbt.getString(LOCK_TYPE_KEY);
            try {
                data.lockType = LockType.valueOf(lockTypeName.toUpperCase());
            } catch (IllegalArgumentException e) {
                data.lockType = null;
            }
        }

        data.isLocked = nbt.getBoolean(IS_LOCKED_KEY);

        if (nbt.contains(SEQUENCE_KEY)) {
            data.lockSequence = nbt.getIntArray(SEQUENCE_KEY);
        }

        if (nbt.contains(LOCK_ID_KEY)) {
            data.lockId = nbt.getUuid(LOCK_ID_KEY);
        }

        return data;
    }
}
