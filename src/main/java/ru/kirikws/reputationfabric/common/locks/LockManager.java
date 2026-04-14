package ru.kirikws.reputationfabric.common.locks;

import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.nbt.NbtCompound;
import org.jetbrains.annotations.Nullable;

/**
 * Utility class for managing locks on chests.
 */
public class LockManager {
    private static final String LOCK_DATA_KEY = "reputation-fabric:lock_data";

    /**
     * Applies a lock to a chest block entity.
     */
    public static boolean applyLock(ChestBlockEntity chest, LockType lockType) {
        LockData lockData = new LockData(lockType);
        saveLockData(chest, lockData);
        return true;
    }

    /**
     * Removes the lock from a chest.
     */
    public static boolean removeLock(ChestBlockEntity chest) {
        NbtCompound nbt = chest.createNbt();
        nbt.remove(LOCK_DATA_KEY);
        chest.readNbt(nbt);
        return true;
    }

    /**
     * Gets the lock data from a chest.
     */
    @Nullable
    public static LockData getLockData(ChestBlockEntity chest) {
        NbtCompound nbt = chest.createNbt();
        if (nbt.contains(LOCK_DATA_KEY)) {
            return LockData.fromNbt(nbt.getCompound(LOCK_DATA_KEY));
        }
        return null;
    }

    /**
     * Checks if a chest is locked.
     */
    public static boolean isLocked(ChestBlockEntity chest) {
        LockData lockData = getLockData(chest);
        return lockData != null && lockData.isLocked();
    }

    /**
     * Saves lock data to chest NBT.
     */
    private static void saveLockData(ChestBlockEntity chest, LockData lockData) {
        NbtCompound nbt = chest.createNbt();
            nbt.put(LOCK_DATA_KEY, lockData.toNbt(new NbtCompound()));
            chest.readNbt(nbt); // Сохранить через readNbt вместо writeNbt
            chest.markDirty(); // Отметить сундук как измененный
    }

    /**
     * Checks if a block entity is a locked chest.
     */
    public static boolean isLockedChest(BlockEntity blockEntity) {
        if (blockEntity instanceof ChestBlockEntity chest) {
            return isLocked(chest);
        }
        return false;
    }
}
