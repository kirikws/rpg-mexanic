package ru.kirikws.reputationfabric.common.locks;

import org.jetbrains.annotations.Nullable;

/**
 * Interface applied to ChestBlockEntity via mixin for lock data storage.
 */
public interface LockableChest {
    @Nullable
    LockData getLockData();
    
    void setLockData(@Nullable LockData lockData);
}
