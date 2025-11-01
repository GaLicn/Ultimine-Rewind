package com.ultimine_rewind.init;

import com.ultimine_rewind.Ultimine_rewind;
import com.ultimine_rewind.menu.RewindMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

/**
 * 注册菜单类型
 */
public class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES = 
        DeferredRegister.create(ForgeRegistries.MENU_TYPES, Ultimine_rewind.MODID);
    
    public static final RegistryObject<MenuType<RewindMenu>> REWIND_MENU = MENU_TYPES.register(
        "rewind_menu",
        () -> IForgeMenuType.create((windowId, inv, data) -> new RewindMenu(windowId, inv))
    );
}

