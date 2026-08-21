package com.ultimine_rewind.logic;

import com.ultimine_rewind.data.BlockRecord;
import com.ultimine_rewind.data.MaterialRequirement;
import com.ultimine_rewind.data.RewindDataManager;
import com.ultimine_rewind.data.UltimineRecord;
import com.ultimine_rewind.menu.RewindMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class RewindExecutor {
    private RewindExecutor() {
    }

    public static boolean execute(ServerPlayer player, RewindMenu menu) {
        UltimineRecord record = menu.record();
        if (record == null || !menu.validateItems()) {
            player.sendSystemMessage(Component.translatable(
                    "message.ultimine_rewind.no_materials").withStyle(ChatFormatting.RED), false);
            return false;
        }

        List<BlockRecord> candidates = menu.restorableBlocks();
        return restore(player, record, candidates, menu::consumeFor);
    }

    public static boolean hasEnoughMaterials(ServerPlayer player, UltimineRecord record) {
        if (player.isCreative()) {
            return true;
        }

        List<MaterialRequirement> available = inventoryMaterials(player);
        for (MaterialRequirement required : record.requiredMaterials()) {
            int materialIndex = findMaterial(available, required.stack());
            if (materialIndex < 0 || available.get(materialIndex).count() < required.count()) {
                return false;
            }
        }
        return true;
    }

    public static boolean execute(ServerPlayer player, UltimineRecord record) {
        if (!hasEnoughMaterials(player, record)) {
            player.sendSystemMessage(Component.translatable(
                    "message.ultimine_rewind.no_materials").withStyle(ChatFormatting.RED), false);
            return false;
        }

        List<BlockRecord> candidates = restorableBlocks(record, inventoryMaterials(player), player.isCreative());
        return restore(player, record, candidates, restored -> consumeInventory(player, restored));
    }

    private static boolean restore(ServerPlayer player, UltimineRecord record,
                                   List<BlockRecord> candidates,
                                   java.util.function.Consumer<List<BlockRecord>> consumer) {
        ServerLevel level = player.level();
        List<BlockRecord> restored = new ArrayList<>();
        for (BlockRecord block : candidates) {
            BlockState current = level.getBlockState(block.pos());
            if (!level.isLoaded(block.pos()) || (!current.isAir() && !current.canBeReplaced())) {
                continue;
            }

            if (level.setBlock(block.pos(), block.state(), Block.UPDATE_ALL)) {
                // 方块实体数据仅在创造模式恢复，避免复制容器内容。
                if (player.isCreative() && block.blockEntityData() != null) {
                    BlockEntity blockEntity = level.getBlockEntity(block.pos());
                    BlockEntity restoredEntity = BlockEntity.loadStatic(
                            block.pos(), block.state(), block.blockEntityData(), level.registryAccess());
                    if (blockEntity != null && restoredEntity != null) {
                        level.setBlockEntity(restoredEntity);
                    }
                }
                restored.add(block);
            }
        }

        consumer.accept(restored);
        UltimineRecord remaining = record.withoutRestored(restored);
        RewindDataManager.update(player.getUUID(), remaining);

        if (remaining == null) {
            String key = player.isCreative()
                    ? "message.ultimine_rewind.success_creative"
                    : "message.ultimine_rewind.success";
            player.sendSystemMessage(Component.translatable(key, restored.size()).withStyle(ChatFormatting.GREEN));
        } else {
            player.sendSystemMessage(Component.translatable(
                    "message.ultimine_rewind.partial_restore", restored.size(), record.blockCount())
                    .withStyle(ChatFormatting.YELLOW));
            player.sendOverlayMessage(Component.translatable(
                    "message.ultimine_rewind.remaining", remaining.blockCount()).withStyle(ChatFormatting.GRAY));
        }
        return !restored.isEmpty();
    }

    private static List<BlockRecord> restorableBlocks(UltimineRecord record,
                                                      List<MaterialRequirement> available,
                                                      boolean creative) {
        if (creative) {
            return record.blocks();
        }

        List<BlockRecord> result = new ArrayList<>();
        for (BlockRecord block : record.blocks()) {
            ItemStack required = block.requiredItem();
            if (required.isEmpty()) {
                continue;
            }
            int materialIndex = findMaterial(available, required);
            if (materialIndex >= 0 && available.get(materialIndex).count() >= required.getCount()) {
                MaterialRequirement material = available.get(materialIndex);
                available.set(materialIndex, new MaterialRequirement(
                        material.stack(), material.count() - required.getCount()));
                result.add(block);
            }
        }
        return result;
    }

    private static List<MaterialRequirement> inventoryMaterials(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        List<MaterialRequirement> available = new ArrayList<>();
        addMaterials(available, inventory);
        return available;
    }

    private static void addMaterials(List<MaterialRequirement> materials, Inventory inventory) {
        // 使用公开容器接口，覆盖主背包、盔甲栏、副手及其他装备槽。
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.isEmpty()) {
                continue;
            }
            int materialIndex = findMaterial(materials, stack);
            if (materialIndex < 0) {
                materials.add(new MaterialRequirement(stack, stack.getCount()));
            } else {
                MaterialRequirement material = materials.get(materialIndex);
                materials.set(materialIndex, new MaterialRequirement(
                        material.stack(), material.count() + stack.getCount()));
            }
        }
    }

    private static void consumeInventory(ServerPlayer player, List<BlockRecord> restored) {
        if (player.isCreative()) {
            return;
        }

        List<MaterialRequirement> required = new ArrayList<>();
        for (BlockRecord block : restored) {
            addMaterial(required, block.requiredItem(), block.requiredItem().getCount());
        }

        Inventory inventory = player.getInventory();
        consumeMaterials(required, inventory);
    }

    private static void consumeMaterials(List<MaterialRequirement> required, Inventory inventory) {
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            int materialIndex = findMaterial(required, stack);
            if (materialIndex < 0) {
                continue;
            }
            MaterialRequirement material = required.get(materialIndex);
            int amount = Math.min(stack.getCount(), material.count());
            stack.shrink(amount);
            required.set(materialIndex, new MaterialRequirement(material.stack(), material.count() - amount));
        }
    }

    private static void addMaterial(List<MaterialRequirement> materials, ItemStack stack, int count) {
        if (stack.isEmpty() || count <= 0) {
            return;
        }
        int materialIndex = findMaterial(materials, stack);
        if (materialIndex < 0) {
            materials.add(new MaterialRequirement(stack, count));
        } else {
            MaterialRequirement material = materials.get(materialIndex);
            materials.set(materialIndex, new MaterialRequirement(
                    material.stack(), material.count() + count));
        }
    }

    private static int findMaterial(List<MaterialRequirement> materials, ItemStack stack) {
        for (int index = 0; index < materials.size(); index++) {
            if (ItemStack.isSameItemSameComponents(materials.get(index).stack(), stack)) {
                return index;
            }
        }
        return -1;
    }
}
