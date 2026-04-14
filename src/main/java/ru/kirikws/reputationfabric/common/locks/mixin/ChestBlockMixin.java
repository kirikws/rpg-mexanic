package ru.kirikws.reputationfabric.common.locks.mixin;

import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.kirikws.reputationfabric.common.locks.LockData;
import ru.kirikws.reputationfabric.common.locks.LockManager;
import ru.kirikws.reputationfabric.common.locks.ModLockItems;
import ru.kirikws.reputationfabric.common.locks.item.KeyItem;
import ru.kirikws.reputationfabric.common.locks.item.LockItem;

/**
 * Mixin to intercept chest opening and show lock screen if chest is locked.
 */
@Mixin(ChestBlock.class)
public class ChestBlockMixin {
    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void onChestUse(net.minecraft.block.BlockState state, World world, BlockPos pos,
                            PlayerEntity player, net.minecraft.util.Hand hand, BlockHitResult hit,
                            CallbackInfoReturnable<ActionResult> cir) {
        if (!world.isClient) {
            net.minecraft.block.entity.BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof ChestBlockEntity chest) {
            // Check if player is sneaking and holding a lock item
            ItemStack mainHand = player.getMainHandStack();
            ItemStack offHand = player.getOffHandStack();
            LockItem lockItem = null;
            ItemStack itemToDecrement = null;
            if (mainHand.getItem() instanceof LockItem) {
                lockItem = (LockItem) mainHand.getItem();
                itemToDecrement = mainHand;
            } else if (offHand.getItem() instanceof LockItem) {
                lockItem = (LockItem) offHand.getItem();
                itemToDecrement = offHand;
            }
            if (player.isSneaking() && lockItem != null && !LockManager.isLocked(chest)) {
                player.sendMessage(Text.literal("Applying lock... Sneaking: " + player.isSneaking() + ", LockItem: " + (lockItem != null) + ", Locked: " + LockManager.isLocked(chest)), true);
                // Apply the lock
                LockManager.applyLock(chest, lockItem.getLockType());
                itemToDecrement.decrement(1);

                    // Get the lock data and create a key
                    LockData lockData = LockManager.getLockData(chest);
                    if (lockData != null && lockData.getLockId() != null) {
                        ItemStack keyStack = new ItemStack(ModLockItems.getKeyForType(lockItem.getLockType()));
                        KeyItem.setLockId(keyStack, lockData.getLockId());
                        if (!player.getInventory().insertStack(keyStack)) {
                            player.dropItem(keyStack, false);
                        }
                        player.sendMessage(Text.literal("Lock applied and key received!").formatted(Formatting.GREEN), true);
                    }
                    cir.setReturnValue(ActionResult.SUCCESS);
                    return;
                }

                if (LockManager.isLocked(chest)) {
                    // Open locked chest screen instead
                    NamedScreenHandlerFactory screenHandlerFactory =
                            new ru.kirikws.reputationfabric.common.locks.LockedChestScreenHandlerFactory(chest, pos);
                    player.openHandledScreen(screenHandlerFactory);
                    cir.setReturnValue(ActionResult.CONSUME);
                }
            }
        }
    }
}
