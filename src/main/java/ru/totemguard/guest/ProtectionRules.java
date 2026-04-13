package ru.totemguard.guest;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Manages intruder flags for PvP rules.
 * Players flagged as intruders can be attacked by region owners without karma penalty.
 */
public class ProtectionRules {
    private static final Set<UUID> intruders = new HashSet<>();

    /**
     * Marks a player as an intruder.
     * Region owners can attack intruders without karma penalty.
     */
    public static void flagIntruder(UUID player) {
        intruders.add(player);
    }

    /**
     * Removes the intruder flag from a player.
     */
    public static void clearIntruder(UUID player) {
        intruders.remove(player);
    }

    /**
     * Checks if a player is currently flagged as an intruder.
     */
    public static boolean isIntruder(UUID player) {
        return intruders.contains(player);
    }

    /**
     * Clears all intruder flags.
     */
    public static void clearAll() {
        intruders.clear();
    }
}
