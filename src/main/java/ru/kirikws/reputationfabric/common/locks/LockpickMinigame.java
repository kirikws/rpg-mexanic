package ru.kirikws.reputationfabric.common.locks;

import net.minecraft.block.Block;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.screen.NamedScreenHandlerFactory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Fallout-style lockpicking minigame manager.
 * Players must enter the correct sequence via commands: /lockpick up/down/left/right
 */
public class LockpickMinigame {
    
    // Активные сессии мини-игры (player UUID -> LockpickSession)
    private static final Map<UUID, LockpickSession> activeSessions = new HashMap<>();
    
    // Направления для мини-игры
    public static final String[] DIRECTIONS = {"up", "down", "left", "right"};
    public static final String[] DIRECTION_SYMBOLS = {"↑", "↓", "←", "→"};
    
    /**
     * Запускает мини-игру для игрока
     */
    public static void startMinigame(PlayerEntity player, ChestBlockEntity chest, LockData lockData) {
        UUID playerId = player.getUuid();
        BlockPos chestPos = chest.getPos();
        
        // Создаём новую сессию
        LockpickSession session = new LockpickSession(chestPos, chest, lockData);
        activeSessions.put(playerId, session);
        
        // Отправляем сообщение игроку
        player.sendMessage(Text.literal("═══════════════════════════════").formatted(Formatting.GOLD), false);
        player.sendMessage(Text.literal("🔓 ВЗЛОМ ЗАМКА").formatted(Formatting.BOLD).formatted(Formatting.YELLOW), false);
        player.sendMessage(Text.literal("Тип замка: ").append(Text.literal(lockData.getLockType().asString().toUpperCase()).formatted(Formatting.GREEN)), false);
        player.sendMessage(Text.literal("Длина последовательности: ").append(Text.literal(String.valueOf(lockData.getLockSequence().length)).formatted(Formatting.AQUA)), false);
        player.sendMessage(Text.literal("Попыток осталось: ").append(Text.literal(String.valueOf(session.maxAttempts)).formatted(Formatting.RED)), false);
        player.sendMessage(Text.literal(""), false);
        player.sendMessage(Text.literal("Используйте: ").formatted(Formatting.GRAY), false);
        player.sendMessage(Text.literal("  /lockpick up    или /lockpick ↑").formatted(Formatting.WHITE), false);
        player.sendMessage(Text.literal("  /lockpick down  или /lockpick ↓").formatted(Formatting.WHITE), false);
        player.sendMessage(Text.literal("  /lockpick left  или /lockpick ←").formatted(Formatting.WHITE), false);
        player.sendMessage(Text.literal("  /lockpick right или /lockpick →").formatted(Formatting.WHITE), false);
        player.sendMessage(Text.literal("═══════════════════════════════").formatted(Formatting.GOLD), false);
        
        // Звук начала взлома
        player.playSound(SoundEvents.BLOCK_LEVER_CLICK, 1.0f, 1.0f);
    }
    
    /**
     * Обрабатывает ввод игрока
     * @return true если последовательность завершена (успех или провал)
     */
    public static boolean processInput(PlayerEntity player, String direction) {
        UUID playerId = player.getUuid();
        LockpickSession session = activeSessions.get(playerId);
        
        if (session == null) {
            player.sendMessage(Text.literal("Вы не взламываете замок!").formatted(Formatting.RED), true);
            return false;
        }
        
        // Нормализуем ввод
        String normalizedDirection = normalizeDirection(direction);
        if (normalizedDirection == null) {
            player.sendMessage(Text.literal("Неверное направление! Используйте: up, down, left, right").formatted(Formatting.RED), true);
            return false;
        }
        
        int directionIndex = getDirectionIndex(normalizedDirection);
        int expectedIndex = session.lockData.getLockSequence()[session.currentIndex];
        
        if (directionIndex == expectedIndex) {
            // Правильный ввод
            session.currentIndex++;
            
            // Показываем прогресс
            String progressBar = buildProgressBar(session);
            player.sendMessage(Text.literal(progressBar).formatted(Formatting.GREEN), true);
            
            // Звук успеха
            player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BIT.value(), 1.0f, 1.5f);
            
            // Проверяем, завершена ли последовательность
            if (session.currentIndex >= session.lockData.getLockSequence().length) {
                // УСПЕХ!
                player.sendMessage(Text.literal("✓ Замок взломан!").formatted(Formatting.BOLD).formatted(Formatting.GREEN), false);
                player.playSound(SoundEvents.BLOCK_IRON_DOOR_OPEN, 1.0f, 1.0f);
                
                // Открываем сундук
                openChestForPlayer(player, session.chest);
                
                // Снимаем замок
                LockManager.removeLock(session.chest);
                session.chest.markDirty();
                
                endSession(playerId);
                return true;
            }
            
            // Показываем сколько осталось
            int remaining = session.lockData.getLockSequence().length - session.currentIndex;
            player.sendMessage(Text.literal("Осталось: ").append(Text.literal(String.valueOf(remaining)).formatted(Formatting.AQUA)).formatted(Formatting.GRAY), true);
            
        } else {
            // Неправильный ввод
            session.failedAttempts++;
            int attemptsLeft = session.maxAttempts - session.failedAttempts;
            
            player.sendMessage(Text.literal("✗ Неверно! Попробуйте снова.").formatted(Formatting.RED), true);
            player.sendMessage(Text.literal("Попыток осталось: ").append(Text.literal(String.valueOf(attemptsLeft)).formatted(attemptsLeft <= 1 ? Formatting.RED : Formatting.YELLOW)), true);
            
            // Звук ошибки
            player.playSound(SoundEvents.BLOCK_NOTE_BLOCK_BASEDRUM.value(), 1.0f, 0.5f);
            
            // Проверяем, кончились ли попытки
            if (attemptsLeft <= 0) {
                player.sendMessage(Text.literal("✗ Отмычка сломалась! Замок остался закрытым.").formatted(Formatting.BOLD).formatted(Formatting.RED), false);
                player.playSound(SoundEvents.ENTITY_ITEM_BREAK, 1.0f, 1.0f);
                endSession(playerId);
                return true;
            }
            
            // Сбрасываем прогресс (как в Fallout - нужно начать сначала)
            session.currentIndex = 0;
            player.sendMessage(Text.literal("Последовательность сброшена! Начните сначала.").formatted(Formatting.YELLOW), true);
        }
        
