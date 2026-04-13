package ru.kirikws.reputationfabric.common.event;

import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import ru.kirikws.reputationfabric.ReputationFabricMod;
import ru.kirikws.reputationfabric.api.ReputationAPIs;
import ru.kirikws.reputationfabric.common.component.ReputationComponents;
import ru.kirikws.reputationfabric.common.component.data.PlayerData.PlayerMode;

/**
 * Handles combat rules based on player modes.
 * Cancels attacks when either attacker or target is in PASSIVE mode.
 */
public class CombatRuleHandler {
    public static void register() {
        AttackEntityCallback.EVENT.register((attacker, world, hand, victim, hitResult) -> {
            if (!(attacker instanceof ServerPlayerEntity serverAttacker)) {
                return ActionResult.PASS;
            }
            if (!(victim instanceof ServerPlayerEntity serverVictim)) {
                return ActionResult.PASS;
            }

            PlayerMode attackerMode = ReputationAPIs.getMode(serverAttacker);
            PlayerMode victimMode = ReputationAPIs.getMode(serverVictim);

            // Check if attacker is in PASSIVE mode
            if (attackerMode == PlayerMode.PASSIVE) {
                serverAttacker.sendMessage(
                        Text.translatable("message." + ReputationFabricMod.MOD_ID + ".attack_blocked_passive"),
                        true
                );
                return ActionResult.FAIL;
            }

            // Check if victim is in PASSIVE mode
            if (victimMode == PlayerMode.PASSIVE) {
                serverAttacker.sendMessage(
                        Text.translatable("message." + ReputationFabricMod.MOD_ID + ".attack_blocked_target_passive"),
                        true
                );
                return ActionResult.FAIL;
            }

            return ActionResult.PASS;
        });
    }
}
