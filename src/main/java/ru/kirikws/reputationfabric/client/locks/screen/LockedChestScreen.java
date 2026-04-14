package ru.kirikws.reputationfabric.client.locks.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.kirikws.reputationfabric.ReputationFabricMod;
import ru.kirikws.reputationfabric.common.locks.LockType;
import ru.kirikws.reputationfabric.common.locks.screen.LockedChestScreenHandler;

/**
 * GUI screen for locked chests showing lock status and key/lockpick slots.
 */
public class LockedChestScreen extends HandledScreen<LockedChestScreenHandler> {
    private static final Identifier TEXTURE = new Identifier(ReputationFabricMod.MOD_ID, 
            "textures/gui/container/locked_chest.png");

    public LockedChestScreen(LockedChestScreenHandler handler, PlayerInventory inventory, Text title) {
        super(handler, inventory, title);
        this.backgroundHeight = 166;
        this.playerInventoryTitleY = this.backgroundHeight - 94;
    }

    @Override
    protected void drawBackground(DrawContext context, float delta, int mouseX, int mouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (this.width - this.backgroundWidth) / 2;
        int y = (this.height - this.backgroundHeight) / 2;
        context.drawTexture(TEXTURE, x, y, 0, 0, this.backgroundWidth, this.backgroundHeight);

        // Draw lock type indicator
        LockType lockType = handler.getLockType();
        if (lockType != null) {
            int color = lockType.getColor();
            context.fill(x + 70, y + 8, x + 90, y + 18, color | 0xFF000000);
            
            // Draw lock icon
            String lockText = switch (lockType) {
                case WOODEN -> "🪵";
                case IRON -> "⚙";
                case GOLDEN -> "🥇";
                case DIAMOND -> "💎";
            };
            context.drawText(textRenderer, lockText, x + 73, y + 9, 0xFFFFFF, false);
        }

        // Draw "Locked" status
        context.drawText(textRenderer, Text.translatable("gui.reputation-fabric.locked_chest.locked"),
                x + 60, y + 35, 0xFF0000, false);

        // Instructions
        context.drawText(textRenderer, Text.translatable("gui.reputation-fabric.locked_chest.instructions"),
                x + 10, y + 65, 0x404040, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);
        this.drawMouseoverTooltip(context, mouseX, mouseY);
    }

    @Override
    protected void drawForeground(DrawContext context, int mouseX, int mouseY) {
        context.drawText(this.textRenderer, this.title, this.titleX, this.titleY, 0x404040, false);
        context.drawText(this.textRenderer, this.playerInventoryTitle,
                this.playerInventoryTitleX, this.playerInventoryTitleY, 0x404040, false);
    }
}
