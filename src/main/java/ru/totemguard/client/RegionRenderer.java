package ru.totemguard.client;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.*;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import ru.totemguard.client.ClientRegionCache.Region;

import java.util.List;

/**
 * Renders region boundaries using WorldRenderEvents.
 */
public class RegionRenderer {

    public static void register() {
        WorldRenderEvents.LAST.register(RegionRenderer::onWorldRenderLast);
    }

    private static void onWorldRenderLast(WorldRenderContext context) {
        // Проверяем, включен ли рендер границ
        if (!ru.totemguard.client.KeyBindings.boundariesVisible) return;

        List<Region> regions = ClientRegionCache.getRegions();
        if (regions.isEmpty()) return;

        MinecraftClient client = MinecraftClient.getInstance();
        Vec3d camPos = context.camera().getPos();

        RenderSystem.enableDepthTest();
        RenderSystem.disableCull();
        // RenderSystem.disableTexture(); // Removed in 1.20.1? Usually not needed for lines
        RenderSystem.setShader(GameRenderer::getPositionColorProgram);

        Tessellator tessellator = Tessellator.getInstance();
        BufferBuilder buffer = tessellator.getBuffer();
        buffer.begin(VertexFormat.DrawMode.DEBUG_LINES, VertexFormats.POSITION_COLOR);

        Matrix4f matrix = context.matrixStack().peek().getPositionMatrix();

        for (Region region : regions) {
            int color = region.isOwner ? 0x44FF44 : 0xFF4444; // Green vs Red
            float r = ((color >> 16) & 0xFF) / 255.0f;
            float g = ((color >> 8) & 0xFF) / 255.0f;
            float b = (color & 0xFF) / 255.0f;
            float a = 0.6f;

            float[][] points = region.points;
            if (points.length < 2) continue;

            double y = context.camera().getPos().y + 2.5; // Render slightly above camera height

            for (int i = 0; i < points.length; i++) {
                float[] p1 = points[i];
                float[] p2 = points[(i + 1) % points.length];

                buffer.vertex(matrix, p1[0] - (float) camPos.x, (float) (y - camPos.y), p1[1] - (float) camPos.z)
                        .color(r, g, b, a).next();
                buffer.vertex(matrix, p2[0] - (float) camPos.x, (float) (y - camPos.y), p2[1] - (float) camPos.z)
                        .color(r, g, b, a).next();
            }
        }

        BufferRenderer.drawWithGlobalProgram(buffer.end());
        
        RenderSystem.enableCull();
        // RenderSystem.enableTexture(); // Not needed
    }
}
