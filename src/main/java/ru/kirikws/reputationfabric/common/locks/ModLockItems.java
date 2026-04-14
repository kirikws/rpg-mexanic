package ru.kirikws.reputationfabric.common.locks;

import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import ru.kirikws.reputationfabric.ReputationFabricMod;
import ru.kirikws.reputationfabric.common.locks.item.KeyItem;
import ru.kirikws.reputationfabric.common.locks.item.LockItem;
import ru.kirikws.reputationfabric.common.locks.item.LockpickItem;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Registry for all lock system items.
 */
public class ModLockItems {
    private static final Map<Item, Identifier> ITEMS = new LinkedHashMap<>();

    // Locks
    public static final LockItem WOODEN_LOCK = register(new LockItem(LockType.WOODEN, new Item.Settings()), "wooden_lock");
    public static final LockItem IRON_LOCK = register(new LockItem(LockType.IRON, new Item.Settings()), "iron_lock");
    public static final LockItem GOLDEN_LOCK = register(new LockItem(LockType.GOLDEN, new Item.Settings()), "golden_lock");
    public static final LockItem DIAMOND_LOCK = register(new LockItem(LockType.DIAMOND, new Item.Settings()), "diamond_lock");

    // Keys
    public static final KeyItem WOODEN_KEY = register(new KeyItem(LockType.WOODEN, new Item.Settings()), "wooden_key");
    public static final KeyItem IRON_KEY = register(new KeyItem(LockType.IRON, new Item.Settings()), "iron_key");
    public static final KeyItem GOLDEN_KEY = register(new KeyItem(LockType.GOLDEN, new Item.Settings()), "golden_key");
    public static final KeyItem DIAMOND_KEY = register(new KeyItem(LockType.DIAMOND, new Item.Settings()), "diamond_key");

    // Lockpicks
    public static final LockpickItem LOCKPICK = register(new LockpickItem(new Item.Settings().maxCount(16)), "lockpick");

    // Creative Tab
    public static final ItemGroup LOCK_GROUP = FabricItemGroup.builder()
            .icon(() -> new ItemStack(WOODEN_LOCK))
            .displayName(Text.translatable("itemGroup.reputation-fabric.locks"))
            .entries((context, entries) -> {
                entries.add(WOODEN_LOCK);
                entries.add(IRON_LOCK);
                entries.add(GOLDEN_LOCK);
                entries.add(DIAMOND_LOCK);
                entries.add(WOODEN_KEY);
                entries.add(IRON_KEY);
                entries.add(GOLDEN_KEY);
                entries.add(DIAMOND_KEY);
                entries.add(LOCKPICK);
            })
            .build();

    private static <T extends Item> T register(T item, String id) {
        ITEMS.put(item, new Identifier(ReputationFabricMod.MOD_ID, id));
        return item;
    }

    public static KeyItem getKeyForType(LockType lockType) {
        return switch (lockType) {
            case WOODEN -> WOODEN_KEY;
            case IRON -> IRON_KEY;
            case GOLDEN -> GOLDEN_KEY;
            case DIAMOND -> DIAMOND_KEY;
        };
    }

    public static void registerItems() {
        for (Map.Entry<Item, Identifier> entry : ITEMS.entrySet()) {
            Registry.register(Registries.ITEM, entry.getValue(), entry.getKey());
        }

        Registry.register(Registries.ITEM_GROUP,
                new Identifier(ReputationFabricMod.MOD_ID, "locks"), LOCK_GROUP);
    }
}
