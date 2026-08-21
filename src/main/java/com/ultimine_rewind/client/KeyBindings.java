package com.ultimine_rewind.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.ultimine_rewind.UltimineRewind;
import com.ultimine_rewind.network.OpenRewindScreenPacket;
import dev.ftb.mods.ftblibrary.platform.client.PlatformClient;
import dev.ftb.mods.ftblibrary.platform.network.Play2ServerNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath(UltimineRewind.MODID, "default"));
    private static final KeyMapping REWIND = new KeyMapping(
            "key.ultimine_rewind.default.ultimine_rewind.rewind",
            KeyConflictContext.IN_GAME, KeyModifier.CONTROL,
            InputConstants.Type.KEYSYM, InputConstants.KEY_Z, CATEGORY);

    private KeyBindings() {
    }

    public static void register() {
        PlatformClient.get().input().registerKeyMapping(UltimineRewind.MODID, REWIND);
        NeoForge.EVENT_BUS.addListener(InputEvent.Key.class, KeyBindings::onKey);
    }

    private static void onKey(InputEvent.Key event) {
        var keyEvent = event.getKeyEvent();
        if (event.getAction() == GLFW.GLFW_PRESS
                && REWIND.matches(keyEvent) && REWIND.isDown()) {
            Play2ServerNetworking.send(new OpenRewindScreenPacket());
        }
    }
}
