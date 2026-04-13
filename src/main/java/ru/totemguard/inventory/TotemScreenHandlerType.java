package ru.totemguard.inventory;

import net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;
import ru.totemguard.TotemGuardMod;

/**
 * Registry for screen handler types.
 */
public class TotemScreenHandlerType {
    public static ExtendedScreenHandlerType<TotemScreenHandler> TYPE;

    public static void register() {
        TYPE = Registry.register(
                Registries.SCREEN_HANDLER,
                new Identifier(TotemGuardMod.MOD_ID, "totem"),
                new ExtendedScreenHandlerType<>(TotemScreenHandler.BUFFER_CODEC)
        );
    }
}
