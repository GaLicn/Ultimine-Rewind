package com.ultimine_rewind.init;

import com.ultimine_rewind.Ultimine_rewind;
import com.ultimine_rewind.menu.RewindMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * 注册菜单类型
 */
public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENUS =
            DeferredRegister.create(Registries.MENU, Ultimine_rewind.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<RewindMenu>> REWIND_MENU =
            MENUS.register("rewind_menu",
                    () -> IMenuTypeExtension.create(
                            (windowId, inv, data) -> new RewindMenu(windowId, inv))
            );

    private ModMenuTypes() {}
}
