package com.ultimine_rewind.data;

import net.minecraft.world.item.ItemStack;

/** 一种恢复材料及其总需求数量。 */
public record MaterialRequirement(ItemStack stack, int count) {
    public MaterialRequirement {
        stack = stack.copyWithCount(1);
    }
}
