package ru.kirikws.reputationfabric.common.network.packet;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import ru.kirikws.reputationfabric.ReputationFabricMod;
import ru.kirikws.reputationfabric.api.ReputationAPIs;
import ru.kirikws.reputationfabric.common.network.ReputationNetworking;

/**
 * Client-to-Server packet for requesting a mode switch.
 * Sent when the player presses the mode toggle key.
 */
public class SwitchModeC2SPacket {
    /**
     * Handles the received mode switch request.
     *
     * @param server the server instance
     * @param player the player who sent the request
     * @param buf the packet buffer
     * @param sender the packet sender
     */
    public static void receive(MinecraftServer server, ServerPlayerEntity player,
                               ServerPlayNetworkHandler handler, PacketByteBuf buf,
                               net.fabricmc.fabric.api.networking.v1.PacketSender responseSender) {
        server.execute(() -> {
            boolean success = ReputationAPIs.requestModeSwitch(player);
            if (!success) {
                player.sendMessage(net.minecraft.text.Text.translatable("message." + ReputationFabricMod.MOD_ID + ".mode_switch_failed"), true);
            }
        });
    }

    /**
     * Sends this packet to the server.
     * This is a client-side method.
     */
    public static void send() {
        PacketByteBuf buf = PacketByteBufs.create();
        ClientPlayNetworking.send(ReputationNetworking.SWITCH_MODE, buf);
    }
}
