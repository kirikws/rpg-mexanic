package ru.totemguard.network.packet;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import ru.totemguard.TotemGuardMod;
import ru.totemguard.config.TotemConfig;
import ru.totemguard.geometry.CuboidRegion;
import ru.totemguard.geometry.UnionedRegion;
import ru.totemguard.network.TotemNetworking;
import ru.totemguard.storage.RegionState;

import java.util.*;

/**
 * Server handler for RequestBoundariesC2SPacket.
 * Collects regions near the player and sends them back.
 */
public class RequestBoundariesHandler {

    public static void receive(MinecraftServer server, ServerPlayerEntity player, ServerPlayNetworkHandler handler, PacketByteBuf buf, PacketSender responseSender) {
        server.execute(() -> {
            RegionState regions = RegionState.getOrCreate(server);
            List<RegionData> nearbyRegions = new ArrayList<>();
            int viewRadius = TotemConfig.get().boundary_view_radius;

            for (UUID owner : regions.getAllOwners()) {
                UnionedRegion region = regions.getRegion(owner);
                if (region == null) continue;

                // Check if any component is within radius
                boolean inRange = false;
                for (CuboidRegion comp : region.getComponents()) {
                    BlockPos center = comp.getCenterXZ();
                    double dist = player.getPos().squaredDistanceTo(center.getX(), player.getY(), center.getZ());
                    if (dist <= viewRadius * viewRadius) {
                        inRange = true;
                        break;
                    }
                }

                if (inRange) {
                    List<Vec3d> hull = region.getConvexHull();
                    List<float[]> points = new ArrayList<>();
                    for (Vec3d v : hull) {
                        points.add(new float[]{(float) v.x, (float) v.z});
                    }
                    nearbyRegions.add(new RegionData(owner, points));
                }
            }

            // Send response
            PacketByteBuf response = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
            response.writeInt(nearbyRegions.size());
            UUID playerUuid = player.getUuid();

            for (RegionData data : nearbyRegions) {
                response.writeUuid(data.owner);
                response.writeBoolean(data.owner.equals(playerUuid)); // Is owner?
                response.writeInt(data.points.size());
                for (float[] p : data.points) {
                    response.writeFloat(p[0]);
                    response.writeFloat(p[1]);
                }
            }

            ServerPlayNetworking.send(player, TotemNetworking.SYNC_BOUNDARIES_PACKET_ID, response);
        });
    }

    private static class RegionData {
        final UUID owner;
        final List<float[]> points;

        RegionData(UUID owner, List<float[]> points) {
            this.owner = owner;
            this.points = points;
        }
    }
}
