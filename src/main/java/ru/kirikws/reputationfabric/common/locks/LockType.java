package ru.kirikws.reputationfabric.common.locks;

import net.minecraft.util.StringIdentifiable;

/**
 * Enum representing different lock tiers with associated properties.
 */
public enum LockType implements StringIdentifiable {
    WOODEN("wooden", 0.3f, 0x8B4513),
    IRON("iron", 0.5f, 0xD3D3D3),
    GOLDEN("golden", 0.7f, 0xFFD700),
    DIAMOND("diamond", 0.9f, 0x00BFFF);

    private final String name;
    private final float lockpickDifficulty;
    private final int color;

    LockType(String name, float lockpickDifficulty, int color) {
        this.name = name;
        this.lockpickDifficulty = lockpickDifficulty;
        this.color = color;
    }

    /**
     * Returns the number of directions to remember in lockpicking minigame.
     */
    public int getSequenceLength() {
        return switch (this) {
            case WOODEN -> 3;
            case IRON -> 4;
            case GOLDEN -> 5;
            case DIAMOND -> 6;
        };
    }

    public float getLockpickDifficulty() {
        return lockpickDifficulty;
    }

    public int getColor() {
        return color;
    }

    @Override
    public String asString() {
        return name;
    }

    public String getTranslationKey() {
        return "lock.type." + name;
    }
}
