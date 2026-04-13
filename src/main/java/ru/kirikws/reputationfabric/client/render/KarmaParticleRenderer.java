package ru.kirikws.reputationfabric.client.render;

import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.util.math.Vec3d;
import ru.kirikws.reputationfabric.client.network.ClientNetworking;
import ru.kirikws.reputationfabric.config.ReputationConfig;

import java.util.Random;

/**
 * Renders karma particle effects around the local player.
 * Good karma: HAPPY_VILLAGER particles above head
 * Bad karma: CAMPFIRE_COSY_SMOKE particles at feet
 */
public class KarmaParticleRenderer {
    private static final Random RANDOM = new Random();

    public static void spawnParticles(PlayerEntity player) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (player != client.player || client.world == null || !client.world.isClient) {
            return;
        }

        // No particles in PASSIVE mode
        if (ClientNetworking.ModeStateHolder.isPassive()) {
            return;
        }

        // Throttle particle spawning using player age
        if (player.age % 10 != 0) {
            return;
        }

        int karma = ClientNetworking.KarmaStateHolder.getKarma();
        ReputationConfig config = ReputationConfig.get();
        Vec3d pos = player.getPos();

        if (karma <= config.threshold_bad) {
            spawnParticles(client, pos, ParticleTypes.CAMPFIRE_COSY_SMOKE, 0.1, 0.1);
        } else if (karma >= config.threshold_good) {
            spawnParticles(client, pos, ParticleTypes.HAPPY_VILLAGER, player.getHeight() + 0.3, 0.05);
        }
    }

    private static void spawnParticles(MinecraftClient client, Vec3d pos, net.minecraft.particle.ParticleEffect type, double yOffset, double velocityY) {
        for (int i = 0; i < 2; i++) {
            double offsetX = (RANDOM.nextDouble() - 0.5) * 0.5;
            double offsetZ = (RANDOM.nextDouble() - 0.5) * 0.5;
            client.world.addParticle(type, pos.x + offsetX, pos.y + yOffset, pos.z + offsetZ, 0.0, velocityY, 0.0);
        }
    }
}
