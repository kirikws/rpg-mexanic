package ru.totemguard.command;

import com.mojang.authlib.GameProfile;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.command.argument.GameProfileArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import ru.totemguard.block.TotemBlockEntity;
import ru.totemguard.block.TotemBlocks;
import ru.totemguard.storage.RegionState;
import ru.totemguard.storage.TrustedManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;

/**
 * Registers all TotemGuard commands.
 */
public class TotemCommands {

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("totem")
                        // 1. Доверенные игроки (можно добавлять оффлайн игроков)
                        .then(CommandManager.literal("trust")
                                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                        .executes(TotemCommands::trustPlayer)
                                )
                        )
                        // 2. Удаление из доверенных
                        .then(CommandManager.literal("untrust")
                                .then(CommandManager.argument("player", GameProfileArgumentType.gameProfile())
                                        .executes(TotemCommands::untrustPlayer)
                                )
                        )
                        // 3. Смена владельца (Только для ОП)
                        .then(CommandManager.literal("setowner")
                                .requires(source -> source.hasPermissionLevel(2))
                                .then(CommandManager.argument("newOwner", GameProfileArgumentType.gameProfile())
                                        .executes(TotemCommands::setOwner)
                                )
                        )
                        // 4. Информация
                        .then(CommandManager.literal("info")
                                .executes(TotemCommands::showInfo)
                        )
        );
    }

    private static int trustPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity source = context.getSource().getPlayer();
        if (source == null) return 0;

        // Получаем профиль игрока (онлайн или оффлайн)
        Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "player");
        if (profiles.isEmpty()) return 0;
        GameProfile targetProfile = profiles.iterator().next();

        RegionState regions = RegionState.getOrCreate(context.getSource().getServer());
        UUID regionOwner = regions.getOwner(source.getBlockPos());

        // Если игрок не в регионе, ищем ближайший
        if (regionOwner == null) {
            regionOwner = findNearestTotemOwner(context.getSource().getServer(), source.getBlockPos());
        }

        if (regionOwner == null) {
            source.sendMessage(Text.literal("Регион не найден. Встаньте рядом с тотемом."), false);
            return 0;
        }

        // Проверка прав (Владелец или ОП)
        if (!source.getUuid().equals(regionOwner) && !source.hasPermissionLevel(2)) {
            source.sendMessage(Text.literal("Вы не владелец этого региона."), false);
            return 0;
        }

        TrustedManager trust = TrustedManager.getOrCreate(context.getSource().getServer());
        trust.add(regionOwner, targetProfile.getId());
        trust.save(context.getSource().getServer());

        source.sendMessage(
                Text.literal("Игрок ")
                        .append(Text.literal(targetProfile.getName() != null ? targetProfile.getName() : targetProfile.getId().toString()))
                        .append(Text.literal(" добавлен в доверенные."))
                        .formatted(net.minecraft.util.Formatting.GREEN),
                false
        );
        return 1;
    }

    private static int untrustPlayer(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity source = context.getSource().getPlayer();
        if (source == null) return 0;

        Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "player");
        if (profiles.isEmpty()) return 0;
        GameProfile targetProfile = profiles.iterator().next();

        RegionState regions = RegionState.getOrCreate(context.getSource().getServer());
        UUID regionOwner = regions.getOwner(source.getBlockPos());
        if (regionOwner == null) regionOwner = findNearestTotemOwner(context.getSource().getServer(), source.getBlockPos());

        if (regionOwner == null) {
            source.sendMessage(Text.literal("Регион не найден."), false);
            return 0;
        }

        if (!source.getUuid().equals(regionOwner) && !source.hasPermissionLevel(2)) {
            source.sendMessage(Text.literal("Вы не владелец этого региона."), false);
            return 0;
        }

        TrustedManager trust = TrustedManager.getOrCreate(context.getSource().getServer());
        trust.remove(regionOwner, targetProfile.getId());
        trust.save(context.getSource().getServer());

        source.sendMessage(
                Text.literal("Игрок ")
                        .append(Text.literal(targetProfile.getName() != null ? targetProfile.getName() : targetProfile.getId().toString()))
                        .append(Text.literal(" удалён из доверенных."))
                        .formatted(net.minecraft.util.Formatting.RED),
                false
        );
        return 1;
    }

    /**
     * Админская команда: /totem setowner <ник>
     * Позволяет передать права оффлайн игроку.
     */
    private static int setOwner(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        Collection<GameProfile> profiles = GameProfileArgumentType.getProfileArgument(context, "newOwner");
        if (profiles.isEmpty()) {
            context.getSource().sendError(Text.literal("Игрок не найден в базе данных сервера."));
            return 0;
        }
        GameProfile targetProfile = profiles.iterator().next();
        UUID newOwnerId = targetProfile.getId();
        String targetName = targetProfile.getName() != null ? targetProfile.getName() : newOwnerId.toString();

        ServerPlayerEntity source = context.getSource().getPlayer();
        if (source == null) return 0;

        // Находим регион, где стоит игрок
        BlockPos targetPos = source.getBlockPos();
        
        // Если смотрим на тотем, берем его координаты
        var hitResult = source.raycast(10.0, 0.0f, false);
        if (hitResult.getType() == net.minecraft.util.hit.HitResult.Type.BLOCK) {
            BlockPos hitPos = ((net.minecraft.util.hit.BlockHitResult) hitResult).getBlockPos();
            if (source.getWorld().getBlockState(hitPos).isOf(TotemBlocks.CLAIM_TOTEM)) {
                targetPos = hitPos;
            }
        }

        RegionState regions = RegionState.getOrCreate(source.getServer());
        UUID oldOwner = regions.getOwner(targetPos);

        // Если не попали в регион, ищем ближайший тотем
        if (oldOwner == null) {
            // Если админ просто стоит рядом, но не точно в центре, пробуем найти ближайший
            oldOwner = findNearestTotemOwner(source.getServer(), targetPos);
        }

        if (oldOwner == null) {
            source.sendMessage(Text.literal("Тотем или регион не найдены. Встаньте ближе к тотему или посмотрите на него."), false);
            return 0;
        }

        // Логика передачи:
        // 1. Берем все тотемы старого владельца.
        // 2. Удаляем их из региона старого.
        // 3. Добавляем их в регион нового.
        
        // ВАЖНО: Копируем список, чтобы избежать ошибок при удалении
        var totems = new ArrayList<>(regions.getTotems(oldOwner));
        
        for (var totem : totems) {
            regions.removeTotem(totem.pos);
        }
        for (var totem : totems) {
            regions.addTotem(totem.pos, newOwnerId, totem.radius);
        }
        regions.save();

        // Обновляем данные в блоках (визуально на сервере не меняется, но для логики важно)
        for (var totem : totems) {
            if (source.getWorld().getBlockEntity(totem.pos) instanceof TotemBlockEntity be) {
                be.setOwner(newOwnerId);
                be.markDirty();
            }
        }

        source.sendMessage(
                Text.literal("Владелец региона изменён на ")
                        .append(Text.literal(targetName).formatted(net.minecraft.util.Formatting.GREEN))
                        .append(Text.literal(".")),
                false
        );
        return 1;
    }

    private static int showInfo(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        ServerPlayerEntity player = context.getSource().getPlayer();
        if (player == null) return 0;

        RegionState regions = RegionState.getOrCreate(context.getSource().getServer());
        UUID regionOwner = regions.getOwner(player.getBlockPos());

        if (regionOwner == null) {
            player.sendMessage(Text.literal("Вы находитесь вне защиты тотема."), false);
            return 0;
        }

        int totemCount = regions.getTotems(regionOwner).size();
        boolean isActive = regions.getRegion(regionOwner) != null && !regions.getRegion(regionOwner).isEmpty();

        player.sendMessage(Text.translatable("command.totemguard.info",
                regionOwner.toString().substring(0, 8), totemCount, isActive), false);
        return 1;
    }

    private static UUID findNearestTotemOwner(net.minecraft.server.MinecraftServer server, BlockPos pos) {
        RegionState regions = RegionState.getOrCreate(server);
        UUID nearestOwner = null;
        double minDist = Double.MAX_VALUE;

        for (UUID owner : regions.getAllOwners()) {
            var region = regions.getRegion(owner);
            if (region == null) continue;
            
            // Проверяем, внутри ли мы региона
            if (region.contains(pos)) {
                return owner;
            }
            
            // Ищем ближайший компонент региона
            for (var comp : region.getComponents()) {
                double dist = pos.getSquaredDistance(comp.getCenterXZ().getX(), pos.getY(), comp.getCenterXZ().getZ());
                if (dist < minDist) {
                    minDist = dist;
                    nearestOwner = owner;
                }
            }
        }
        return nearestOwner;
    }
}
