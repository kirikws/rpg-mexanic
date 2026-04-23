package ru.kirikws.reputationfabric.common.locks;

import net.minecraft.block.Block;
import net.minecraft.block.entity.ChestBlockEntity;
import org.jetbrains.annotations.Nullable;
import ru.kirikws.reputationfabric.ReputationFabricMod;

public class LockManager {
    private static final org.slf4j.Logger LOGGER = org.slf4j.LoggerFactory.getLogger(LockManager.class);

    public static boolean applyLock(ChestBlockEntity chest, LockType lockType) {
        LOGGER.info("[LockManager] Применяем замок типа: {}", lockType);
        LockData lockData = new LockData(lockType);
        LOGGER.info("[LockManager] LockData создан: isLocked={}, lockId={}", lockData.isLocked(), lockData.getLockId());
        
        // Используем интерфейс для установки данных
        LockableChest lockable = (LockableChest) chest;
        lockable.setLockData(lockData);
        
        chest.markDirty();
        syncToClients(chest);
        
        // Проверяем, что сохранилось
        LockData saved = getLockData(chest);
        LOGGER.info("[LockManager] После сохранения: isLocked={}", saved != null && saved.isLocked());
        return true;
    }

    public static boolean removeLock(ChestBlockEntity chest) {
        LockableChest lockable = (LockableChest) chest;
        lockable.setLockData(null);
        chest.markDirty();
        syncToClients(chest);
        return true;
    }

    @Nullable
    public static LockData getLockData(ChestBlockEntity chest) {
        LockableChest lockable = (LockableChest) chest;
        return lockable.getLockData();
    }

    public static boolean isLocked(ChestBlockEntity chest) {
        LockData lockData = getLockData(chest);
        return lockData != null && lockData.isLocked();
    }

    private static void syncToClients(ChestBlockEntity chest) {
        if (chest.getWorld() != null && !chest.getWorld().isClient) {
            // Обновляем BlockEntity на клиентах
            chest.getWorld().updateListeners(
                chest.getPos(),
                chest.getCachedState(),
                chest.getCachedState(),
                Block.NOTIFY_ALL
            );
        }
    }

    public static boolean isLockedChest(net.minecraft.block.entity.BlockEntity blockEntity) {
        return blockEntity instanceof ChestBlockEntity chest && isLocked(chest);
    }
}
