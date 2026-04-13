package ru.kirikws.reputationfabric.common.item;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import ru.kirikws.reputationfabric.ReputationFabricMod;
import ru.kirikws.reputationfabric.api.ReputationAPIs;
import ru.kirikws.reputationfabric.common.network.packet.CompassTargetS2CPacket;
import ru.kirikws.reputationfabric.config.ReputationConfig;

/**
 * Karma compass item that points to the nearest player with low karma.
 * When used, searches for players with karma below the configured threshold
 * within the configured radius and sends their position to the client.
 */
public class KarmaCompassItem extends Item {
    private static final String COOLDOWN_GROUP = ReputationFabricMod.MOD_ID + "_karma_compass";

    public KarmaCompassItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        if (!(user instanceof ServerPlayerEntity serverPlayer)) {
            return TypedActionResult.pass(user.getStackInHand(hand));
        }

        // Check cooldown
        if (serverPlayer.getItemCooldownManager().isCoolingDown(this)) {
            return TypedActionResult.fail(user.getStackInHand(hand));
        }

        ReputationConfig config = ReputationConfig.get();
        PlayerEntity target = ReputationAPIs.findNearestLowKarmaPlayer(
                serverPlayer,
                config.compass_radius,
                config.compass_target_karma
        );

        if (target != null) {
            // Send compass target to client
            CompassTargetS2CPacket.send(serverPlayer, target.getPos(), config.compass_cooldown);
            serverPlayer.sendMessage(
                    Text.translatable("message." + ReputationFabricMod.MOD_ID + ".compass_target_found"),
                    true
            );
        } else {
            serverPlayer.sendMessage(
                    Text.translatable("message." + ReputationFabricMod.MOD_ID + ".compass_no_targets"),
                    true
            );
        }

        // Apply cooldown
        serverPlayer.getItemCooldownManager().set(this, config.compass_cooldown);

        return TypedActionResult.success(user.getStackInHand(hand));
    }
}
