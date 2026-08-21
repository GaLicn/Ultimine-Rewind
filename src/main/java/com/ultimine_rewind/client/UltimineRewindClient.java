package com.ultimine_rewind.client;

import com.ultimine_rewind.UltimineRewind;
import com.ultimine_rewind.client.screen.RewindScreen;
import com.ultimine_rewind.init.ModMenuTypes;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

@Mod(value = UltimineRewind.MODID, dist = Dist.CLIENT)
public final class UltimineRewindClient {
    public UltimineRewindClient(IEventBus modEventBus) {
        KeyBindings.register();
        modEventBus.addListener(RegisterMenuScreensEvent.class,
                event -> event.register(ModMenuTypes.REWIND_MENU.get(), RewindScreen::new));
    }
}
