package ru.kirikws.reputationfabric.common.network.packet;

import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;
import ru.kirikws.reputationfabric.ReputationFabricMod;
import ru.kirikws.reputationfabric.common.network.ReputationNetworking;

/**
 * Server-to-Client packet for sending compass target coordinates.
 * Sent when the karma compass is used and a target is found.
 */
public class CompassTargetS2CPacket {
    private final Vec3d targetPos;
    private final int durationTicks;

    public CompassTargetS2CPacket(Vec3d targetPos, int durationTicks) {
        this.targetPos = targetPos;
        this.durationTicks = durationTicks;
    }

    /**
     * Sends this packet to a specific player.
     *
     * @param player the player to send the packet to
     * @param targetPos the target position
     * @param durationTicks the duration of the compass effect
     */
    public static void send(ServerPlayerEntity player, Vec3d targetPos, int durationTicks) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeDouble(targetPos.x);
        buf.writeDouble(targetPos.y);
        buf.writeDouble(targetPos.z);
        buf.writeInt(durationTicks);
        ServerPlayNetworking.send(player, ReputationNetworking.COMPASS_TARGET, buf);
    }

    /**
     * Writes the packet data to a buffer.
     *
     * @param buf the buffer to write to
     */
    public void write(PacketByteBuf buf) {
        buf.writeDouble(targetPos.x);
        buf.writeDouble(targetPos.y);
        buf.writeDouble(targetPos.z);
        buf.writeInt(durationTicks);
    }

    /**
     * Reads the packet data from a buffer.
     *
     * @param buf the buffer to read from
     * @return array containing [x, y, z, durationTicks]
     */
    public static double[] read(PacketByteBuf buf) {
        return new double[]{
                buf.readDouble(),
                buf.readDouble(),
                buf.readDouble(),
                buf.readInt()
        };
    }
}
