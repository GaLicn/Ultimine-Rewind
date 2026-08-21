package com.ultimine_rewind.data;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 一次连锁采集的可撤销记录。 */
public record UltimineRecord(UUID playerId, long timestamp, List<BlockRecord> blocks, BlockPos centerPos) {
    private static final long EXPIRY_MILLIS = 600_000L;

    public UltimineRecord {
        blocks = List.copyOf(blocks);
        centerPos = centerPos.immutable();
    }

    public Map<Item, Integer> requiredItems() {
        Map<Item, Integer> result = new LinkedHashMap<>();
        for (BlockRecord block : blocks) {
            ItemStack stack = block.requiredItem();
            if (!stack.isEmpty()) {
                result.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        return result;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() - timestamp > EXPIRY_MILLIS;
    }

    public int blockCount() {
        return blocks.size();
    }

    public UltimineRecord withoutRestored(List<BlockRecord> restored) {
        List<BlockRecord> remaining = new ArrayList<>(blocks);
        remaining.removeAll(restored);
        return remaining.isEmpty() ? null : new UltimineRecord(playerId, timestamp, remaining, centerPos);
    }
}
