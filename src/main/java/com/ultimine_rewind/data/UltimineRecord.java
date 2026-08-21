package com.ultimine_rewind.data;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** 一次连锁采集的可撤销记录。 */
public record UltimineRecord(UUID playerId, long timestamp, List<BlockRecord> blocks, BlockPos centerPos) {
    private static final long EXPIRY_MILLIS = 600_000L;

    public UltimineRecord {
        blocks = List.copyOf(blocks);
        centerPos = centerPos.immutable();
    }

    public List<MaterialRequirement> requiredMaterials() {
        List<MaterialRequirement> result = new ArrayList<>();
        for (BlockRecord block : blocks) {
            ItemStack stack = block.requiredItem();
            if (stack.isEmpty()) {
                continue;
            }

            int existingIndex = findMaterial(result, stack);
            if (existingIndex < 0) {
                result.add(new MaterialRequirement(stack, stack.getCount()));
            } else {
                MaterialRequirement existing = result.get(existingIndex);
                result.set(existingIndex, new MaterialRequirement(existing.stack(), existing.count() + stack.getCount()));
            }
        }
        return List.copyOf(result);
    }

    private static int findMaterial(List<MaterialRequirement> materials, ItemStack stack) {
        for (int index = 0; index < materials.size(); index++) {
            if (ItemStack.isSameItemSameComponents(materials.get(index).stack(), stack)) {
                return index;
            }
        }
        return -1;
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
