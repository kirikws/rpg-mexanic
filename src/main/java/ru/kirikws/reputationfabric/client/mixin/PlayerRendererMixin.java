package ru.kirikws.reputationfabric.client.mixin;

import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.model.EntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import ru.kirikws.reputationfabric.client.render.KarmaParticleRenderer;

/**
 * Unified mixin for player renderer enhancements.
 * Adds karma particle effects during rendering.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class PlayerRendererMixin<T extends LivingEntity, M extends EntityModel<T>> {
    @Inject(method = "render", at = @At("TAIL"))
    private void onRender(T entity, float yaw, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, CallbackInfo ci) {
        if (entity instanceof PlayerEntity player) {
            KarmaParticleRenderer.spawnParticles(player);
        }
    }
}
