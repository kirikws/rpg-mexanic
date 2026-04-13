package ru.totemguard.block;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerFactory;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.inventory.Inventories;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import ru.totemguard.config.TotemConfig;
import ru.totemguard.inventory.TotemScreenHandler;
import ru.totemguard.item.CurrencyItem;
import ru.totemguard.storage.RegionState;

import java.util.UUID;

/**
 * BlockEntity for the Claim Totem.
 * Stores: owner UUID, inventory (1 slot for currency), active state, tick counter.
 */
public class TotemBlockEntity extends BlockEntity implements Inventory, ExtendedScreenHandlerFactory {
    private static final int SLOT_COUNT = 1;

    private UUID owner;
    private final DefaultedList<ItemStack> inventory = DefaultedList.ofSize(SLOT_COUNT, ItemStack.EMPTY);
    private boolean active = true;
    private int tickCounter = 0;

    public TotemBlockEntity(BlockPos pos, BlockState state) {
        super(TotemBlocks.BLOCK_ENTITY_TYPE, pos, state);
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        markDirty();
    }

    public UUID getOwner() {
        return owner;
    }

    public boolean isActive() {
        return active;
    }

    public DefaultedList<ItemStack> getInventory() {
        return inventory;
    }

    // === Inventory Interface ===

    @Override
    public int size() {
        return SLOT_COUNT;
    }

    @Override
    public boolean isEmpty() {
        return inventory.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getStack(int slot) {
        return inventory.get(slot);
    }

    @Override
    public ItemStack removeStack(int slot, int amount) {
        onCurrencyChanged();
        markDirty();
        return Inventories.splitStack(inventory, slot, amount);
    }

    @Override
    public ItemStack removeStack(int slot) {
        onCurrencyChanged();
        markDirty();
        return Inventories.removeStack(inventory, slot);
    }

    @Override
    public void setStack(int slot, ItemStack stack) {
        inventory.set(slot, stack);
        onCurrencyChanged();
        markDirty();
    }

    @Override
    public boolean canPlayerUse(PlayerEntity player) {
        if (world.getBlockEntity(pos) != this) return false;
        return player.squaredDistanceTo(
                (double) pos.getX() + 0.5,
                (double) pos.getY() + 0.5,
                (double) pos.getZ() + 0.5) <= 64.0;
    }

    @Override
    public void clear() {
        inventory.clear();
        onCurrencyChanged();
        markDirty();
    }

    @Override
    public void markDirty() {
        super.markDirty();
    }

    // === ScreenHandler ===
    public static <T extends BlockEntity> void serverTick(World world, BlockPos pos, BlockState state, T blockEntity) {
        if (!(blockEntity instanceof TotemBlockEntity self)) return;
        if (!self.active) return;

        self.tickCounter++;
        long drainInterval = TotemConfig.get().drain_interval_ticks;

        if (self.tickCounter >= drainInterval) {
            self.tickCounter = 0;

            ItemStack currency = self.inventory.get(0);
            if (!currency.isEmpty()) {
                currency.decrement(1);
                if (currency.isEmpty()) {
                    self.inventory.set(0, ItemStack.EMPTY);
                    self.setActive(false, world, pos, state);
                }
                self.markDirty();
            } else {
                self.setActive(false, world, pos, state);
            }
        }
    }

    private void setActive(boolean active, World world, BlockPos pos, BlockState state) {
        this.active = active;
        BlockState newState = state.with(ClaimTotemBlock.ACTIVE, active);
        if (!state.equals(newState)) {
            world.setBlockState(pos, newState);
        }
        if (world != null && !world.isClient && owner != null) {
            onCurrencyChanged();
        }
    }

    private void onCurrencyChanged() {
        // Отладка: проверяем условия
        if (world == null) { 
            System.out.println("[TotemGuard] World is null!"); 
            return; 
        }
        if (world.isClient) return;
        if (owner == null) { 
            System.out.println("[TotemGuard] Owner is null for totem at " + pos); 
            return; 
        }

        ItemStack item = inventory.get(0);
        int baseRadius = TotemConfig.get().base_radius;
        int radius = baseRadius;
        String currencyName = "Нет валюты";

        if (!item.isEmpty()) {
            if (item.getItem() instanceof CurrencyItem currency) {
                radius = (int) Math.round(baseRadius * currency.getRadiusMultiplier());
                currencyName = item.getName().getString();
            } else {
                currencyName = item.getName().getString();
                // Если предмет не валюта, радиус базовый (или 0, если хотите запретить)
                radius = baseRadius; 
            }
        }

        // --- ЖЕСТКОЕ ОБНОВЛЕНИЕ ---
        RegionState state = RegionState.getOrCreate(world.getServer());
        
        // Удаляем старый тотем (по позиции)
        boolean removed = state.removeTotem(pos);
        
        // Добавляем новый с обновленным радиусом
        state.addTotem(pos, owner, radius);
        
        // Сохраняем
        state.save();
        // --------------------------

        // Сообщение игроку
        ServerPlayerEntity ownerPlayer = world.getServer().getPlayerManager().getPlayer(owner);
        if (ownerPlayer != null) {
            ownerPlayer.sendMessage(
                    Text.literal("⚡ Тотем обновлён!").formatted(net.minecraft.util.Formatting.GREEN),
                    false
            );
            ownerPlayer.sendMessage(
                    Text.literal("   Предмет: " + currencyName).formatted(net.minecraft.util.Formatting.YELLOW),
                    false
            );
            ownerPlayer.sendMessage(
                    Text.literal("   Радиус: " + radius + " блоков").formatted(net.minecraft.util.Formatting.AQUA),
                    false
            );
            if (removed) {
                ownerPlayer.sendMessage(
                        Text.literal("   (Старая запись удалена, новая создана)").formatted(net.minecraft.util.Formatting.GRAY),
                        false
                );
            }
            ownerPlayer.sendMessage(
                    Text.literal("⚠ Границы на экране (V) могут не измениться сразу.").formatted(net.minecraft.util.Formatting.RED),
                    false
            );
            ownerPlayer.sendMessage(
                    Text.literal("Нажмите V дважды, чтобы обновить линии.").formatted(net.minecraft.util.Formatting.YELLOW),
                    false
            );
        } else {
            System.out.println("[TotemGuard] Owner player not found (maybe offline): " + owner);
        }
    }

    // === ScreenHandler ===

    @Override
    public Text getDisplayName() {
        return Text.translatable("block.totemguard.claim_totem");
    }

    @Nullable
    @Override
    public ScreenHandler createMenu(int syncId, PlayerInventory playerInventory, PlayerEntity player) {
        return new TotemScreenHandler(syncId, playerInventory, this);
    }

    @Override
    public void writeScreenOpeningData(ServerPlayerEntity player, PacketByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    // === NBT ===

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);
        if (owner != null) {
            nbt.putUuid("owner", owner);
        }
        nbt.putBoolean("active", active);
        nbt.putInt("tickCounter", tickCounter);
        Inventories.writeNbt(nbt, inventory);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.containsUuid("owner")) {
            owner = nbt.getUuid("owner");
        }
        active = nbt.getBoolean("active");
        tickCounter = nbt.getInt("tickCounter");
        Inventories.readNbt(nbt, inventory);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        return createNbt();
    }
}
