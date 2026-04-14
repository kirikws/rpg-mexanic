package ru.kirikws.reputationfabric.client.locks;

import net.fabricmc.fabric.api.client.screenhandler.v1.ScreenRegistry;
import ru.kirikws.reputationfabric.client.locks.screen.LockedChestScreen;
import ru.kirikws.reputationfabric.common.locks.screen.ReputationFabricScreenHandlers;

/**
 * Client-side initialization for lock system screens.
 */
public class LockSystemClient {
    public static void init() {
        ScreenRegistry.register(
                ReputationFabricScreenHandlers.LOCKED_CHEST_HANDLER,
                LockedChestScreen::new
        );
    }
}
