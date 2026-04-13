# Документация кодовой базы мода

## Общая информация

- **Minecraft:** 1.20.1
- **Загрузчик:** Fabric
- **Версия мода:** 1.0.0
- **Автор:** kirikws
- **ID мода:** `reputation-fabric`
- **Язык:** Java 17

---

## Описание

Мод добавляет две интегрированные системы:

1. **Система репутации (Reputation Fabric)** — карма игрока влияет на PvP-бои, торговлю с NPC и визуальные эффекты. Игрок может переключаться между режимами PASSIVE и PVP.
2. **TotemGuard** — установка Claim Totem создаёт защищённую территорию. Тотем требует валюту для обслуживания, а гости чужих регионов постепенно теряют карму.

Системы связаны: карма влияет на поведение гостей, а режимы PASSIVE/PVP интегрированы в PvP-защиту тотемов.

---

## Структура проекта

```
src/main/java/ru/
├── kirikws/reputationfabric/          # Система репутации
│   ├── api/ReputationAPIs.java        # Публичное API (статические методы)
│   ├── common/
│   │   ├── component/
│   │   │   ├── PlayerDataComponent.java   # Cardinal Component (карма + режим)
│   │   │   ├── ReputationComponents.java  # Регистрация компонентов
│   │   │   └── data/PlayerData.java       # Enum PlayerMode
│   │   ├── event/
│   │   │   ├── ReputationEventHandler.java # События сервера
│   │   │   └── CombatRuleHandler.java      # Правила PvP
│   │   ├── item/
│   │   │   ├── KarmaCompassItem.java       # Предмет компаса
│   │   │   ├── TradeModifierHelper.java    # Модификатор цен торговли
│   │   │   ├── ModItems.java               # Регистрация предметов
│   │   │   └── ModTradeOffers.java         # Предложения торговли
│   │   ├── network/
│   │   │   ├── ReputationNetworking.java   # Регистрация пакетов
│   │   │   └── packet/                     # Классы пакетов
│   │   └── registry/ModCommands.java       # Команды сервера
│   ├── client/
│   │   ├── ReputationFabricClient.java     # Точка входа клиента
│   │   ├── event/ClientEventHandler.java   # События клиента
│   │   ├── network/ClientNetworking.java   # Приём S2C пакетов + StateHolder
│   │   ├── registry/ModKeyBindings.java    # Клавиша G
│   │   ├── render/
│   │   │   ├── CompassHudOverlay.java      # HUD компаса
│   │   │   ├── ModeHudOverlay.java         # HUD режима
│   │   │   └── KarmaParticleRenderer.java  # Частицы кармы
│   │   └── mixin/PlayerRendererMixin.java  # Инжекция в рендер игрока
│   └── config/ReputationConfig.java        # Конфигурация репутации
└── totemguard/                          # Система защиты территорий
    ├── TotemGuardMod.java               # Точка входа
    ├── block/
    │   ├── ClaimTotemBlock.java         # Блок тотема
    │   ├── TotemBlockEntity.java        # BlockEntity тотема
    │   └── TotemBlocks.java             # Регистрация блоков
    ├── config/TotemConfig.java          # Конфигурация тотемов
    ├── storage/
    │   ├── RegionState.java             # Хранение регионов (NBT)
    │   └── TrustedManager.java          # Система доверия
    ├── geometry/
    │   ├── CuboidRegion.java            # Кубоидная область
    │   └── UnionedRegion.java           # Объединение кубоидов
    ├── item/
    │   ├── CurrencyItem.java            # Базовый класс валюты
    │   └── TotemItems.java              # Медная монета, серебряный сигил, золотой идол
    ├── inventory/
    │   ├── TotemScreenHandler.java      # Экран тотема (1 слот валюты)
    │   └── TotemScreenHandlerType.java  # Регистрация типа экрана
    ├── protection/ProtectionHandler.java # Защита территорий
    ├── guest/
    │   ├── GuestTracker.java            # Трекинг гостей
    │   └── ProtectionRules.java         # Флаги intruder
    ├── network/
    │   ├── TotemNetworking.java         # ID пакетов
    │   └── packet/RequestBoundariesHandler.java
    ├── command/
    │   ├── TotemCommands.java           # Команды тотемов
    │   └── TotemPlaceCommand.java       # /totem place
    ├── recipe/TotemRecipes.java         # Рецепты (datapack JSON)
    └── client/
        ├── TotemGuardClient.java        # Точка входа клиента
        ├── KeyBindings.java             # Клавиша V
        ├── ClientRegionCache.java       # Кэш регионов
        ├── RegionRenderer.java          # Рендер границ
        └── screen/TotemScreen.java      # GUI тотема
```

---

## Система репутации

### Karma

Карма — целое число, хранящееся в Cardinal Component. Положительное значение = хорошая репутация, отрицательное = плохая.

