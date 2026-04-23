package ru.kirikws.reputationfabric.common.locks.mixin;

import net.minecraft.block.ChestBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import ru.kirikws.reputationfabric.ReputationFabricMod;
import ru.kirikws.reputationfabric.common.locks.LockData;
import ru.kirikws.reputationfabric.common.locks.LockManager;
import ru.kirikws.reputationfabric.common.locks.LockableChest;
import ru.kirikws.reputationfabric.common.locks.LockpickMinigame;
import ru.kirikws.reputationfabric.common.locks.ModLockItems;
import ru.kirikws.reputationfabric.common.locks.item.KeyItem;
import ru.kirikws.reputationfabric.common.locks.item.LockItem;
import ru.kirikws.reputationfabric.common.locks.item.LockpickItem;

@Mixin(ChestBlock.class)
public class ChestBlockMixin {

    @Inject(method = "onUse", at = @At("HEAD"), cancellable = true)
    private void onChestUse(net.minecraft.block.BlockState state, World world, BlockPos pos,
                            PlayerEntity player, Hand hand, BlockHitResult hit,
                            CallbackInfoReturnable<ActionResult> cir) {
        BlockEntity blockEntity = world.getBlockEntity(pos);
        if (!(blockEntity instanceof ChestBlockEntity chest)) {
            return;
        }

        ItemStack heldStack = player.getStackInHand(hand);
        LockData lockData = LockManager.getLockData(chest);
        boolean isLocked = lockData != null && lockData.isLocked();

        // 1. Если сундук заблокирован и игрок держит КЛЮЧ → открываем (без снятия замка)
        if (isLocked && heldStack.getItem() instanceof KeyItem keyItem) {
            if (world.isClient) {
                cir.setReturnValue(ActionResult.SUCCESS);
                return;
            }

            // Проверяем, подходит ли ключ
            if (keyItem.getLockType() == lockData.getLockType()) {
                java.util.UUID keyLockId = KeyItem.getLockId(heldStack);
                if (lockData.getLockId() == null || lockData.getLockId().equals(keyLockId)) {
                    // Ключ подходит - снимаем блокировку и открываем сундук
                    player.sendMessage(Text.literal("🔑 Замок открыт!").formatted(Formatting.GREEN), true);
                    // Открываем интерфейс сундука
                    net.minecraft.screen.NamedScreenHandlerFactory screenHandlerFactory = state.createScreenHandlerFactory(world, pos);
                    if (screenHandlerFactory != null) {
                        player.openHandledScreen(screenHandlerFactory);
                    }
                    cir.setReturnValue(ActionResult.SUCCESS);
                    return;
                }
            }

            player.sendMessage(Text.literal("🔑 Ключ не подходит к этому замку!").formatted(Formatting.RED), true);
            cir.setReturnValue(ActionResult.FAIL);
            return;
        }

        // 2. Если сундук НЕ заблокирован и игрок держит ЗАМОК → установка по ПКМ
        if (!isLocked && heldStack.getItem() instanceof LockItem lockItem) {
            if (world.isClient) {
                cir.setReturnValue(ActionResult.SUCCESS);
                return;
            }

            ReputationFabricMod.LOGGER.info("[ChestBlockMixin] Применяем замок типа: {}", lockItem.getLockType());
            LockManager.applyLock(chest, lockItem.getLockType());
            heldStack.decrement(1);

            LockData newLockData = LockManager.getLockData(chest);
            if (newLockData != null && newLockData.getLockId() != null) {
                ItemStack keyStack = new ItemStack(ModLockItems.getKeyForType(lockItem.getLockType()));
                KeyItem.setLockId(keyStack, newLockData.getLockId());
                if (!player.getInventory().insertStack(keyStack)) {
                    player.dropItem(keyStack, false);
                }
                player.sendMessage(Text.literal("🔒 Замок установлен! Ключ получен.").formatted(Formatting.GREEN), true);
            }
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        // 2.5. Если сундук заблокирован и игрок держит ОТМЫЧКУ → запускаем мини-игру
        if (isLocked && heldStack.getItem() instanceof LockpickItem) {
            if (world.isClient) {
                cir.setReturnValue(ActionResult.SUCCESS);
                return;
            }

            // Проверяем, не играет ли игрок уже
            if (LockpickMinigame.isPlaying(player.getUuid())) {
                player.sendMessage(Text.literal("Вы уже взламываете замок!").formatted(Formatting.YELLOW), true);
                cir.setReturnValue(ActionResult.FAIL);
                return;
            }

            // Удаляем одну отмычку
            heldStack.decrement(1);

            // Запускаем мини-игру
            LockpickMinigame.startMinigame(player, chest, lockData);
            cir.setReturnValue(ActionResult.SUCCESS);
            return;
        }

        // 3. Если сундук заблокирован, но игрок не использует ключ/замок/отмычку → блокируем
        if (isLocked) {
            if (world.isClient) {
                cir.setReturnValue(ActionResult.SUCCESS);
                return;
            }
            player.sendMessage(Text.literal("🔒 Сундук заперт! Используйте ключ или отмычку.").formatted(Formatting.RED), true);
            cir.setReturnValue(ActionResult.FAIL);
            return;
        }

        // 4. Обычный сундук → стандартное открытие (ничего не делаем, пусть работает оригинальный код)
    }
}
