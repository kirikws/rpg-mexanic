package ru.kirikws.reputationfabric.common.locks.item;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Lockpick item used in the lockpicking minigame.
 * Single-use item that breaks after attempting to pick a lock.
 */
public class LockpickItem extends Item {
    public LockpickItem(Settings settings) {
        super(settings);
    }

    @Override
    public Text getName() {
        return Text.translatable("item.reputation-fabric.lockpick");
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable net.minecraft.world.World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("item.reputation-fabric.lockpick.tooltip").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("item.reputation-fabric.lockpick.tooltip2").formatted(Formatting.DARK_RED));
    }
}
