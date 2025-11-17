package com.ultimine_rewind.data;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * 单个方块的记录信息
 * 注意：为防止刷物品漏洞，生存模式不恢复方块实体数据
 */
public class BlockRecord {
    private final BlockPos pos;              // 方块位置
    private final BlockState state;          // 方块状态
    private final CompoundTag blockEntityData; // 方块实体数据（仅创造模式恢复）
    
    public BlockRecord(BlockPos pos, BlockState state, @Nullable CompoundTag beData) {
        this.pos = pos.immutable();
        this.state = state;
        this.blockEntityData = beData; // 保存NBT数据
    }
    
    public BlockPos getPos() {
        return pos;
    }
    
    public BlockState getState() {
        return state;
    }
    
    /**
     * 获取方块实体数据
     * 注意：只有创造模式才会使用此数据恢复
     */
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

