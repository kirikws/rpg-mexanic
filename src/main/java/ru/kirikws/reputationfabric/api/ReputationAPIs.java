package ru.kirikws.reputationfabric.api;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.Vec3d;
import ru.kirikws.reputationfabric.common.component.ReputationComponents;
import ru.kirikws.reputationfabric.common.component.data.PlayerData.PlayerMode;
import ru.kirikws.reputationfabric.config.ReputationConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Central utility for karma and mode operations.
 * Replaces the previous interface-based API with direct static methods.
 *
 * <p>Example usage:
 * <pre>
 *     int karma = ReputationAPIs.getKarma(player);
 *     ReputationAPIs.modifyKarma(player, 10);
 *     boolean isPvP = ReputationAPIs.isPvP(player);
 * </pre>
 */
public final class ReputationAPIs {
    // Position tracking for mode switch immobility check
    private static final Map<UUID, Vec3d> lastPositions = new HashMap<>();

    private ReputationAPIs() {}

    // === Karma Operations ===

    public static int getKarma(PlayerEntity player) {
        return ReputationComponents.PLAYER_DATA.get(player).getKarma();
    }

    public static void setKarma(PlayerEntity player, int karma) {
        var component = ReputationComponents.PLAYER_DATA.get(player);
        component.setKarma(karma);
        if (player instanceof ServerPlayerEntity sp) {
            component.syncToClient();
        }
    }

    public static void modifyKarma(PlayerEntity player, int delta) {
        var component = ReputationComponents.PLAYER_DATA.get(player);
        component.modifyKarma(delta);
        if (player instanceof ServerPlayerEntity sp) {
            component.syncToClient();
        }
    }

    public static boolean hasBadKarma(PlayerEntity player) {
        return getKarma(player) <= ReputationConfig.get().threshold_bad;
    }

    public static boolean hasGoodKarma(PlayerEntity player) {
        return getKarma(player) >= ReputationConfig.get().threshold_good;
    }

    // === Mode Operations ===

    public static PlayerMode getMode(PlayerEntity player) {
        return ReputationComponents.PLAYER_DATA.get(player).getMode();
    }

    public static void setMode(PlayerEntity player, PlayerMode mode) {
        var component = ReputationComponents.PLAYER_DATA.get(player);
        component.setMode(mode);
        if (player instanceof ServerPlayerEntity sp) {
            component.syncToClient();
        }
    }

    public static boolean isPassive(PlayerEntity player) {
        return getMode(player) == PlayerMode.PASSIVE;
    }

    public static boolean isPvP(PlayerEntity player) {
        return getMode(player) == PlayerMode.PVP;
    }

    /**
     * Requests a mode switch for the player.
     * Requires the player to be immobile for a configured time and not recently damaged.
     * Returns true if successful, false otherwise.
     */
    public static boolean requestModeSwitch(PlayerEntity player) {
        var component = ReputationComponents.PLAYER_DATA.get(player);
        long currentTime = System.currentTimeMillis();
        long lastDamageTime = component.getLastDamageTakenTime();

        ReputationConfig config = ReputationConfig.get();
        long requiredTime = config.mode_switch_immobile_time * 50L;

        // Check damage cooldown
        if (lastDamageTime > 0) {
            long timeSinceDamage = currentTime - lastDamageTime;
            if (timeSinceDamage < requiredTime) {
                return false;
            }
        }

        // Check immobility
        UUID playerId = player.getUuid();
        Vec3d currentPos = player.getPos();
        Vec3d lastPos = lastPositions.get(playerId);

        if (lastPos != null) {
            double distSq = currentPos.squaredDistanceTo(lastPos);
            if (distSq > 0.01) {
                return false;
            }
        }

        // Update position and switch mode
        lastPositions.put(playerId, currentPos);
        PlayerMode newMode = component.getMode().toggle();
        component.setMode(newMode);
        if (player instanceof ServerPlayerEntity sp) {
            component.syncToClient();
        }
        return true;
    }

    /**
     * Updates the tracked position for a player. Call every server tick.
     */
    public static void updatePlayerPosition(PlayerEntity player) {
        lastPositions.put(player.getUuid(), player.getPos());
    }

    /**
     * Removes a player from position tracking.
     */
    public static void removePlayer(PlayerEntity player) {
        lastPositions.remove(player.getUuid());
    }

    // === Karma Helper Utilities ===

    public static PlayerEntity findNearestLowKarmaPlayer(ServerPlayerEntity searcher, double radius, int karmaThreshold) {
        PlayerEntity nearest = null;
        double nearestDistSq = Double.MAX_VALUE;

        for (ServerPlayerEntity player : searcher.getServer().getPlayerManager().getPlayerList()) {
            if (player == searcher) continue;

            int karma = getKarma(player);
            if (karma <= karmaThreshold) {
                double distSq = searcher.squaredDistanceTo(player);
                if (distSq <= radius * radius && distSq < nearestDistSq) {
                    nearestDistSq = distSq;
                    nearest = player;
                }
            }
        }
        return nearest;
    }

    public static int calculateAttackKarmaPenalty(PlayerEntity attacker, PlayerEntity target) {
        int targetKarma = getKarma(target);
        if (targetKarma >= 0) {
            return -ReputationConfig.get().karma_damage_penalty;
        }
        return 0;
    }

    public static int calculateKillKarma(PlayerEntity killer, PlayerEntity victim) {
        int victimKarma = getKarma(victim);
        ReputationConfig config = ReputationConfig.get();

        if (victimKarma >= 0) {
            return -config.karma_kill_penalty;
        } else {
            int absVictimKarma = Math.abs(victimKarma);
            return (int) Math.max(config.karma_kill_bonus_min, absVictimKarma * config.karma_kill_bonus_multiplier);
        }
    }
}
