package com.ultimine_rewind.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.ultimine_rewind.Ultimine_rewind;
import com.ultimine_rewind.network.OpenRewindScreenPacket;
import net.minecraft.client.KeyMapping;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.lwjgl.glfw.GLFW;

/**
 * 快捷键绑定
 */
public class KeyBindings {
    
    public static final String KEY_CATEGORY = "key.categories.ultimine_rewind";
    
    public static final KeyMapping REWIND_KEY = new KeyMapping(
        "key.ultimine_rewind.rewind",
        InputConstants.Type.KEYSYM,
        GLFW.GLFW_KEY_Z,
        KEY_CATEGORY
    );
    
    @EventBusSubscriber(modid = Ultimine_rewind.MODID, value = Dist.CLIENT)
    public static class ModEventBusEvents {
        @SubscribeEvent
        public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
            event.register(REWIND_KEY);
        }
    }
    
    @EventBusSubscriber(modid = Ultimine_rewind.MODID, value = Dist.CLIENT)
    public static class ForgeEventBusEvents {
        @SubscribeEvent
        public static void onKeyInput(InputEvent.Key event) {
            if (REWIND_KEY.consumeClick()) {
                // 检查是否按住 Ctrl 键
                boolean ctrlPressed = (event.getModifiers() & GLFW.GLFW_MOD_CONTROL) != 0;
                
                if (ctrlPressed) {
                    // 发送打开界面请求到服务端
                    PacketDistributor.sendToServer(OpenRewindScreenPacket.INSTANCE);
                }
            }
        }
    }
}

