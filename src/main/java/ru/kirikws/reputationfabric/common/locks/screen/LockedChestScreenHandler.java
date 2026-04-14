package ru.kirikws.reputationfabric.common.locks.screen;

import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import ru.kirikws.reputationfabric.common.locks.LockData;
import ru.kirikws.reputationfabric.common.locks.LockManager;
import ru.kirikws.reputationfabric.common.locks.LockType;
import ru.kirikws.reputationfabric.common.locks.item.KeyItem;
import ru.kirikws.reputationfabric.common.locks.item.LockpickItem;

import java.util.UUID;

/**
 * Screen handler for locked chests with key/lockpick slots.
 */
public class LockedChestScreenHandler extends ScreenHandler {
    private final ChestBlockEntity chest;
    private final BlockPos pos;
    private final Inventory specialSlots;
    private LockData lockData;

    // Slot indices
    private static final int KEY_SLOT = 0;
    private static final int LOCKPICK_SLOT = 1;

    public LockedChestScreenHandler(int syncId, PlayerInventory playerInventory, ChestBlockEntity chest, BlockPos pos) {
        super(ReputationFabricScreenHandlers.LOCKED_CHEST_HANDLER, syncId);
        this.chest = chest;
        this.pos = pos;
        this.lockData = LockManager.getLockData(chest);
        this.specialSlots = new SimpleInventory(2) {
            @Override
            public void markDirty() {
                super.markDirty();
                onSpecialSlotChange();
            }
        };

        // Key slot (for unlocking with proper key)
        this.addSlot(new Slot(specialSlots, KEY_SLOT, 80, 20) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem() instanceof KeyItem;
            }
        });

        // Lockpick slot
        this.addSlot(new Slot(specialSlots, LOCKPICK_SLOT, 80, 50) {
            @Override
            public boolean canInsert(ItemStack stack) {
                return stack.getItem() instanceof LockpickItem;
            }
        });

        // Add player inventory slots
        for (int i = 0; i < 3; ++i) {
            for (int j = 0; j < 9; ++j) {
                this.addSlot(new Slot(playerInventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }

        // Add player hotbar slots
        for (int i = 0; i < 9; ++i) {
            this.addSlot(new Slot(playerInventory, i, 8 + i * 18, 142));
        }
    }

    private void onSpecialSlotChange() {
        ItemStack keyStack = specialSlots.getStack(KEY_SLOT);
        ItemStack lockpickStack = specialSlots.getStack(LOCKPICK_SLOT);

        // Try to unlock with key
        if (!keyStack.isEmpty() && keyStack.getItem() instanceof KeyItem key) {
            if (tryUnlockWithKey(key)) {
                keyStack.decrement(1);
            }
        }

        // Try to lockpick
        if (!lockpickStack.isEmpty() && lockpickStack.getItem() instanceof LockpickItem) {
            // Send lockpicking minigame packet to player
            if (canAttemptLockpick()) {
                // This will be handled via networking
                lockpickStack.decrement(1);
            }
        }
    }

    public LockData getLockData() {
        return lockData;
    }

    public LockType getLockType() {
        return lockData != null ? lockData.getLockType() : null;
    }

    /**
     * Attempts to unlock the chest with a key.
     */
    public boolean tryUnlockWithKey(KeyItem keyItem) {
        if (lockData != null && lockData.getLockType() == keyItem.getLockType()) {
            // Check if the key has the correct lock ID
            UUID keyLockId = KeyItem.getLockId(specialSlots.getStack(KEY_SLOT));
            if (lockData.getLockId() == null || lockData.getLockId().equals(keyLockId)) {
                LockManager.removeLock(chest);
                // Close the screen for the player
                return true;
            }
        }
        return false;
    }

    /**
     * Attempts to use a lockpick on the chest.
     */
    public boolean canAttemptLockpick() {
        return lockData != null && lockData.isLocked();
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return chest.canPlayerUse(player);
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        ItemStack itemStack = ItemStack.EMPTY;
        Slot slot2 = this.slots.get(slot);
        
        if (slot2 != null && slot2.hasStack()) {
            ItemStack itemStack2 = slot2.getStack();
            itemStack = itemStack2.copy();
            
            // Move items between inventory and slots
            if (slot < 2) {
                // Move from special slots to inventory
                if (!this.insertItem(itemStack2, 2, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // Try to move to special slots
                if (itemStack2.getItem() instanceof KeyItem) {
                    if (this.insertItem(itemStack2, KEY_SLOT, KEY_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (itemStack2.getItem() instanceof LockpickItem) {
                    if (this.insertItem(itemStack2, LOCKPICK_SLOT, LOCKPICK_SLOT + 1, false)) {
                        return ItemStack.EMPTY;
                    }
                }
                
                // Move to hotbar or inventory
                if (slot >= 2 && slot < 29) {
                    if (!this.insertItem(itemStack2, 29, this.slots.size(), false)) {
                        return ItemStack.EMPTY;
                    }
                } else if (slot >= 29 && slot < this.slots.size()) {
                    if (!this.insertItem(itemStack2, 2, 29, false)) {
                        return ItemStack.EMPTY;
                    }
                }
            }
            
            if (itemStack2.isEmpty()) {
                slot2.setStack(ItemStack.EMPTY);
            } else {
                slot2.markDirty();
            }
        }
        
        return itemStack;
    }
}