#### Начисление и списание

| Событие | Эффект |
|---------|--------|
| Атака игрока с неотрицательной кармой | −5 к атакующему |
| Убийство игрока с хорошей/нейтральной кармой | −50 к убийце |
| Убийство игрока с плохой кармой | Бонус: `max(10, \|карма_жертвы\| × 0.5)` |

#### Влияние на торговлю

| Карма | Эффект |
|-------|--------|
| `≥ 30` | Скидка (множитель 0.8) |
| `0` | Базовые цены |
| `< 0` | Наценка (до ×1.5 при приближении к порогу) |
| `< −50` | Торговля заблокирована |

#### Визуальные эффекты

| Карма | Эффект |
|-------|--------|
| `≥ 30` | Частицы HAPPY_VILLAGER над головой |
| `≤ −30` | Частицы CAMPFIRE_COSY_SMOKE у ног |

В режиме PASSIVE частицы не отображаются.

### Режимы игрока

| Режим | Описание |
|-------|----------|
| **PASSIVE** | Не может атаковать других игроков и быть атакованным |
| **PVP** | Обычные правила боя |

**Переключение:** клавиша `G`. Требования:
- Игрок не двигается 5 секунд (100 тиков)
- Не получал урон последние 5 секунд

### Карма-компас

Предмет, продаваемый у картографа (3-й уровень, 15 изумрудов).

При использовании ищет ближайшего игрока с кармой `≤ −20` в радиусе 500 блоков. Отправляет координаты клиенту, который отображает стрелку в HUD. Кулдаун — 30 секунд.

### Публичное API

Класс `ReputationAPIs` — единственная точка доступа для других модов или внутренних систем.

```java
// Получение кармы
int karma = ReputationAPIs.getKarma(player);

// Установка кармы
ReputationAPIs.setKarma(player, 100);

// Изменение кармы (delta добавляется к текущему значению)
ReputationAPIs.modifyKarma(player, -5);

// Проверки порогов
boolean bad = ReputationAPIs.hasBadKarma(player);   // karma <= -30
boolean good = ReputationAPIs.hasGoodKarma(player);  // karma >= 30

// Режим игрока
PlayerMode mode = ReputationAPIs.getMode(player);
boolean isPassive = ReputationAPIs.isPassive(player);
boolean isPvP = ReputationAPIs.isPvP(player);
ReputationAPIs.setMode(player, PlayerMode.PVP);

// Запрос переключения режима (с проверкой неподвижности и кулдауна)
boolean success = ReputationAPIs.requestModeSwitch(player);

// Поиск ближайшего игрока с низкой кармой
PlayerEntity target = ReputationAPIs.findNearestLowKarmaPlayer(
    searcher,    // ServerPlayerEntity
    500.0,       // радиус поиска
    -20          // порог кармы
);

// Расчёт кармы за бой
int penalty = ReputationAPIs.calculateAttackKarmaPenalty(attacker, target);
int killDelta = ReputationAPIs.calculateKillKarma(killer, victim);
```

---

## TotemGuard

### Claim Totem

Блок, создающий защищённую территорию при установке.

#### Свойства блока

- **Базовый радиус:** 16 блоков (настраивается)
- **Высота региона:** −64 до 319 (полная высота мира)
- **Обслуживание:** каждые 60 секунд списывает 1 единицу валюты из слота тотема
- **Пустой слот:** регион деактивируется

#### Валюта

| Предмет | Множитель радиуса |
|---------|-------------------|
| Медная монета (COPPER_COIN) | ×1.0 |
| Серебряный сигил (SILVER_SIGIL) | ×1.5 |
| Золотой идол (GOLDEN_IDOL) | ×2.5 |

#### Рецепты крафта

Все рецепты загружаются из JSON datapacks в `data/totemguard/recipes/`.

### Защита территорий

| Действие | Разрешено |
|----------|-----------|
| Ломать блоки в своём регионе | ✅ Владелец и доверенные |
| Ломать блоки в чужом регионе | ❌ |
| Использовать контейнеры в чужом регионе | ❌ |
| Атаковать в PASSIVE режиме | ❌ |
| Атаковать игрока в PASSIVE | ❌ |
| Атаковать flagged intruder | ✅ (без штрафа кармы) |

### Система доверия

Владелец может добавить доверенных игроков, которые получат полный доступ к региону.

### Система гостей

При входе в чужой регион:

| Время | Эффект |
|-------|--------|
| 0–30 сек | Уведомление о входе (жёлтый текст + звук) |
| 30–60 сек | Предупреждение о скором штрафе (красный текст + звук наковальни) |
| >60 сек | Списание 1 кармы/минуту + флаг "intruder" |

Игрок с флагом intruder может быть атакован владельцами регионов без штрафа кармы. При выходе из региона флаг снимается.

