package com.ultimine_rewind.data;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 单次连锁采集的记录
 *
 * @param timestamp 记录时间戳
 * @param blocks    被破坏的方块列表
 * @param centerPos 中心位置(第一个破坏的方块)
 */
public record UltimineRecord(UUID playerId, long timestamp, List<BlockRecord> blocks, BlockPos centerPos) {
    public UltimineRecord {
        blocks = List.copyOf(blocks);
        centerPos = centerPos.immutable();
    }

    /**
     * 获取所有需要的物品 (合并相同物品)
     */
    public List<MaterialRequirement> requiredMaterials() {
        List<MaterialRequirement> result = new ArrayList<>();
        for (BlockRecord record : blocks) {
            ItemStack stack = record.requiredItem();
            if (!stack.isEmpty()) {
                int index = findMaterial(result, stack);
                if (index < 0) {
                    result.add(new MaterialRequirement(stack, stack.getCount()));
                } else {
                    MaterialRequirement material = result.get(index);
                    result.set(index, new MaterialRequirement(material.stack(), material.count() + stack.getCount()));
                }
            }
        }
        return List.copyOf(result);
    }

    /** 供旧版客户端同步界面使用的按物品聚合数据。 */
    public Map<net.minecraft.world.item.Item, Integer> getRequiredItems() {
        Map<net.minecraft.world.item.Item, Integer> items = new LinkedHashMap<>();
        for (MaterialRequirement material : requiredMaterials()) {
            items.merge(material.stack().getItem(), material.count(), Integer::sum);
        }
        return items;
    }

    private static int findMaterial(List<MaterialRequirement> materials, ItemStack stack) {
        for (int index = 0; index < materials.size(); index++) {
            if (ItemStack.isSameItemSameComponents(materials.get(index).stack(), stack)) {
                return index;
            }
        }
        return -1;
    }

    /**
     * 检查记录是否过期 (默认10分钟)
     */
    public boolean isExpired() {
        return isExpired(600_000); // 10分钟
    }

    /**
     * 检查记录是否过期
     *
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
     *
     * @param count 要移除的方块数量
     * @return 新的记录（如果还有剩余方块），否则返回null
     */
    public UltimineRecord withoutRestored(List<BlockRecord> restored) {
        List<BlockRecord> remaining = new ArrayList<>(blocks);
        remaining.removeAll(restored);
        return remaining.isEmpty() ? null : new UltimineRecord(playerId, timestamp, remaining, centerPos);
    }
}
