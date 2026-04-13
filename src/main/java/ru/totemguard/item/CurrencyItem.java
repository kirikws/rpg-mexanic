package ru.totemguard.item;

import net.minecraft.item.Item;

/**
 * Base class for TotemGuard currency items.
 */
public class CurrencyItem extends Item {
    private final double radiusMultiplier;

    public CurrencyItem(double radiusMultiplier, Settings settings) {
        super(settings);
        this.radiusMultiplier = radiusMultiplier;
    }

    public double getRadiusMultiplier() {
        return radiusMultiplier;
    }
}
