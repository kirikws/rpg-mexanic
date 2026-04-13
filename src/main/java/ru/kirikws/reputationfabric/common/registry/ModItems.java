package ru.kirikws.reputationfabric.common.registry;

import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import ru.kirikws.reputationfabric.ReputationFabricMod;
import ru.kirikws.reputationfabric.common.item.KarmaCompassItem;
import ru.kirikws.reputationfabric.common.item.ModTradeOffers;

/**
 * Registers all items and trades provided by the mod.
 */
public class ModItems {
    public static final KarmaCompassItem KARMA_COMPASS = register(
            "karma_compass",
            new KarmaCompassItem(new Item.Settings().maxCount(1))
    );

    private static <T extends Item> T register(String name, T item) {
        return Registry.register(Registries.ITEM, new Identifier(ReputationFabricMod.MOD_ID, name), item);
    }

    /**
     * Registers items with creative tabs and initializes trade offers.
     */
    public static void registerItems() {
        // Add karma compass to functional items tab
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(KARMA_COMPASS);
        });

        // Initialize villager trades
        ModTradeOffers.registerTrades();
    }
}
