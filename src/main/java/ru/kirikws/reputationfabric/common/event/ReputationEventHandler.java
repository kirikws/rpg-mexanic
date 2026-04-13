package ru.kirikws.reputationfabric.common.event;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import ru.kirikws.reputationfabric.ReputationFabricMod;
import ru.kirikws.reputationfabric.api.ReputationAPIs;
import ru.kirikws.reputationfabric.common.component.ReputationComponents;
import ru.kirikws.reputationfabric.common.component.data.PlayerData.PlayerMode;
import ru.kirikws.reputationfabric.common.network.ReputationNetworking;
import ru.kirikws.reputationfabric.common.network.packet.SyncKarmaS2CPacket;
import ru.kirikws.reputationfabric.common.network.packet.SyncModeS2CPacket;
import ru.kirikws.reputationfabric.config.ReputationConfig;

/**
 * Registers all server-side event handlers for the mod.
 */
public class ReputationEventHandler {
    public static void registerEvents() {
        registerKarmaEvents();
        registerPacketHandlers();
        registerPlayerJoinEvents();
        registerServerTickEvents();
    }

    private static void registerKarmaEvents() {
        AttackEntityCallback.EVENT.register((attacker, world, hand, victim, hitResult) -> {
            if (!(attacker instanceof ServerPlayerEntity serverAttacker)) {
                return ActionResult.PASS;
            }
            if (!(victim instanceof ServerPlayerEntity serverVictim)) {
                return ActionResult.PASS;
            }

            // Apply karma penalty for attacking players with non-negative karma
            if (ReputationAPIs.getKarma(serverVictim) >= 0) {
                int penalty = ReputationConfig.get().karma_damage_penalty;
                ReputationAPIs.modifyKarma(serverAttacker, -penalty);
                serverAttacker.sendMessage(
                        Text.translatable("message." + ReputationFabricMod.MOD_ID + ".karma_damage_penalty", penalty),
                        true
                );
            }

            // Track damage for mode switching cooldown
            ReputationComponents.PLAYER_DATA.get(serverVictim).setLastDamageTakenTime(System.currentTimeMillis());
            return ActionResult.PASS;
        });

        net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents.AFTER_DEATH.register((entity, damageSource) -> {
            if (!(entity instanceof ServerPlayerEntity victim)) return;
            if (!(damageSource.getAttacker() instanceof ServerPlayerEntity killer)) return;

            int karmaChange = ReputationAPIs.calculateKillKarma(killer, victim);
            ReputationAPIs.modifyKarma(killer, karmaChange);

            if (karmaChange < 0) {
                killer.sendMessage(Text.translatable("message." + ReputationFabricMod.MOD_ID + ".karma_kill_penalty", -karmaChange), true);
            } else {
                killer.sendMessage(Text.translatable("message." + ReputationFabricMod.MOD_ID + ".karma_kill_bonus", karmaChange), true);
            }
        });
    }

    private static void registerPacketHandlers() {
        ServerPlayNetworking.registerGlobalReceiver(ReputationNetworking.SWITCH_MODE, (server, player, handler, buf, sender) -> {
            server.execute(() -> {
                boolean success = ReputationAPIs.requestModeSwitch(player);
                if (!success) {
                    player.sendMessage(Text.translatable("message." + ReputationFabricMod.MOD_ID + ".mode_switch_failed"), true);
                } else {
                    player.sendMessage(Text.translatable("message." + ReputationFabricMod.MOD_ID + ".mode_switched",
                            ReputationAPIs.getMode(player).name()), true);
                }
            });
        });
    }

    private static void registerPlayerJoinEvents() {
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            SyncKarmaS2CPacket.send(player, ReputationAPIs.getKarma(player));
            SyncModeS2CPacket.send(player, ReputationAPIs.getMode(player));
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ReputationAPIs.removePlayer(handler.getPlayer());
        });
    }

    private static void registerServerTickEvents() {
        ServerTickEvents.START_SERVER_TICK.register(server -> {
            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                ReputationAPIs.updatePlayerPosition(player);
            }
        });
    }
}
