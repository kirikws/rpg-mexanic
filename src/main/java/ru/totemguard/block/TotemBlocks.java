package ru.totemguard.block;

import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import ru.totemguard.TotemGuardMod;

/**
 * Registry for all TotemGuard blocks and block entities.
 */
public class TotemBlocks {
    public static final ClaimTotemBlock CLAIM_TOTEM = register(
            "claim_totem",
            new ClaimTotemBlock(AbstractBlock.Settings.create()
                    .mapColor(net.minecraft.block.MapColor.BROWN)
                    .strength(2.0f, 6.0f)
                    .luminance(state -> state.get(ClaimTotemBlock.ACTIVE) ? 14 : 0))
    );

    public static BlockEntityType<TotemBlockEntity> BLOCK_ENTITY_TYPE;

    /**
     * Must be called during mod initialization.
     */
    public static void init() {
        // Register block entity type
        BLOCK_ENTITY_TYPE = Registry.register(
                Registries.BLOCK_ENTITY_TYPE,
                new Identifier(TotemGuardMod.MOD_ID, "claim_totem"),
                FabricBlockEntityTypeBuilder.create(TotemBlockEntity::new, CLAIM_TOTEM).build()
        );
    }

    private static <T extends Block> T register(String name, T block) {
        Identifier id = new Identifier(TotemGuardMod.MOD_ID, name);
        Registry.register(Registries.BLOCK, id, block);
        // Auto-register BlockItem
        Registry.register(Registries.ITEM, id, new BlockItem(block, new Item.Settings()));
        return block;
    }
}
