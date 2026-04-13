package ru.kirikws.reputationfabric.client.registry;

import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import ru.kirikws.reputationfabric.ReputationFabricMod;

/**
 * Mod key bindings registration.
 */
public class ModKeyBindings {
    public static KeyBinding modeToggleKey;

    public static void register() {
        modeToggleKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + ReputationFabricMod.MOD_ID + ".mode_toggle",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_G,
                "category." + ReputationFabricMod.MOD_ID + ".main"
        ));
    }
}
