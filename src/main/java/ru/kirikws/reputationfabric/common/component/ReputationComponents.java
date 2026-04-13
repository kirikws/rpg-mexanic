package ru.kirikws.reputationfabric.common.component;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import net.minecraft.util.Identifier;
import ru.kirikws.reputationfabric.ReputationFabricMod;

/**
 * Registers Cardinal Components for the mod.
 * Provides unified player data component (karma + mode).
 */
public class ReputationComponents implements EntityComponentInitializer {
    public static final ComponentKey<PlayerDataComponent> PLAYER_DATA =
            ComponentRegistry.getOrCreate(new Identifier(ReputationFabricMod.MOD_ID, "player_data"), PlayerDataComponent.class);

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(
                PLAYER_DATA,
                PlayerDataComponent::new,
                RespawnCopyStrategy.INVENTORY
        );
    }
}
