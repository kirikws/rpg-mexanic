package ru.kirikws.reputationfabric.client.event;

import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import ru.kirikws.reputationfabric.client.registry.ModKeyBindings;
import ru.kirikws.reputationfabric.client.render.CompassHudOverlay;
import ru.kirikws.reputationfabric.client.render.ModeHudOverlay;
import ru.kirikws.reputationfabric.common.network.packet.SwitchModeC2SPacket;

/**
 * Client-side event registration for key bindings and HUD rendering.
 */
public class ClientEventHandler {
    public static void registerEvents() {
        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null) return;
            while (ModKeyBindings.modeToggleKey.wasPressed()) {
                SwitchModeC2SPacket.send();
            }
        });

        HudRenderCallback.EVENT.register((drawContext, tickDelta) -> {
            CompassHudOverlay.render(drawContext);
            ModeHudOverlay.render(drawContext);
        });
    }
}
