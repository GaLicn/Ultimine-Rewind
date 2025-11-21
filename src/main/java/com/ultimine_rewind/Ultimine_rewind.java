package com.ultimine_rewind;

import com.mojang.logging.LogUtils;
import com.ultimine_rewind.client.screen.RewindScreen;
import com.ultimine_rewind.init.ModMenuTypes;
import com.ultimine_rewind.init.NetworkHandler;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import org.slf4j.Logger;

@Mod(Ultimine_rewind.MODID)
public class Ultimine_rewind {

    public static final String MODID = "ultimine_rewind";
    private static final Logger LOGGER = LogUtils.getLogger();

    public Ultimine_rewind(IEventBus modEventBus, ModContainer modContainer) {
        modEventBus.addListener(this::commonSetup);

        // Register menu types
        ModMenuTypes.MENUS.register(modEventBus);

        NeoForge.EVENT_BUS.register(this);
        modEventBus.addListener(NetworkHandler::registerPayloadHandlers);
        // 注册配置：接入自定义的 ModConfigs
//        modContainer.registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_SPEC, "useless_mod-client.toml");
    }


    private void commonSetup(final FMLCommonSetupEvent event) {
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }

    @EventBusSubscriber(modid = MODID, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onRegisterScreens(RegisterMenuScreensEvent event) {
            // 菜单 -> 屏幕 绑定
            event.register(
                    ModMenuTypes.REWIND_MENU.get(),
                    RewindScreen::new
            );
        }
    }

    public static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(MODID, path);
    }
}
