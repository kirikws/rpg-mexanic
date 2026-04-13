package ru.totemguard.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import ru.totemguard.block.TotemBlockEntity;
import ru.totemguard.block.TotemBlocks;
import ru.totemguard.config.TotemConfig;
import ru.totemguard.storage.RegionState;

import java.util.UUID;

/**
 * Place command for placing totems via command.
 */
public class TotemPlaceCommand {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("totem")
                        .then(CommandManager.literal("place")
                                .executes(TotemPlaceCommand::execute)
                        )
        );
    }

    private static int execute(CommandContext<ServerCommandSource> context) {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;

        BlockPos placePos = player.getBlockPos().down();
        player.getServer().getOverworld().setBlockState(placePos, TotemBlocks.CLAIM_TOTEM.getDefaultState(), 3);

        if (player.getServer().getOverworld().getBlockEntity(placePos) instanceof TotemBlockEntity be) {
            be.setOwner(player.getUuid());

            RegionState regions = RegionState.getOrCreate(player.getServer());
            int radius = TotemConfig.get().base_radius;
            regions.addTotem(placePos, player.getUuid(), radius);
            regions.save();

            player.sendMessage(Text.translatable("message.totemguard.totem_placed", radius), false);
        }

        return 1;
    }
}
