package ru.totemguard.guest;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import ru.kirikws.reputationfabric.api.ReputationAPIs;
import ru.totemguard.config.TotemConfig;
import ru.totemguard.storage.RegionState;
import ru.totemguard.storage.TrustedManager;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks guest players in foreign regions.
 * Implements grace period, karma drain, and intruder flagging.
 *
 * Timeline:
 *  0 - grace_period:       No penalty, title notification
 *  grace - warning:        Warning in action bar
 *  > warning:              Karma drain + intruder flag (owner can attack)
 */
public class GuestTracker {

    /**
     * State of a guest in a foreign region.
     */
    public static class GuestState {
        public final UUID regionOwner;
        public final long entryTime;
        public boolean warned = false;
        public boolean flagged = false;

        public GuestState(UUID regionOwner, long entryTime) {
            this.regionOwner = regionOwner;
            this.entryTime = entryTime;
        }

        public long getElapsedSeconds() {
            return (System.currentTimeMillis() - entryTime) / 1000;
        }
    }

    // player UUID -> guest state
    private static final Map<UUID, GuestState> trackedGuests = new HashMap<>();

    /**
     * Registers the server tick listener for guest tracking.
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Only check every second (20 ticks)
            if (server.getTicks() % 20 != 0) return;

            TotemConfig config = TotemConfig.get();
            RegionState regions = RegionState.getOrCreate(server);
            TrustedManager trust = TrustedManager.getOrCreate(server);

            for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
                UUID playerId = player.getUuid();
                BlockPos pos = player.getBlockPos();
                UUID regionOwner = regions.getOwner(pos);

                // Not in any region
                if (regionOwner == null) {
                    clearPlayer(playerId);
                    continue;
                }

                // Owner or trusted — no tracking needed
                if (playerId.equals(regionOwner) || trust.isTrusted(regionOwner, playerId)) {
                    clearPlayer(playerId);
                    continue;
                }

                // Guest in foreign region
                GuestState state = trackedGuests.get(playerId);
                if (state == null || !state.regionOwner.equals(regionOwner)) {
                    state = new GuestState(regionOwner, System.currentTimeMillis());
                    trackedGuests.put(playerId, state);
                    // Send enter notification
                    sendEnterNotification(player, regionOwner, server);
                }

                long elapsed = state.getElapsedSeconds();
                int grace = config.guest_grace_period_seconds;
                int warning = config.guest_karma_warning_seconds;

                // Phase 1: Warning phase (after grace period)
                if (elapsed > grace && !state.warned) {
                    state.warned = true;
                    sendWarningNotification(player);
                }

                // Phase 2: Intruder phase (after warning period)
                if (elapsed > warning) {
                    if (!state.flagged) {
                        state.flagged = true;
                        ProtectionRules.flagIntruder(playerId);
                    }

                    // Drain karma
                    ReputationAPIs.modifyKarma(player, -config.guest_karma_drain_per_minute / 60);

                    // Show karma warning
                    int karma = ReputationAPIs.getKarma(player);
                    if (karma < 0) {
                        player.sendMessage(
                                Text.translatable("message.totemguard.karma_drain", karma)
                                        .formatted(Formatting.RED),
                                true
                        );
                    }
                }
            }
        });

        // Clean up on disconnect
        net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            trackedGuests.remove(handler.getPlayer().getUuid());
            ProtectionRules.clearIntruder(handler.getPlayer().getUuid());
        });
    }

    /**
     * Sends a title notification when entering a foreign region.
     */
    private static void sendEnterNotification(ServerPlayerEntity player, UUID regionOwner, MinecraftServer server) {
        String ownerName = getOwnerDisplayName(regionOwner, server);

        player.sendMessage(
                Text.literal("⚠ ")
                        .append(Text.translatable("message.totemguard.region_enter", ownerName))
                        .formatted(Formatting.YELLOW),
                false
        );

        // Play warning sound
        player.getWorld().playSound(
                null, player.getBlockPos(),
                SoundEvents.ENTITY_VILLAGER_NO,
                SoundCategory.PLAYERS, 0.5f, 1.0f
        );
    }

    /**
     * Sends a warning when approaching intruder status.
     */
    private static void sendWarningNotification(ServerPlayerEntity player) {
        player.sendMessage(
                Text.literal("⚠ ")
                        .append(Text.translatable("message.totemguard.intruder_warning"))
                        .formatted(Formatting.RED),
                false
        );

        player.getWorld().playSound(
                null, player.getBlockPos(),
                SoundEvents.BLOCK_ANVIL_LAND,
                SoundCategory.PLAYERS, 0.3f, 0.8f
        );
    }

    /**
     * Gets the display name of a region owner.
     */
    private static String getOwnerDisplayName(UUID owner, MinecraftServer server) {
        ServerPlayerEntity ownerPlayer = server.getPlayerManager().getPlayer(owner);
        if (ownerPlayer != null) {
            return ownerPlayer.getName().getString();
        }
        return owner.toString().substring(0, 8);
    }

    /**
     * Clears tracking data for a player.
     */
    private static void clearPlayer(UUID playerId) {
        trackedGuests.remove(playerId);
        ProtectionRules.clearIntruder(playerId);
    }

    /**
     * Gets the current guest state for a player.
     */
    public static GuestState getState(UUID player) {
        return trackedGuests.get(player);
    }

    /**
     * Checks if a player is currently tracked as a guest.
     */
    public static boolean isGuest(UUID player) {
        return trackedGuests.containsKey(player);
    }
}