        return false;
    }
    
    /**
     * Нормализует ввод игрока
     */
    private static String normalizeDirection(String input) {
        String normalized = input.toLowerCase().trim();
        
        if (normalized.equals("up") || normalized.equals("u") || normalized.equals("↑") || normalized.equals("w")) {
            return "up";
        }
        if (normalized.equals("down") || normalized.equals("d") || normalized.equals("↓") || normalized.equals("s")) {
            return "down";
        }
        if (normalized.equals("left") || normalized.equals("l") || normalized.equals("←") || normalized.equals("a")) {
            return "left";
        }
        if (normalized.equals("right") || normalized.equals("r") || normalized.equals("→") || normalized.equals("d")) {
            return "right";
        }
        
        return null;
    }
    
    /**
     * Получает индекс направления (0=up, 1=down, 2=left, 3=right)
     */
    private static int getDirectionIndex(String direction) {
        return switch (direction) {
            case "up" -> 0;
            case "down" -> 1;
            case "left" -> 2;
            case "right" -> 3;
            default -> -1;
        };
    }
    
    /**
     * Строит прогресс-бар в стиле [↑][↓][?][?]
     */
    private static String buildProgressBar(LockpickSession session) {
        StringBuilder sb = new StringBuilder("[");
        int sequenceLength = session.lockData.getLockSequence().length;
        
        for (int i = 0; i < sequenceLength; i++) {
            if (i < session.currentIndex) {
                // Уже введено - показываем символ
                int dirIndex = session.lockData.getLockSequence()[i];
                sb.append(DIRECTION_SYMBOLS[dirIndex]);
            } else {
                // Ещё не введено
                sb.append("?");
            }
            
            if (i < sequenceLength - 1) {
                sb.append("][");
            }
        }
        sb.append("]");
        
        return sb.toString();
    }
    
    /**
     * Открывает сундук для игрока
     */
    private static void openChestForPlayer(PlayerEntity player, ChestBlockEntity chest) {
        if (chest.getWorld() != null) {
            net.minecraft.block.BlockState state = chest.getCachedState();
            NamedScreenHandlerFactory screenHandlerFactory = state.createScreenHandlerFactory(chest.getWorld(), chest.getPos());
            if (screenHandlerFactory != null) {
                player.openHandledScreen(screenHandlerFactory);
            }
        }
    }
    
    /**
     * Завершает сессию и возвращает результат
     */
    public static boolean isSuccess(UUID playerId) {
        LockpickSession session = activeSessions.get(playerId);
        return session != null && session.currentIndex >= session.lockData.getLockSequence().length;
    }
    
    /**
     * Завершает сессию игрока
     */
    private static void endSession(UUID playerId) {
        activeSessions.remove(playerId);
    }
    
    /**
     * Проверяет, находится ли игрок в процессе взлома
     */
    public static boolean isPlaying(UUID playerId) {
        return activeSessions.containsKey(playerId);
    }
    
    /**
     * Отменяет мини-игру для игрока
     */
    public static void cancel(PlayerEntity player) {
        UUID playerId = player.getUuid();
        if (activeSessions.containsKey(playerId)) {
            player.sendMessage(Text.literal("Взлом отменён.").formatted(Formatting.YELLOW), true);
            endSession(playerId);
        }
    }
    
    /**
     * Получает текущую сессию игрока
     */
    public static LockpickSession getSession(UUID playerId) {
        return activeSessions.get(playerId);
    }
    
    /**
     * Сессия мини-игры
     */
    public static class LockpickSession {
        public final BlockPos chestPos;
        public final ChestBlockEntity chest;
        public final LockData lockData;
        public int currentIndex = 0;
        public int maxAttempts;
        public int failedAttempts = 0;
        
        public LockpickSession(BlockPos chestPos, ChestBlockEntity chest, LockData lockData) {
            this.chestPos = chestPos;
            this.chest = chest;
            this.lockData = lockData;
            
            // Количество попыток зависит от типа замка
            this.maxAttempts = switch (lockData.getLockType()) {
                case WOODEN -> 5;
                case IRON -> 4;
                case GOLDEN -> 3;
                case DIAMOND -> 2;
            };
        }
        
        /**
         * Проверяет, был ли взлом успешным
         */
        public boolean isSuccessful() {
            return currentIndex >= lockData.getLockSequence().length;
        }
        
        /**
         * Проверяет, закончились ли попытки
         */
        public boolean isFailed() {
            return failedAttempts >= maxAttempts;
        }
    }
}
