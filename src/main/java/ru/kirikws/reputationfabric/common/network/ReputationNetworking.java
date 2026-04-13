package ru.kirikws.reputationfabric.common.network;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.util.Identifier;
import ru.kirikws.reputationfabric.ReputationFabricMod;
import ru.kirikws.reputationfabric.common.network.packet.SwitchModeC2SPacket;

/**
 * Registry for all network packet types used by the mod.
 */
public class ReputationNetworking {
    public static final Identifier SWITCH_MODE = new Identifier(ReputationFabricMod.MOD_ID, "switch_mode");
    public static final Identifier SYNC_KARMA = new Identifier(ReputationFabricMod.MOD_ID, "sync_karma");
    public static final Identifier SYNC_MODE = new Identifier(ReputationFabricMod.MOD_ID, "sync_mode");
    public static final Identifier COMPASS_TARGET = new Identifier(ReputationFabricMod.MOD_ID, "compass_target");

    public static void registerPackets() {
        ServerPlayNetworking.registerGlobalReceiver(SWITCH_MODE, SwitchModeC2SPacket::receive);
        ReputationFabricMod.LOGGER.debug("Registered networking packets");
    }
}
