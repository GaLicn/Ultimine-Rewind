package com.ultimine_rewind.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** 单个被连锁破坏方块的快照。 */
public record BlockRecord(BlockPos pos, BlockState state, ItemStack requiredItem,
                          @Nullable CompoundTag blockEntityData) {
    public BlockRecord {
        pos = pos.immutable();
        requiredItem = requiredItem.copyWithCount(requiredItem.isEmpty() ? 0 : 1);
        blockEntityData = blockEntityData == null ? null : blockEntityData.copy();
    }
}
