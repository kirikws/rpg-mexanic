package ru.totemguard.network;

import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import ru.totemguard.TotemGuardMod;
import ru.totemguard.network.packet.RequestBoundariesHandler;

/**
 * Defines networking packets and registers receivers.
 */
public class TotemNetworking {
    public static final Identifier REQUEST_BOUNDARIES_PACKET_ID = new Identifier(TotemGuardMod.MOD_ID, "request_boundaries");
    public static final Identifier SYNC_BOUNDARIES_PACKET_ID = new Identifier(TotemGuardMod.MOD_ID, "sync_boundaries");

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(REQUEST_BOUNDARIES_PACKET_ID, RequestBoundariesHandler::receive);
        TotemGuardMod.LOGGER.info("TotemGuard networking registered.");
    }
}
