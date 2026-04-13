package ru.totemguard.inventory;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.math.BlockPos;
import ru.totemguard.block.TotemBlockEntity;

/**
 * ScreenHandler for the Claim Totem.
 * Has 1 slot for currency + player inventory.
 */
public class TotemScreenHandler extends ScreenHandler {
    private final Inventory inventory;
    private final TotemBlockEntity blockEntity;

    // Factory for ExtendedScreenHandlerType
    public static final ExtendedScreenHandlerType.ExtendedFactory<TotemScreenHandler> BUFFER_CODEC =
            (syncId, playerInventory, buf) -> {
                BlockPos pos = buf.readBlockPos();
                BlockEntity be = playerInventory.player.getWorld().getBlockEntity(pos);
                if (be instanceof TotemBlockEntity totemBE) {
                    return new TotemScreenHandler(syncId, playerInventory, totemBE);
                }
                // Fallback
                return new TotemScreenHandler(syncId, playerInventory, null);
            };

    public TotemScreenHandler(int syncId, PlayerInventory playerInventory) {
        this(syncId, playerInventory, null);
    }

    public TotemScreenHandler(int syncId, PlayerInventory playerInventory, TotemBlockEntity blockEntity) {
        super(TotemScreenHandlerType.TYPE, syncId);
        this.blockEntity = blockEntity;
        this.inventory = (blockEntity != null) ? blockEntity : new net.minecraft.inventory.SimpleInventory(1);

        // Currency slot (centered)
        this.addSlot(new Slot(inventory, 0, 80, 35));

        // Player inventory (rows 1-3)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }

        // Hotbar (row 4)
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        if (blockEntity != null) {
            return blockEntity.canPlayerUse(player);
        }
        return true;
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int index) {
        ItemStack newStack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);

        if (slot.hasStack()) {
            ItemStack originalStack = slot.getStack();
            newStack = originalStack.copy();

            if (index == 0) {
                // Currency slot -> player inventory
                if (!this.insertItem(originalStack, 1, 37, true)) {
                    return ItemStack.EMPTY;
                }
                slot.onQuickTransfer(originalStack, newStack);
            } else {
                // Player inventory -> currency slot
                if (!this.insertItem(originalStack, 0, 1, false)) {
                    return ItemStack.EMPTY;
                }
            }

            if (originalStack.isEmpty()) {
                slot.setStack(ItemStack.EMPTY);
            } else {
                slot.markDirty();
            }
        }

        return newStack;
    }
}
