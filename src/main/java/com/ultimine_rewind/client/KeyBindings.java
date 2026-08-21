package com.ultimine_rewind.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.ultimine_rewind.network.NetworkHandler;
import com.ultimine_rewind.network.OpenRewindScreenPacket;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.settings.KeyConflictContext;
import net.minecraftforge.client.settings.KeyModifier;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

/**
 * 快捷键绑定
 */
public class KeyBindings {
    
    public static final String KEY_CATEGORY = "key.category.ultimine_rewind.default";
    
    public static final KeyMapping REWIND_KEY = new KeyMapping(
        "key.ultimine_rewind.default.ultimine_rewind.rewind",
        KeyConflictContext.IN_GAME,
        KeyModifier.CONTROL,
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_Z,
        KEY_CATEGORY
    );
    
    @Mod.EventBusSubscriber(modid = com.ultimine_rewind.Ultimine_rewind.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ModEventBusEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(REWIND_KEY);
        }
    }
    
    @Mod.EventBusSubscriber(modid = com.ultimine_rewind.Ultimine_rewind.MODID, value = Dist.CLIENT)
    public static class ForgeEventBusEvents {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (REWIND_KEY.consumeClick()) {
                NetworkHandler.INSTANCE.sendToServer(new OpenRewindScreenPacket());
            }
        }
    }
}
