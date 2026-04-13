package ru.totemguard.client;

import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.network.PacketByteBuf;
import org.lwjgl.glfw.GLFW;
import ru.totemguard.network.TotemNetworking;

/**
 * Registers client-side keybindings.
 */
public class KeyBindings {
    public static KeyBinding TOGGLE_BOUNDARIES;
    public static boolean boundariesVisible = false;

    public static void register() {
        TOGGLE_BOUNDARIES = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.totemguard.toggle_boundaries",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_V,
                "category.totemguard.main"
        ));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (TOGGLE_BOUNDARIES.wasPressed()) {
                boundariesVisible = !boundariesVisible;
                
                if (boundariesVisible && client.player != null) {
                    // Запрашиваем данные у сервера при включении
                    PacketByteBuf buf = new PacketByteBuf(io.netty.buffer.Unpooled.buffer());
                    ClientPlayNetworking.send(TotemNetworking.REQUEST_BOUNDARIES_PACKET_ID, buf);
                } else {
                    // Очищаем кэш при выключении
                    ClientRegionCache.update(new java.util.ArrayList<>());
                }
            }
        });
    }
}
