package ru.kirikws.reputationfabric.common.component.data;

/**
 * Unified player mode and data definitions.
 * Replaces separate data classes with a single cohesive unit.
 */
public final class PlayerData {
    private PlayerData() {}

    /**
     * Player combat mode enumeration.
     * PASSIVE - cannot attack or be attacked by other players
     * PVP - normal combat rules apply
     */
    public enum PlayerMode {
        PASSIVE,
        PVP;

        /**
         * Toggles to the opposite mode.
         * @return PASSIVE if this is PVP, PVP if this is PASSIVE
         */
        public PlayerMode toggle() {
            return this == PASSIVE ? PVP : PASSIVE;
        }
    }
}
