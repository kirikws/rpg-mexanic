package ru.kirikws.reputationfabric.client.locks.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import ru.kirikws.reputationfabric.ReputationFabricMod;
import ru.kirikws.reputationfabric.common.locks.LockData;

/**
 * Lockpicking minigame screen where player must remember and repeat a sequence.
 */
public class LockpickScreen extends Screen {
    private static final Identifier TEXTURE = new Identifier(ReputationFabricMod.MOD_ID,
            "textures/gui/container/lockpick.png");

    private final LockData lockData;
    private final int[] sequence;
    private int[] playerSequence;
    private int currentStep = 0;
    private boolean showingSequence = true;
    private long sequenceStartTime = 0;
    private final int SEQUENCE_SHOW_TIME = 3000; // 3 seconds

    // Direction buttons: 0=up, 1=down, 2=left, 3=right
    private ButtonWidget upButton;
    private ButtonWidget downButton;
    private ButtonWidget leftButton;
    private ButtonWidget rightButton;

    // Button positions
    private int centerX, centerY;
    private static final int BUTTON_SIZE = 40;
    private static final int BUTTON_SPACING = 10;

    public LockpickScreen(LockData lockData) {
        super(Text.translatable("gui.reputation-fabric.lockpick.title"));
        this.lockData = lockData;
        this.sequence = lockData.getLockSequence();
        this.playerSequence = new int[sequence.length];
    }

    @Override
    protected void init() {
        super.init();

        centerX = width / 2;
        centerY = height / 2;

        sequenceStartTime = System.currentTimeMillis();

        // Create direction buttons
        upButton = ButtonWidget.builder(
                Text.literal("↑"),
                button -> onDirectionPressed(0)
        ).dimensions(centerX - BUTTON_SIZE/2, centerY - BUTTON_SIZE * 2 - BUTTON_SPACING, BUTTON_SIZE, BUTTON_SIZE)
                .build();

        downButton = ButtonWidget.builder(
                Text.literal("↓"),
                button -> onDirectionPressed(1)
        ).dimensions(centerX - BUTTON_SIZE/2, centerY + BUTTON_SPACING, BUTTON_SIZE, BUTTON_SIZE)
                .build();

        leftButton = ButtonWidget.builder(
                Text.literal("←"),
                button -> onDirectionPressed(2)
        ).dimensions(centerX - BUTTON_SIZE - BUTTON_SPACING/2, centerY - BUTTON_SIZE/2, BUTTON_SIZE, BUTTON_SIZE)
                .build();

        rightButton = ButtonWidget.builder(
                Text.literal("→"),
                button -> onDirectionPressed(3)
        ).dimensions(centerX + BUTTON_SPACING/2, centerY - BUTTON_SIZE/2, BUTTON_SIZE, BUTTON_SIZE)
                .build();

        this.addDrawableChild(upButton);
        this.addDrawableChild(downButton);
        this.addDrawableChild(leftButton);
        this.addDrawableChild(rightButton);

        // Initially disable buttons during show phase
        setButtonsActive(false);
    }

    private void onDirectionPressed(int direction) {
        if (!showingSequence && currentStep < sequence.length) {
            playerSequence[currentStep] = direction;
            currentStep++;

            // Play click sound
            MinecraftClient.getInstance().player.playSound(SoundEvents.BLOCK_WOODEN_BUTTON_CLICK_ON, 1.0F, 1.0F);

            // Check if sequence is complete
            if (currentStep == sequence.length) {
                checkSequence();
            }
        }
    }

    private void checkSequence() {
        boolean correct = true;
        for (int i = 0; i < sequence.length; i++) {
            if (playerSequence[i] != sequence[i]) {
                correct = false;
                break;
            }
        }

        if (correct) {
            // Success - close screen and unlock
            MinecraftClient.getInstance().player.sendMessage(
                    Text.translatable("gui.reputation-fabric.lockpick.success"), true);
            this.close();
            // TODO: Trigger unlock logic
        } else {
            // Failure - regenerate sequence and try again
            MinecraftClient.getInstance().player.sendMessage(
                    Text.translatable("gui.reputation-fabric.lockpick.failure"), true);
            lockData.regenerateSequence();
            this.close();
            // TODO: Consume lockpick
        }
    }

    private void setButtonsActive(boolean active) {
        upButton.active = active;
        downButton.active = active;
        leftButton.active = active;
        rightButton.active = active;
    }

    @Override
    public void tick() {
        super.tick();

        // Check if it's time to hide the sequence and let player input
        if (showingSequence && System.currentTimeMillis() - sequenceStartTime >= SEQUENCE_SHOW_TIME) {
            showingSequence = false;
            setButtonsActive(true);
            currentStep = 0;
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        // Draw background
        RenderSystem.setShader(GameRenderer::getPositionTexProgram);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);
        int x = (width - 176) / 2;
        int y = (height - 166) / 2;
        context.drawTexture(TEXTURE, x, y, 0, 0, 176, 166);

        // Draw title
        context.drawText(textRenderer, title, width / 2 - textRenderer.getWidth(title) / 2, y + 10, 0x404040, false);

        // Draw sequence or status
        if (showingSequence) {
            context.drawText(textRenderer,
                    Text.translatable("gui.reputation-fabric.lockpick.memorize"),
                    width / 2 - textRenderer.getWidth(Text.translatable("gui.reputation-fabric.lockpick.memorize")) / 2,
                    y + 30, 0xFFAA00, false);

            // Draw the sequence arrows
            drawSequenceArrows(context, x, y);
            
            // Draw timer
            long timeLeft = SEQUENCE_SHOW_TIME - (System.currentTimeMillis() - sequenceStartTime);
            String timerText = String.format("%.1f", timeLeft / 1000.0f);
            context.drawText(textRenderer, timerText,
                    width / 2 - textRenderer.getWidth(timerText) / 2, y + 80, 0xFFFF00, false);
        } else {
            context.drawText(textRenderer,
                    Text.translatable("gui.reputation-fabric.lockpick.repeat"),
                    width / 2 - textRenderer.getWidth(Text.translatable("gui.reputation-fabric.lockpick.repeat")) / 2,
                    y + 30, 0x00AA00, false);
            
            // Draw progress
            String progress = currentStep + " / " + sequence.length;
            context.drawText(textRenderer, progress,
                    width / 2 - textRenderer.getWidth(progress) / 2, y + 80, 0x404040, false);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawSequenceArrows(DrawContext context, int x, int y) {
        int startX = x + 20;
        int arrowY = y + 50;
        int spacing = 20;

        for (int i = 0; i < sequence.length; i++) {
            String arrow = switch (sequence[i]) {
                case 0 -> "↑";
                case 1 -> "↓";
                case 2 -> "←";
                case 3 -> "→";
                default -> "?";
            };
            context.drawText(textRenderer, arrow, startX + i * spacing, arrowY, 0xFFFFFF, false);
        }
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
            this.close();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return true;
    }
}
