package ru.kirikws.reputationfabric.common.network.packet;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import ru.kirikws.reputationfabric.ReputationFabricMod;
import ru.kirikws.reputationfabric.common.network.ReputationNetworking;

/**
 * Server-to-Client packet for syncing karma value to the client.
 * Sent whenever a player's karma changes.
 */
public class SyncKarmaS2CPacket {
    private final int karma;

    public SyncKarmaS2CPacket(int karma) {
        this.karma = karma;
    }

    /**
     * Sends this packet to a specific player.
     *
     * @param player the player to send the packet to
     * @param karma the karma value to sync
     */
    public static void send(ServerPlayerEntity player, int karma) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(karma);
        ServerPlayNetworking.send(player, ReputationNetworking.SYNC_KARMA, buf);
    }

    /**
     * Writes the packet data to a buffer.
     *
     * @param buf the buffer to write to
     */
    public void write(PacketByteBuf buf) {
        buf.writeInt(karma);
    }

    /**
     * Reads the packet data from a buffer.
     *
     * @param buf the buffer to read from
     * @return the karma value
     */
    public static int read(PacketByteBuf buf) {
        return buf.readInt();
    }
}
