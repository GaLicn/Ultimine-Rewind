package com.ultimine_rewind.data;

import net.minecraft.world.item.ItemStack;

/** 单种恢复材料及其数量。 */
public record MaterialRequirement(ItemStack stack, int count) {
    public MaterialRequirement {
        ItemStack normalized = stack.copy();
        normalized.setCount(1);
        stack = normalized;
    }
}
