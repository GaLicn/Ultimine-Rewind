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
import java.util.function.Consumer;

/** 方块恢复执行器。 */
public final class RewindExecutor {
    private RewindExecutor() {
    }

    public static boolean executeRewind(ServerPlayer player, RewindMenu menu) {
        UltimineRecord record = menu.record();
        if (record == null || !menu.validateItems()) {
            player.displayClientMessage(Component.translatable(
                    "message.ultimine_rewind.no_materials").withStyle(ChatFormatting.RED), false);
            return false;
        }
        return restore(player, record, menu.restorableBlocks(), menu::consumeFor);
    }

    public static boolean hasEnoughMaterials(ServerPlayer player, UltimineRecord record) {
        if (player.isCreative()) {
            return true;
        }
        List<MaterialRequirement> available = inventoryMaterials(player);
        for (MaterialRequirement required : record.requiredMaterials()) {
            int index = findMaterial(available, required.stack());
            if (index < 0 || available.get(index).count() < required.count()) {
                return false;
            }
        }
        return true;
    }

    public static boolean executeRewind(ServerPlayer player, UltimineRecord record) {
        if (!hasEnoughMaterials(player, record)) {
            player.displayClientMessage(Component.translatable(
                    "message.ultimine_rewind.no_materials").withStyle(ChatFormatting.RED), false);
            return false;
        }
        return restore(player, record, record.getBlocks(), restored -> consumeInventory(player, restored));
    }

    private static boolean restore(ServerPlayer player, UltimineRecord record, List<BlockRecord> candidates,
                                   Consumer<List<BlockRecord>> consumer) {
        ServerLevel level = player.serverLevel();
        List<BlockRecord> restored = new ArrayList<>();
        for (BlockRecord block : candidates) {
            BlockState current = level.getBlockState(block.getPos());
            if (!level.isLoaded(block.getPos()) || (!current.isAir() && !current.canBeReplaced())) {
                continue;
            }
            if (level.setBlock(block.getPos(), block.getState(), Block.UPDATE_ALL)) {
                // 方块实体数据只在创造模式恢复，避免复制容器内容。
                if (player.isCreative() && block.getBlockEntityData() != null) {
                    BlockEntity blockEntity = level.getBlockEntity(block.getPos());
                    if (blockEntity != null) {
                        blockEntity.load(block.getBlockEntityData());
                        blockEntity.setChanged();
                    }
                }
                restored.add(block);
            }
        }

        consumer.accept(restored);
        UltimineRecord remaining = record.withoutRestored(restored);
        RewindDataManager.updateRecord(player.getUUID(), remaining);
        if (remaining == null) {
            String key = player.isCreative()
                    ? "message.ultimine_rewind.success_creative"
                    : "message.ultimine_rewind.success";
            player.displayClientMessage(Component.translatable(key, restored.size())
                    .withStyle(ChatFormatting.GREEN), false);
        } else {
            player.displayClientMessage(Component.translatable(
                    "message.ultimine_rewind.partial_restore", restored.size(), record.getBlockCount())
                    .withStyle(ChatFormatting.YELLOW), false);
            player.displayClientMessage(Component.translatable(
                    "message.ultimine_rewind.remaining", remaining.getBlockCount())
                    .withStyle(ChatFormatting.GRAY), true);
        }
        return !restored.isEmpty();
    }

    private static List<MaterialRequirement> inventoryMaterials(ServerPlayer player) {
        Inventory inventory = player.getInventory();
        List<MaterialRequirement> available = new ArrayList<>();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            addMaterial(available, stack, stack.getCount());
        }
        return available;
    }

    private static void consumeInventory(ServerPlayer player, List<BlockRecord> restored) {
        if (player.isCreative()) {
            return;
        }
        List<MaterialRequirement> required = new ArrayList<>();
        for (BlockRecord block : restored) {
            addMaterial(required, block.getRequiredItem(), 1);
        }
        Inventory inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            int index = findMaterial(required, stack);
            if (index < 0) {
                continue;
            }
            MaterialRequirement material = required.get(index);
            int amount = Math.min(stack.getCount(), material.count());
            stack.shrink(amount);
            required.set(index, new MaterialRequirement(material.stack(), material.count() - amount));
        }
    }

    private static void addMaterial(List<MaterialRequirement> materials, ItemStack stack, int count) {
        if (stack.isEmpty() || count <= 0) {
            return;
        }
        int index = findMaterial(materials, stack);
        if (index < 0) {
            materials.add(new MaterialRequirement(stack, count));
        } else {
            MaterialRequirement material = materials.get(index);
            materials.set(index, new MaterialRequirement(material.stack(), material.count() + count));
        }
    }

    private static int findMaterial(List<MaterialRequirement> materials, ItemStack stack) {
        for (int index = 0; index < materials.size(); index++) {
            if (ItemStack.isSameItemSameTags(materials.get(index).stack(), stack)) {
                return index;
            }
        }
        return -1;
    }
}
