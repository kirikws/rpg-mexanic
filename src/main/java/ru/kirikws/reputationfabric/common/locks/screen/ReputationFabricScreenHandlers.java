package ru.kirikws.reputationfabric.common.locks.screen;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.util.Identifier;
import ru.kirikws.reputationfabric.ReputationFabricMod;

/**
 * Registry for screen handlers related to the lock system.
 */
public class ReputationFabricScreenHandlers {
    public static final ScreenHandlerType<LockedChestScreenHandler> LOCKED_CHEST_HANDLER = 
            Registry.register(
                    Registries.SCREEN_HANDLER,
                    new Identifier(ReputationFabricMod.MOD_ID, "locked_chest"),
                    new ExtendedScreenHandlerType<>((syncId, inventory, buf) -> {
                        // Read position from buffer
                        var pos = buf.readBlockPos();
                        var chest = (net.minecraft.block.entity.ChestBlockEntity) inventory.player.getWorld().getBlockEntity(pos);
                        return new LockedChestScreenHandler(syncId, inventory, chest, pos);
                    })
            );

    public static void registerScreenHandlers() {
        // Already registered via static init
    }
}
