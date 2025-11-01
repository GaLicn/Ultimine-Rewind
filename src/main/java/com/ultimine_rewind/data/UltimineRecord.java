package com.ultimine_rewind.data;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.*;

/**
 * 单次连锁采集的记录
 */
public class UltimineRecord {
    private final UUID playerId;
    private final long timestamp;                    // 记录时间戳
    private final List<BlockRecord> blocks;          // 被破坏的方块列表
    private final BlockPos centerPos;                // 中心位置(第一个破坏的方块)
    
    public UltimineRecord(UUID playerId, long timestamp, List<BlockRecord> blocks, BlockPos centerPos) {
        this.playerId = playerId;
        this.timestamp = timestamp;
        this.blocks = new ArrayList<>(blocks);
        this.centerPos = centerPos.immutable();
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public long getTimestamp() {
        return timestamp;
    }
    
    public List<BlockRecord> getBlocks() {
        return Collections.unmodifiableList(blocks);
    }
    
    public BlockPos getCenterPos() {
        return centerPos;
    }
    
    /**
     * 获取所有需要的物品 (合并相同物品)
     */
    public Map<Item, Integer> getRequiredItems() {
        Map<Item, Integer> items = new HashMap<>();
        for (BlockRecord record : blocks) {
            ItemStack stack = record.getRequiredItem();
            if (!stack.isEmpty()) {
                items.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        return items;
    }
    
    /**
     * 检查记录是否过期 (默认10分钟)
     */
    public boolean isExpired() {
        return isExpired(600_000); // 10分钟
    }
    
    /**
     * 检查记录是否过期
     * @param expiryTimeMs 过期时间（毫秒）
     */
    public boolean isExpired(long expiryTimeMs) {
        return System.currentTimeMillis() - timestamp > expiryTimeMs;
    }
    
    /**
     * 获取方块数量
     */
    public int getBlockCount() {
        return blocks.size();
    }
    
    /**
     * 移除已恢复的方块（从前往后移除指定数量）
     * @param count 要移除的方块数量
     * @return 新的记录（如果还有剩余方块），否则返回null
     */
    public UltimineRecord removeRestoredBlocks(int count) {
        if (count >= blocks.size()) {
            // 已经全部恢复，返回null
            return null;
        }
        
        // 创建新的记录，包含剩余的方块
        List<BlockRecord> remainingBlocks = new ArrayList<>(blocks.subList(count, blocks.size()));
        return new UltimineRecord(playerId, timestamp, remainingBlocks, centerPos);
    }
}

