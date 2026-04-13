package ru.totemguard.item;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import ru.totemguard.TotemGuardMod;
import ru.totemguard.config.TotemConfig;

/**
 * Registry for all TotemGuard items (currency).
 */
public class TotemItems {
    public static final CurrencyItem COPPER_COIN = register(
            "copper_coin",
            new CurrencyItem(TotemConfig.get().copper_multiplier, new Item.Settings())
    );

    public static final CurrencyItem SILVER_SIGIL = register(
            "silver_sigil",
            new CurrencyItem(TotemConfig.get().silver_multiplier, new Item.Settings())
    );

    public static final CurrencyItem GOLDEN_IDOL = register(
            "golden_idol",
            new CurrencyItem(TotemConfig.get().gold_multiplier, new Item.Settings())
    );

    /**
     * Must be called during mod initialization.
     */
    public static void init() {
        // Items are registered inline via static initializers
    }

    private static <T extends Item> T register(String name, T item) {
        return Registry.register(Registries.ITEM, new Identifier(TotemGuardMod.MOD_ID, name), item);
    }
}
