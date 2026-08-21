package com.ultimine_rewind.menu;

import com.ultimine_rewind.data.BlockRecord;
import com.ultimine_rewind.data.MaterialRequirement;
import com.ultimine_rewind.data.UltimineRecord;
import com.ultimine_rewind.init.ModMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** 撤回材料菜单，材料不足时允许玩家手动补充并部分恢复。 */
public class RewindMenu extends AbstractContainerMenu {
    public static final int CONTAINER_SIZE = 54;
    private final @Nullable UltimineRecord record;
    private final Container container;
    private final Player player;
    private List<MaterialRequirement> clientRequiredMaterials = List.of();
    private int clientBlockCount;

    public RewindMenu(int containerId, Inventory playerInventory, @Nullable UltimineRecord record) {
        super(ModMenuTypes.REWIND_MENU.get(), containerId);
        this.record = record;
        this.player = playerInventory.player;
        this.container = new SimpleContainer(CONTAINER_SIZE);

        for (int row = 0; row < 6; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(container, row * 9 + column, 8 + column * 18, 18 + row * 18));
            }
        }

        int inventoryY = 140;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(playerInventory, column + row * 9 + 9,
                        8 + column * 18, inventoryY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(playerInventory, column, 8 + column * 18, inventoryY + 58));
        }
    }

    public RewindMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
    }

    public void setClientRecordData(List<MaterialRequirement> requiredMaterials, int blockCount) {
        clientRequiredMaterials = List.copyOf(requiredMaterials);
        clientBlockCount = blockCount;
    }

    public @Nullable UltimineRecord record() {
        return record;
    }

    public List<MaterialRequirement> requiredMaterials() {
        if (record != null) {
            return record.requiredMaterials();
        }
        return clientRequiredMaterials;
    }

    public int getBlockCount() {
        return record == null ? clientBlockCount : record.getBlockCount();
    }

    public boolean hasData() {
        return getBlockCount() > 0;
    }

    public boolean validateItems() {
        if (record == null) {
            return false;
        }
        if (player.isCreative()) {
            return true;
        }
        for (int index = 0; index < CONTAINER_SIZE; index++) {
            ItemStack stack = container.getItem(index);
            for (MaterialRequirement material : record.requiredMaterials()) {
                if (ItemStack.isSameItemSameComponents(stack, material.stack())) {
                    return true;
                }
            }
        }
        return false;
    }

    /** 按记录顺序模拟材料消耗，返回本次可恢复的方块。 */
    public List<BlockRecord> restorableBlocks() {
        if (record == null) {
            return List.of();
        }
        if (player.isCreative()) {
            return record.blocks();
        }

        List<MaterialRequirement> available = new ArrayList<>();
        for (int index = 0; index < CONTAINER_SIZE; index++) {
            ItemStack stack = container.getItem(index);
            addMaterial(available, stack, stack.getCount());
        }

        List<BlockRecord> result = new ArrayList<>();
        for (BlockRecord block : record.blocks()) {
            ItemStack required = block.requiredItem();
            int materialIndex = findMaterial(available, required);
            if (!required.isEmpty() && materialIndex >= 0
                    && available.get(materialIndex).count() >= required.getCount()) {
                MaterialRequirement material = available.get(materialIndex);
                available.set(materialIndex, new MaterialRequirement(
                        material.stack(), material.count() - required.getCount()));
                result.add(block);
            }
        }
        return result;
    }

    public void consumeFor(List<BlockRecord> restored) {
        if (record == null || player.level().isClientSide) {
            return;
        }
        List<MaterialRequirement> consume = new ArrayList<>();
        if (!player.isCreative()) {
            for (BlockRecord block : restored) {
                ItemStack required = block.requiredItem();
                addMaterial(consume, required, required.getCount());
            }
        }

        for (int index = 0; index < CONTAINER_SIZE; index++) {
            ItemStack stack = container.removeItemNoUpdate(index);
            int materialIndex = findMaterial(consume, stack);
            int remaining = materialIndex < 0 ? 0 : consume.get(materialIndex).count();
            int amount = Math.min(stack.getCount(), remaining);
            if (amount > 0) {
                stack.shrink(amount);
                MaterialRequirement material = consume.get(materialIndex);
                consume.set(materialIndex, new MaterialRequirement(material.stack(), remaining - amount));
            }
            returnToPlayer(stack);
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
            materials.set(materialIndex, new MaterialRequirement(material.stack(), material.count() + count));
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

    private void returnToPlayer(ItemStack stack) {
        if (!stack.isEmpty() && !player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    @Override
    public @NotNull ItemStack quickMoveStack(@NotNull Player player, int index) {
        Slot slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        boolean moved = index < CONTAINER_SIZE
                ? moveItemStackTo(stack, CONTAINER_SIZE, slots.size(), true)
                : moveItemStackTo(stack, 0, CONTAINER_SIZE, false);
        if (!moved) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return copy;
    }

    @Override
    public void removed(@NotNull Player player) {
        super.removed(player);
        if (!player.level().isClientSide) {
            for (int index = 0; index < CONTAINER_SIZE; index++) {
                returnToPlayer(container.removeItemNoUpdate(index));
            }
        }
    }

    @Override
    public boolean stillValid(@NotNull Player player) {
        return true;
    }
}
