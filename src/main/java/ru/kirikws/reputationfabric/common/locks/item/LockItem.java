package ru.kirikws.reputationfabric.common.locks.item;

import net.minecraft.block.BlockState;
import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import ru.kirikws.reputationfabric.common.locks.LockManager;
import ru.kirikws.reputationfabric.common.locks.LockType;

/**
 * Lock item that can be applied to chests by right-clicking.
 */
public class LockItem extends Item {
    private final LockType lockType;

    public LockItem(LockType lockType, Settings settings) {
        super(settings);
        this.lockType = lockType;
    }

    public LockType getLockType() {
        return lockType;
    }

    // Lock application is now handled in ChestBlockMixin via Shift + click

    @Override
    public Text getName() {
        return Text.translatable("item.reputation-fabric.lock." + lockType.asString());
    }
}
