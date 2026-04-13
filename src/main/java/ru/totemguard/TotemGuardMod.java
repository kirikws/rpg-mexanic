package ru.totemguard;

import net.fabricmc.api.ModInitializer;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.totemguard.block.TotemBlocks;
import ru.totemguard.config.TotemConfig;
import ru.totemguard.item.CurrencyItem;
import ru.totemguard.item.TotemItems;

/**
 * Main entry point for TotemGuard functionality.
 * Integrated into the reputation-fabric mod project.
 */
public class TotemGuardMod {
    public static final String MOD_ID = "totemguard";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        LOGGER.info("Initializing TotemGuard...");

        // Load config
        TotemConfig.load();

        // Register blocks and block entities
        TotemBlocks.init();

        // Register screen handlers
        ru.totemguard.inventory.TotemScreenHandlerType.register();

        // Register items
        TotemItems.init();

        // Add items to creative tabs
        registerCreativeTabEntries();

        // Register networking
        ru.totemguard.network.TotemNetworking.register();

        // Register protection handlers
        ru.totemguard.protection.ProtectionHandler.register();

        // Register guest tracker (timer + karma drain)
        ru.totemguard.guest.GuestTracker.register();

        // Register commands (via callback)
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess, environment) -> {
                        ru.totemguard.command.TotemCommands.register(dispatcher);
                        ru.totemguard.command.TotemPlaceCommand.register(dispatcher);
                }
        );

        LOGGER.info("TotemGuard initialized.");
    }

    private static void registerCreativeTabEntries() {
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.FUNCTIONAL).register(entries -> {
            entries.add(TotemBlocks.CLAIM_TOTEM);
            entries.add(TotemItems.COPPER_COIN);
            entries.add(TotemItems.SILVER_SIGIL);
            entries.add(TotemItems.GOLDEN_IDOL);
        });
    }
}
