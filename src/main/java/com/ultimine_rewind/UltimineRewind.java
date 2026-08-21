package com.ultimine_rewind;

import com.mojang.logging.LogUtils;
import com.ultimine_rewind.init.ModMenuTypes;
import com.ultimine_rewind.network.NetworkHandler;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

@Mod(UltimineRewind.MODID)
public class UltimineRewind {
    public static final String MODID = "ultimine_rewind";
    public static final Logger LOGGER = LogUtils.getLogger();

    public UltimineRewind(IEventBus modEventBus) {
        ModMenuTypes.MENU_TYPES.register(modEventBus);
        NetworkHandler.register();
        LOGGER.info("Ultimine Rewind 已加载");
    }
}
