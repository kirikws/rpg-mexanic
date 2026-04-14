package ru.kirikws.reputationfabric.common.registry;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import ru.kirikws.reputationfabric.ReputationFabricMod;
import ru.kirikws.reputationfabric.api.ReputationAPIs;
import ru.kirikws.reputationfabric.common.component.ReputationComponents;
import ru.kirikws.reputationfabric.common.component.data.PlayerData.PlayerMode;
import ru.kirikws.reputationfabric.config.ReputationConfig;

import java.util.Collection;

/**
 * Registers all commands provided by the mod.
 * Includes admin commands for karma and mode management,
 * as well as the configuration reload command.
 */
public class ModCommands {
    private static final String ROOT_COMMAND = "reputation-fabric";
    private static final String KARMA_COMMAND = "karma";
    private static final String MODE_COMMAND = "mode";

    /**
     * Registers all mod commands with the server command dispatcher.
     */
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            registerKarmaCommand(dispatcher, registryAccess);
            registerModeCommand(dispatcher, registryAccess);
            registerReloadCommand(dispatcher);
            ru.kirikws.reputationfabric.common.locks.LockCommands.register(dispatcher, registryAccess, environment);
        });

        ReputationFabricMod.LOGGER.debug("Registering mod commands...");
    }

    /**
     * Registers the karma subcommand tree.
     * Provides: /karma get <player>, /karma set <player> <value>
     */
    private static void registerKarmaCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(
                CommandManager.literal(ROOT_COMMAND)
                        .then(CommandManager.literal(KARMA_COMMAND)
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("get")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .executes(ModCommands::getKarma)
                                        )
                                )
                                .then(CommandManager.literal("set")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager.argument("value", IntegerArgumentType.integer())
                                                        .executes(ModCommands::setKarma)
                                                )
                                        )
                                )
                        )
        );
    }

    /**
     * Registers the mode subcommand tree.
     * Provides: /mode set <player> <passive|pvp>
     */
    private static void registerModeCommand(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess) {
        dispatcher.register(
                CommandManager.literal(ROOT_COMMAND)
                        .then(CommandManager.literal(MODE_COMMAND)
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.literal("set")
                                        .then(CommandManager.argument("player", EntityArgumentType.player())
                                                .then(CommandManager.argument("mode", StringArgumentType.word())
                                                        .suggests((context, builder) -> {
                                                            builder.suggest("passive");
                                                            builder.suggest("pvp");
                                                            return builder.buildFuture();
                                                        })
                                                        .executes(ModCommands::setMode)
                                                )
                                        )
                                )
                        )
        );
    }

    /**
     * Registers the reload subcommand.
     * Provides: /reputation-fabric reload
     */
    private static void registerReloadCommand(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal(ROOT_COMMAND)
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(CommandManager.literal("reload")
                                .executes(ModCommands::reloadConfig)
                        )
        );
    }

    private static int getKarma(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
        int karma = ReputationAPIs.getKarma(target);

        context.getSource().sendFeedback(
                () -> Text.translatable("command." + ReputationFabricMod.MOD_ID + ".karma.get",
                        target.getName().getString(), karma),
                true
        );

        return karma;
    }

    private static int setKarma(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
        int value = IntegerArgumentType.getInteger(context, "value");

        ReputationAPIs.setKarma(target, value);

        context.getSource().sendFeedback(
                () -> Text.translatable("command." + ReputationFabricMod.MOD_ID + ".karma.set",
                        target.getName().getString(), value),
                true
        );

        target.sendMessage(
                Text.translatable("message." + ReputationFabricMod.MOD_ID + ".karma_changed_by_admin", value),
                true
        );

        return 1;
    }

    private static int setMode(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity target = EntityArgumentType.getPlayer(context, "player");
        String modeStr = StringArgumentType.getString(context, "mode");

        PlayerMode mode;
        try {
            mode = PlayerMode.valueOf(modeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            context.getSource().sendError(
                    Text.translatable("command." + ReputationFabricMod.MOD_ID + ".mode.invalid", modeStr)
            );
            return 0;
        }

        ReputationAPIs.setMode(target, mode);

        context.getSource().sendFeedback(
                () -> Text.translatable("command." + ReputationFabricMod.MOD_ID + ".mode.set",
                        target.getName().getString(), mode.name()),
                true
        );

        target.sendMessage(
                Text.translatable("message." + ReputationFabricMod.MOD_ID + ".mode_changed_by_admin", mode.name()),
                true
        );

        return 1;
    }

    private static int reloadConfig(CommandContext<ServerCommandSource> context) {
        ReputationConfig.reload();

        context.getSource().sendFeedback(
                () -> Text.translatable("command." + ReputationFabricMod.MOD_ID + ".reload.success"),
                true
        );

        return 1;
    }
}
