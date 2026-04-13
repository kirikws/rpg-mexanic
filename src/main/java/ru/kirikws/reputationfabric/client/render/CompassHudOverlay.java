package ru.kirikws.reputationfabric.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

/**
 * HUD overlay for displaying compass direction to low-karma targets.
 * Renders an arrow pointing toward the target location.
 */
public class CompassHudOverlay {
    private static Vec3d targetPosition = null;
    private static int remainingTicks = 0;
    private static final int COMPASS_SIZE = 64;
    private static final int COMPASS_MARGIN = 10;

    /**
     * Sets the compass target coordinates and duration.
     *
     * @param targetPos the position of the target player
     * @param durationTicks how long the compass effect should last
     */
    public static void setTarget(Vec3d targetPos, int durationTicks) {
        targetPosition = targetPos;
        remainingTicks = durationTicks;
    }

    /**
     * Renders the compass arrow on the HUD.
     * Called every frame by the HUD render event.
     *
     * @param drawContext the drawing context
     */
    public static void render(DrawContext drawContext) {
        if (targetPosition == null || remainingTicks <= 0) {
            return;
        }

        remainingTicks--;

        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) {
            return;
        }

        Vec3d playerPos = client.player.getPos();
        double deltaX = targetPosition.x - playerPos.x;
        double deltaZ = targetPosition.z - playerPos.z;
        double distance = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

        // Calculate angle to target
        double angleToTarget = Math.atan2(deltaZ, deltaX);
        double playerYaw = Math.toRadians(client.player.getYaw());
        double relativeAngle = angleToTarget - playerYaw;

        // Render compass background
        int screenWidth = client.getWindow().getScaledWidth();
        int compassX = screenWidth / 2 - COMPASS_SIZE / 2;
        int compassY = COMPASS_MARGIN;

        drawContext.fill(
                compassX, compassY,
                compassX + COMPASS_SIZE, compassY + COMPASS_SIZE,
                0x80000000
        );

        // Draw direction arrow
        int centerX = compassX + COMPASS_SIZE / 2;
        int centerY = compassY + COMPASS_SIZE / 2;

        double arrowAngle = relativeAngle - Math.PI / 2;
        int arrowLength = COMPASS_SIZE / 2 - 5;
        int endX = centerX + (int) (Math.cos(arrowAngle) * arrowLength);
        int endY = centerY + (int) (Math.sin(arrowAngle) * arrowLength);

        drawContext.fill(
                centerX - 2, centerY - 2,
                endX + 2, endY + 2,
                0xFFFF5555
        );

        // Draw distance text
        String distanceText = String.format("%.0fm", distance);
        int textWidth = client.textRenderer.getWidth(distanceText);
        drawContext.drawText(
                client.textRenderer,
                distanceText,
                centerX - textWidth / 2,
                compassY + COMPASS_SIZE + 2,
                0xFFFFFF,
                true
        );

        if (remainingTicks <= 0) {
            targetPosition = null;
        }
    }

    /**
     * Clears the compass target.
     */
    public static void clear() {
        targetPosition = null;
        remainingTicks = 0;
    }
}
