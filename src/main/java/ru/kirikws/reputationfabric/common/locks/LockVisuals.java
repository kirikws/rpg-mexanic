package ru.kirikws.reputationfabric.common.locks;

import net.minecraft.util.math.MathHelper;

/**
 * Helper class for generating lock-related visual effects and colors.
 */
public class LockVisuals {
    
    /**
     * Gets the display color for a lock type.
     */
    public static int getLockColor(LockType lockType) {
        return switch (lockType) {
            case WOODEN -> 0x8B4513;
            case IRON -> 0xD3D3D3;
            case GOLDEN -> 0xFFD700;
            case DIAMOND -> 0x00BFFF;
        };
    }

    /**
     * Gets the arrow character for a direction index.
     * 0=up, 1=down, 2=left, 3=right
     */
    public static String getDirectionArrow(int direction) {
        return switch (direction) {
            case 0 -> "↑";
            case 1 -> "↓";
            case 2 -> "←";
            case 3 -> "→";
            default -> "?";
        };
    }

    /**
     * Gets the display name color for a lock type.
     */
    public static int getTextColorForLock(LockType lockType) {
        return switch (lockType) {
            case WOODEN -> 0x8B4513;
            case IRON -> 0x808080;
            case GOLDEN -> 0xFFAA00;
            case DIAMOND -> 0x55FFFF;
        };
    }
}
