package ru.kirikws.reputationfabric.common.locks;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

/**
 * Commands for the lock system.
 */
public class LockCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        // Команда /lock - установка замка
        dispatcher.register(
            CommandManager.literal("lock")
                .then(CommandManager.argument("pos", BlockPosArgumentType.blockPos())
                    .then(CommandManager.argument("type", StringArgumentType.word())
                        .executes(LockCommands::applyLock))
                )
        );
        
        // Команда /lockpick - ввод для мини-игры
        dispatcher.register(
            CommandManager.literal("lockpick")
                .then(CommandManager.argument("direction", StringArgumentType.word())
                    .executes(LockCommands::lockpickInput))
                .then(CommandManager.literal("cancel")
                    .executes(LockCommands::cancelLockpick))
        );
    }

    private static int applyLock(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        BlockPos pos = BlockPosArgumentType.getBlockPos(context, "pos");
        String lockTypeName = StringArgumentType.getString(context, "type");

        // Check if player is present
        ServerPlayerEntity player = source.getPlayer();
        if (player == null) {
            source.sendError(Text.literal("Only players can use this command"));
            return 0;
        }

        // Get block entity at position
        if (!(source.getWorld().getBlockEntity(pos) instanceof ChestBlockEntity chest)) {
            source.sendError(Text.literal("Target block is not a chest"));
            return 0;
        }

        // Parse lock type
        LockType lockType;
        try {
            lockType = LockType.valueOf(lockTypeName.toUpperCase());
        } catch (IllegalArgumentException e) {
            source.sendError(Text.literal("Invalid lock type. Use: wooden, iron, golden, diamond"));
            return 0;
        }

        // Apply lock
        LockManager.applyLock(chest, lockType);
        source.sendFeedback(() -> Text.literal("Successfully applied " + lockTypeName + " lock to chest at " + pos), true);

        return 1;
    }
    
    private static int lockpickInput(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        if (player == null) {
            source.sendError(Text.literal("Only players can use this command"));
            return 0;
        }
        
        String direction = StringArgumentType.getString(context, "direction");
        
        // Проверяем, играет ли игрок
        if (!LockpickMinigame.isPlaying(player.getUuid())) {
            player.sendMessage(Text.literal("Вы не взламываете замок! Используйте отмычку на сундуке.").formatted(Formatting.RED), true);
            return 0;
        }
        
        // Обрабатываем ввод
        boolean isFinished = LockpickMinigame.processInput(player, direction);
        
        // Если игра закончена, проверяем результат
        if (isFinished) {
            LockpickMinigame.LockpickSession session = LockpickMinigame.getSession(player.getUuid());
            // Сессия уже удалена, так что проверяем через отдельный метод
            // Но мы можем получить позицию из сообщения о завершении
            // Открываем сундук если успех
            LockpickMinigame.LockpickSession dummySession = LockpickMinigame.getSession(player.getUuid());
            // Поскольку сессия удалена, нам нужно по-другому обработать
            // Открываем сундук если взлом успешен - это обрабатывается в processInput
        }
        
        return 1;
    }
    
    private static int cancelLockpick(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerCommandSource source = context.getSource();
        ServerPlayerEntity player = source.getPlayer();
        
        if (player == null) {
            source.sendError(Text.literal("Only players can use this command"));
            return 0;
        }
        
        LockpickMinigame.cancel(player);
        return 1;
    }
}
