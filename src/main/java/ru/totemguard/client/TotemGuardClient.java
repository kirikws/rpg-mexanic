package ru.totemguard.client;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import ru.totemguard.client.ClientRegionCache.Region;
import ru.totemguard.network.TotemNetworking;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Client entry point for TotemGuard.
 */
public class TotemGuardClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        KeyBindings.register();
        RegionRenderer.register();
        registerReceivers();
        registerScreens();
    }

    private static void registerScreens() {
        net.minecraft.client.gui.screen.ingame.HandledScreens.register(
                ru.totemguard.inventory.TotemScreenHandlerType.TYPE,
                ru.totemguard.client.screen.TotemScreen::new
        );
    }

    private static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(TotemNetworking.SYNC_BOUNDARIES_PACKET_ID, (client, handler, buf, sender) -> {
            int count = buf.readInt();
            List<Region> regions = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                UUID owner = buf.readUuid();
                boolean isOwner = buf.readBoolean();
                int pointCount = buf.readInt();
                float[][] points = new float[pointCount][2];
                for (int j = 0; j < pointCount; j++) {
                    points[j][0] = buf.readFloat();
                    points[j][1] = buf.readFloat();
                }
                regions.add(new Region(owner, isOwner, points));
            }

            client.execute(() -> ClientRegionCache.update(regions));
        });
    }
}