### Объединение регионов

Несколько тотемов одного владельца автоматически объединяются в единый регион. Пересекающиеся области сливаются итеративно.

### Визуализация границ

Клавиша `V` переключает отображение:
- **Зелёные линии** — собственные регионы
- **Красные линии** — чужие регионы

Рендер выполняется через выпуклую оболочку (алгоритм Грэхема) в плоскости XZ.

---

## Конфигурация

### reputation-fabric.json

Файл: `config/reputation-fabric.json`

| Параметр | По умолчанию | Описание |
|----------|-------------|----------|
| `karma_damage_penalty` | 5 | Штраф за атаку игрока с неотрицательной кармой |
| `karma_kill_penalty` | 50 | Штраф за убийство игрока с хорошей/нейтральной кармой |
| `karma_kill_bonus_multiplier` | 0.5 | Множитель бонуса за убийство игрока с плохой кармой |
| `karma_kill_bonus_min` | 10 | Минимальный бонус за убийство игрока с плохой кармой |
| `threshold_bad` | −30 | Порог «плохой» кармы |
| `threshold_good` | 30 | Порог «хорошей» кармы |
| `trade_penalty_multiplier` | 1.5 | Максимальный множитель наценки |
| `trade_bonus_multiplier` | 0.8 | Множитель скидки при хорошей карме |
| `trade_block_threshold` | −50 | Порог блокировки торговли |
| `compass_radius` | 500 | Радиус поиска компаса |
| `compass_target_karma` | −20 | Порог кармы цели компаса |
| `compass_cooldown` | 600 | Кулдаун компаса в тиках (30 сек) |
| `mode_switch_immobile_time` | 100 | Тиков неподвижности для смены режима (5 сек) |

### totemguard.json

Файл: `config/totemguard.json`

| Параметр | По умолчанию | Описание |
|----------|-------------|----------|
| `base_radius` | 16 | Базовый радиус региона |
| `drain_interval_ticks` | 1200 | Интервал расхода валюты в тиках (60 сек) |
| `guest_grace_period_seconds` | 30 | Льготный период без штрафа |
| `guest_karma_warning_seconds` | 60 | Время до начала списания кармы |
| `guest_karma_drain_per_minute` | 1 | Списание кармы в минуту |
| `boundary_view_radius` | 64 | Радиус запроса границ для рендера |
| `copper_multiplier` | 1.0 | Множитель радиуса медной монеты |
| `silver_multiplier` | 1.5 | Множитель радиуса серебряного сигила |
| `gold_multiplier` | 2.5 | Множитель радиуса золотого идола |

---

## Сетевая архитектура

### Сервер → Клиент (S2C)

| Пакет | ID | Данные | Назначение |
|-------|-----|--------|------------|
| SyncKarma | `reputation-fabric:sync_karma` | `int karma` | Синхронизация кармы |
| SyncMode | `reputation-fabric:sync_mode` | `String mode` | Синхронизация режима |
| CompassTarget | `reputation-fabric:compass_target` | `double x, y, z` + `int durationTicks` | Координаты цели компаса |
| SyncBoundaries | `totemguard:sync_boundaries` | `int count` + `Region[]` | Границы регионов |

### Клиент → Сервер (C2S)

| Пакет | ID | Данные | Назначение |
|-------|-----|--------|------------|
| SwitchMode | `reputation-fabric:switch_mode` | — | Запрос смены режима |
| RequestBoundaries | `totemguard:request_boundaries` | — | Запрос границ регионов |

### Структура Region в SyncBoundaries

```
int count                                // количество регионов
для каждого региона:
    UUID owner                           // владелец
    boolean isOwner                      // текущий игрок — владелец
    int pointCount                       // количество точек оболочки
    для каждой точки:
        float x, float z                 // координаты на XZ плоскости
```

---

## Команды

### Reputation Fabric

Все команды требуют permission level 2 (оператор или cheat-enabled мир).

| Команда | Описание |
|---------|----------|
| `/reputation-fabric karma get <player>` | Показать карму игрока |
| `/reputation-fabric karma set <player> <value>` | Установить карму |
| `/reputation-fabric mode set <player> <passive\|pvp>` | Установить режим |
| `/reputation-fabric reload` | Перезагрузить конфигурацию |

### TotemGuard

| Команда | Permission | Описание |
|---------|-----------|----------|
| `/totem trust <player>` | Владелец региона или ОП | Добавить игрока в доверенные |
| `/totem untrust <player>` | Владелец региона или ОП | Удалить из доверенных |
| `/totem setowner <player>` | **2** (только ОП) | Передать владение регионом |
| `/totem info` | Любой игрок | Информация о регионе |
| `/totem place` | Любой игрок | Поставить тотем под ногами |

---

## Cardinal Components

### PlayerDataComponent

Единый компонент для хранения данных игрока.

