package ru.kirikws.reputationfabric.client;

import net.fabricmc.api.ClientModInitializer;
import ru.kirikws.reputationfabric.client.event.ClientEventHandler;
import ru.kirikws.reputationfabric.client.network.ClientNetworking;
import ru.kirikws.reputationfabric.client.registry.ModKeyBindings;

/**
 * Client-side entry point for the Reputation Fabric mod.
 * Initializes client-specific features like key bindings, HUD, and rendering.
 */
public class ReputationFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        // Register key bindings
        ModKeyBindings.register();

        // Register client-side networking
        ClientNetworking.registerReceivers();

        // Register client event handlers
        ClientEventHandler.registerEvents();
    }
}
