package ru.kirikws.reputationfabric.common.locks;

import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import ru.kirikws.reputationfabric.common.locks.screen.LockedChestScreenHandler;

/**
 * Factory for creating locked chest screen handlers.
 */
public class LockedChestScreenHandlerFactory implements NamedScreenHandlerFactory {
    private final ChestBlockEntity chest;
    private final BlockPos pos;

    public LockedChestScreenHandlerFactory(ChestBlockEntity chest, BlockPos pos) {
        this.chest = chest;
        this.pos = pos;
    }

    @Override
    public Text getDisplayName() {
        return Text.translatable("container.reputation-fabric.locked_chest");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory inv, PlayerEntity player) {
        return new LockedChestScreenHandler(syncId, inv, chest, pos);
    }
}
