package ru.kirikws.reputationfabric.client.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.util.math.Vec3d;
import ru.kirikws.reputationfabric.ReputationFabricMod;
import ru.kirikws.reputationfabric.client.render.CompassHudOverlay;
import ru.kirikws.reputationfabric.common.component.data.PlayerData;
import ru.kirikws.reputationfabric.common.network.ReputationNetworking;

/**
 * Client-side networking receiver registration.
 */
public class ClientNetworking {
    public static void registerReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(ReputationNetworking.SYNC_KARMA, (client, handler, buf, sender) -> {
            int karma = buf.readInt();
            client.execute(() -> {
                KarmaStateHolder.setKarma(karma);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ReputationNetworking.SYNC_MODE, (client, handler, buf, sender) -> {
            PlayerData.PlayerMode mode = PlayerData.PlayerMode.valueOf(buf.readString(32));
            client.execute(() -> {
                ModeStateHolder.setMode(mode);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(ReputationNetworking.COMPASS_TARGET, (client, handler, buf, sender) -> {
            Vec3d targetPos = new Vec3d(buf.readDouble(), buf.readDouble(), buf.readDouble());
            int durationTicks = buf.readInt();
            client.execute(() -> {
                CompassHudOverlay.setTarget(targetPos, durationTicks);
            });
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            KarmaStateHolder.setKarma(0);
            ModeStateHolder.setMode(PlayerData.PlayerMode.PASSIVE);
            CompassHudOverlay.clear();
        });

        ReputationFabricMod.LOGGER.debug("Client networking receivers registered");
    }

    public static class KarmaStateHolder {
        private static int karma = 0;

        public static int getKarma() { return karma; }
        public static void setKarma(int value) { karma = value; }
        public static boolean hasBadKarma(int threshold) { return karma <= threshold; }
        public static boolean hasGoodKarma(int threshold) { return karma >= threshold; }
    }

    public static class ModeStateHolder {
        private static PlayerData.PlayerMode mode = PlayerData.PlayerMode.PASSIVE;

        public static PlayerData.PlayerMode getMode() { return mode; }
        public static void setMode(PlayerData.PlayerMode value) { mode = value; }
        public static boolean isPassive() { return mode == PlayerData.PlayerMode.PASSIVE; }
        public static boolean isPvP() { return mode == PlayerData.PlayerMode.PVP; }
    }
}
