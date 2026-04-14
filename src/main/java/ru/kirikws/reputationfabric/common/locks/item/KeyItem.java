package ru.kirikws.reputationfabric.common.locks.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;
import ru.kirikws.reputationfabric.common.locks.LockType;

import java.util.List;
import java.util.UUID;

/**
 * Key item that can open a specific lock type and specific lock ID.
 */
public class KeyItem extends Item {
    private static final String LOCK_ID_KEY = "LockId";
    private final LockType lockType;

    public KeyItem(LockType lockType, Settings settings) {
        super(settings);
        this.lockType = lockType;
    }

    public LockType getLockType() {
        return lockType;
    }

    public static void setLockId(ItemStack stack, UUID lockId) {
        NbtCompound nbt = stack.getOrCreateNbt();
        nbt.putUuid(LOCK_ID_KEY, lockId);
    }

    @Nullable
    public static UUID getLockId(ItemStack stack) {
        NbtCompound nbt = stack.getNbt();
        if (nbt != null && nbt.contains(LOCK_ID_KEY)) {
            return nbt.getUuid(LOCK_ID_KEY);
        }
        return null;
    }

    @Override
    public Text getName(ItemStack stack) {
        UUID lockId = getLockId(stack);
        if (lockId != null) {
            return Text.translatable("item.reputation-fabric.key." + lockType.asString() + ".unique");
        }
        return Text.translatable("item.reputation-fabric.key." + lockType.asString());
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable net.minecraft.world.World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.reputation-fabric.key.tooltip").formatted(Formatting.GRAY));
        tooltip.add(Text.literal("» ").append(Text.translatable(lockType.getTranslationKey()).formatted(Formatting.GREEN)).formatted(Formatting.DARK_GRAY));
        UUID lockId = getLockId(stack);
        if (lockId != null) {
            tooltip.add(Text.literal("Unique ID: " + lockId.toString().substring(0, 8) + "...").formatted(Formatting.YELLOW));
        } else {
            tooltip.add(Text.literal("Generic key").formatted(Formatting.YELLOW));
        }
    }
}
