package com.ultimine_rewind.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 单个方块的记录信息
 */
public class BlockRecord {
    private final BlockPos pos;              // 方块位置
    private final BlockState state;          // 方块状态
    private final CompoundTag blockEntityData; // 方块实体数据 (可为null)
    
    public BlockRecord(BlockPos pos, BlockState state, @Nullable CompoundTag beData) {
        this.pos = pos.immutable();
        this.state = state;
        this.blockEntityData = beData;
    }
    
    public BlockPos getPos() {
        return pos;
    }
    
    public BlockState getState() {
        return state;
    }
    
    @Nullable
    public CompoundTag getBlockEntityData() {
        return blockEntityData;
    }
    
    /**
     * 获取恢复此方块需要的物品
     */
    public ItemStack getRequiredItem() {
        return new ItemStack(state.getBlock().asItem());
    }
}

