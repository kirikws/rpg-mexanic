package ru.kirikws.reputationfabric;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ru.kirikws.reputationfabric.common.event.ReputationEventHandler;
import ru.kirikws.reputationfabric.common.network.ReputationNetworking;
import ru.kirikws.reputationfabric.common.registry.ModCommands;
import ru.kirikws.reputationfabric.config.ReputationConfig;

/**
 * Main entry point for the Reputation Fabric mod.
 * Initializes configuration, networking, events, and commands.
 */
public class ReputationFabricMod implements ModInitializer {
    public static final String MOD_ID = "reputation-fabric";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Initializing Reputation Fabric mod...");

        // Initialize configuration
        ReputationConfig.load();

        // Register networking packets
        ReputationNetworking.registerPackets();

        // Register event handlers
        ReputationEventHandler.registerEvents();

        // Register combat rules
        ru.kirikws.reputationfabric.common.event.CombatRuleHandler.register();

        // Register commands
        ModCommands.register();

        // Register items and trades
        ru.kirikws.reputationfabric.common.registry.ModItems.registerItems();

        // Register lock system items
        ru.kirikws.reputationfabric.common.locks.ModLockItems.registerItems();

        // Initialize TotemGuard
        ru.totemguard.TotemGuardMod.init();
        ru.totemguard.recipe.TotemRecipes.register();

        LOGGER.info("Reputation Fabric mod initialized successfully.");
    }
}
