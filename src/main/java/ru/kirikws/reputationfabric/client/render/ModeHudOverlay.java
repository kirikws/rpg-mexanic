package ru.kirikws.reputationfabric.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.Items;
import ru.kirikws.reputationfabric.client.network.ClientNetworking;

/**
 * HUD overlay showing current player mode (PASSIVE/PVP).
 */
public class ModeHudOverlay {
    private static final int ICON_SIZE = 16;
    private static final int MARGIN = 10;

    public static void render(DrawContext drawContext) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        int x = MARGIN;
        int y = client.getWindow().getScaledHeight() / 2 - ICON_SIZE / 2;

        if (ClientNetworking.ModeStateHolder.isPassive()) {
            drawContext.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, 0x8033CC33);
            drawContext.drawText(client.textRenderer, "PASSIVE", x + ICON_SIZE + 4, y + 4, 0x33CC33, false);
        } else {
            drawContext.fill(x, y, x + ICON_SIZE, y + ICON_SIZE, 0x80CC3333);
            drawContext.drawItem(Items.DIAMOND_SWORD.getDefaultStack(), x, y);
            drawContext.drawText(client.textRenderer, "PvP", x + ICON_SIZE + 4, y + 4, 0xCC3333, false);
        }
    }
}
