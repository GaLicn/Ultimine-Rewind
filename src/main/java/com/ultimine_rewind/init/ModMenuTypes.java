package com.ultimine_rewind.init;

import com.ultimine_rewind.UltimineRewind;
import com.ultimine_rewind.menu.RewindMenu;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModMenuTypes {
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, UltimineRewind.MODID);

    public static final DeferredHolder<MenuType<?>, MenuType<RewindMenu>> REWIND_MENU = MENU_TYPES.register(
            "rewind_menu", () -> new MenuType<>(RewindMenu::new, FeatureFlags.DEFAULT_FLAGS));

    private ModMenuTypes() {
    }
}
