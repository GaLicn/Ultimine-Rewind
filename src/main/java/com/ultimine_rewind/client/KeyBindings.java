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
import net.neoforged.neoforge.client.settings.KeyConflictContext;
import net.neoforged.neoforge.client.settings.KeyModifier;
import net.neoforged.neoforge.network.PacketDistributor;
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
                // 原生组合键映射会验证 Ctrl 修饰键，并支持玩家自行重绑。
                PacketDistributor.sendToServer(OpenRewindScreenPacket.INSTANCE);
            }
        }
    }
}
