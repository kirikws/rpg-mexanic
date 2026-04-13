package ru.kirikws.reputationfabric.common.network.packet;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import ru.kirikws.reputationfabric.ReputationFabricMod;
import ru.kirikws.reputationfabric.common.component.data.PlayerData;
import ru.kirikws.reputationfabric.common.network.ReputationNetworking;

/**
 * Server-to-Client packet for syncing player mode to the client.
 * Sent whenever a player's mode (stance) changes.
 */
public class SyncModeS2CPacket {
    private final PlayerData.PlayerMode mode;

    public SyncModeS2CPacket(PlayerData.PlayerMode mode) {
        this.mode = mode;
    }

    public static void send(ServerPlayerEntity player, PlayerData.PlayerMode mode) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeString(mode.name(), 32);
        ServerPlayNetworking.send(player, ReputationNetworking.SYNC_MODE, buf);
    }

    public static PlayerData.PlayerMode read(PacketByteBuf buf) {
        return PlayerData.PlayerMode.valueOf(buf.readString(32));
    }
}