**Ключ:** `reputation-fabric:player_data`

**Поля:**

| Поле | Тип | Значение по умолчанию | Описание |
|------|-----|----------------------|----------|
| `karma` | int | 0 | Значение кармы |
| `mode` | PlayerMode | PASSIVE | Текущий режим |
| `lastModeChangeTime` | long | 0 | Время последнего переключения (мс) |
| `lastDamageTakenTime` | long | 0 | Время последнего получения урона (мс) |

**NBT-структура:**

```
reputation-fabric (Compound)
├── karma (Int)
├── mode (String) — "PASSIVE" или "PVP"
├── last_mode_change (Long)
└── last_damage_taken (Long)
```

**Копирование при респавне:** `RespawnCopyStrategy.INVENTORY`

### Регистрация

В `fabric.mod.json`:

```json
"entrypoints": {
    "cardinal-components": [
        "ru.kirikws.reputationfabric.common.component.ReputationComponents"
    ]
},
"custom": {
    "cardinal-components": [
        "reputation-fabric:player_data"
    ]
}
```

---

## Хранение данных

### Регионы тотемов

- **Файл:** `<world>/totemguard_regions.dat`
- **Формат:** NBT
- **Структура:**

```
Data (Compound)
└── owners (List)
    └── [0] (Compound)
        ├── owner (Uuid)
        └── totems (List)
            └── [0] (Compound)
                ├── pos (IntArray) — [x, y, z]
                ├── owner (Uuid)
                └── radius (Int)
```

### Доверенные игроки

- **Файл:** `<world>/totemguard_trust.dat`
- **Формат:** NBT
- **Структура:** `Map<UUID владельца, Set<UUID доверенных>>`

---

## Клиентские компоненты

### HUD-элементы

| Элемент | Позиция | Описание |
|---------|---------|----------|
| ModeHudOverlay | Левый центр экрана | Зелёный квадрат с текстом «PASSIVE» или красный с мечом и «PvP» |
| CompassHudOverlay | Правый верхний угол | Стрелка к цели компаса + расстояние в блоках |

### Клавиши

| Клавиша | Действие |
|---------|----------|
| `G` | Запросить переключение PASSIVE ↔ PVP |
| `V` | Переключить отображение границ регионов |

### Частицы

Спавнятся каждые 10 тиков (через `player.age % 10`) для локального игрока:

| Карма | Тип частиц | Позиция |
|-------|-----------|---------|
| `≤ −30` | CAMPFIRE_COSY_SMOKE | У ног (y + 0.1) |
| `≥ 30` | HAPPY_VILLAGER | Над головой (y + height + 0.3) |

---

## Миксины

### PlayerRendererMixin

- **Цель:** `net.minecraft.client.render.entity.LivingEntityRenderer`
- **Метод:** `render(LivingEntity, float, float, MatrixStack, VertexConsumerProvider, int)`
- **Точка инжекции:** `@At("TAIL")`
- **Действие:** Вызывает `KarmaParticleRenderer.spawnParticles()` для PlayerEntity

---

## Зависимости

| Мод | Версия | Назначение |
|-----|--------|------------|
| Fabric Loader | ≥ 0.15.11 | Загрузчик модов |
| Minecraft | 1.20.1 | Целевая версия |
| Fabric API | ≥ 0.92.0 | События, сеть, рендеринг |
| Cardinal Components Base | ≥ 5.2.0 | Система компонентов |
| Cardinal Components Entity | ≥ 5.2.0 | Компоненты для сущностей |
| Cloth Config | ≥ 11.0.0 | (опционально) GUI конфигурации |

---

## Сборка и запуск

```bash
# Сборка JAR
gradlew build

# Запуск клиента
gradlew runClient

# Запуск сервера
gradlew runServer
```

Собранный JAR: `build/libs/reputation-fabric-1.0.0.jar` (~232 КБ)

---

## Интеграция между системами

### TotemGuard ↔ Reputation Fabric

| Точка интеграции | Описание |
|------------------|----------|
| **PvP в регионах** | ProtectionHandler проверяет режим PASSIVE через `ReputationAPIs.isPassive()` |
| **Гости регионов** | GuestTracker списывает карму через `ReputationAPIs.modifyKarma()` |
| **Intruder-флаг** | Игроки с флагом могут быть атакованы без штрафа кармы |

### Взаимосвязь потоков

```
Игрок входит в чужой регион
    → GuestTracker: отсчёт таймера
        → > 60 сек: ReputationAPIs.modifyKarma(player, -1/мин)
        → > 60 сек: ProtectionRules.flagIntruder(playerId)

Игрок атакует в регионе
    → ProtectionHandler: проверка PASSIVE через ReputationAPIs.isPassive()
        → PASSIVE: отмена атаки
    → Если intruder: атака разрешена
```
