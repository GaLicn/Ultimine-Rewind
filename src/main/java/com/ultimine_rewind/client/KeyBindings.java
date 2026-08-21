package com.ultimine_rewind.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.ultimine_rewind.UltimineRewind;
import com.ultimine_rewind.network.OpenRewindScreenPacket;
import dev.ftb.mods.ftblibrary.platform.client.PlatformClient;
import dev.ftb.mods.ftblibrary.platform.network.Play2ServerNetworking;
import dev.ftb.mods.ftblibrary.platform.client.input.InputHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.lwjgl.glfw.GLFW;

public final class KeyBindings {
    private static final KeyMapping.Category CATEGORY = new KeyMapping.Category(
            Identifier.fromNamespaceAndPath(UltimineRewind.MODID, "default"));
    private static final KeyMapping REWIND = InputHelper.createSimpleKeyMapping(
            "ultimine_rewind.rewind", CATEGORY, InputConstants.KEY_Z);

    private KeyBindings() {
    }

    public static void register() {
        PlatformClient.get().input().registerKeyMapping(UltimineRewind.MODID, REWIND);
        NeoForge.EVENT_BUS.addListener(InputEvent.Key.class, KeyBindings::onKey);
    }

    private static void onKey(InputEvent.Key event) {
        var keyEvent = event.getKeyEvent();
        if (event.getAction() == GLFW.GLFW_PRESS
                && (keyEvent.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0
                && REWIND.matches(keyEvent)) {
            Play2ServerNetworking.send(new OpenRewindScreenPacket());
        }
    }
}
