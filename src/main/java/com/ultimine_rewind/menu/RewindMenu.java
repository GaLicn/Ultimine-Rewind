package com.ultimine_rewind.menu;

import com.ultimine_rewind.data.BlockRecord;
import com.ultimine_rewind.data.UltimineRecord;
import com.ultimine_rewind.init.ModMenuTypes;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/** 放入恢复材料的 6×9 容器菜单。 */
public class RewindMenu extends AbstractContainerMenu {
    public static final int CONTAINER_SIZE = 54;
    private final Container container = new SimpleContainer(CONTAINER_SIZE);
    private final Player player;
    private final @Nullable UltimineRecord record;
    private Map<Item, Integer> clientRequiredItems = new LinkedHashMap<>();
    private int clientBlockCount;

    public RewindMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, null);
    }

    public RewindMenu(int containerId, Inventory inventory, @Nullable UltimineRecord record) {
        super(ModMenuTypes.REWIND_MENU.get(), containerId);
        this.player = inventory.player;
        this.record = record;

        for (int row = 0; row < 6; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(container, row * 9 + column, 8 + column * 18, 18 + row * 18));
            }
        }

        int inventoryY = 140;
        for (int row = 0; row < 3; row++) {
            for (int column = 0; column < 9; column++) {
                addSlot(new Slot(inventory, column + row * 9 + 9, 8 + column * 18, inventoryY + row * 18));
            }
        }
        for (int column = 0; column < 9; column++) {
            addSlot(new Slot(inventory, column, 8 + column * 18, inventoryY + 58));
        }
    }

    public @Nullable UltimineRecord record() {
        return record;
    }

    public void setClientData(Map<Item, Integer> requiredItems, int blockCount) {
        clientRequiredItems = new LinkedHashMap<>(requiredItems);
        clientBlockCount = blockCount;
    }

    public Map<Item, Integer> requiredItems() {
        return record == null ? clientRequiredItems : record.requiredItems();
    }

    public int blockCount() {
        return record == null ? clientBlockCount : record.blockCount();
    }

    public boolean hasData() {
        return blockCount() > 0;
    }

    public boolean validateItems() {
        if (record == null) {
            return false;
        }
        if (player.isCreative()) {
            return true;
        }
        Map<Item, Integer> required = record.requiredItems();
        for (int index = 0; index < CONTAINER_SIZE; index++) {
            ItemStack stack = container.getItem(index);
            if (!stack.isEmpty() && required.containsKey(stack.getItem())) {
                return true;
            }
        }
        return false;
    }

    /** 按记录顺序模拟消耗，返回本次能够恢复的具体方块。 */
    public java.util.List<BlockRecord> restorableBlocks() {
        if (record == null) {
            return java.util.List.of();
        }
        if (player.isCreative()) {
            return record.blocks();
        }

        Map<Item, Integer> available = new HashMap<>();
        for (int index = 0; index < CONTAINER_SIZE; index++) {
            ItemStack stack = container.getItem(index);
            available.merge(stack.getItem(), stack.getCount(), Integer::sum);
        }

        java.util.List<BlockRecord> result = new java.util.ArrayList<>();
        for (BlockRecord block : record.blocks()) {
            ItemStack required = block.requiredItem();
            if (required.isEmpty()) {
                continue;
            }
            int count = available.getOrDefault(required.getItem(), 0);
            if (count >= required.getCount()) {
                available.put(required.getItem(), count - required.getCount());
                result.add(block);
            }
        }
        return result;
    }

    public void consumeFor(java.util.List<BlockRecord> restored) {
        if (record == null || player.level().isClientSide()) {
            return;
        }
        Map<Item, Integer> consume = new HashMap<>();
        if (!player.isCreative()) {
            for (BlockRecord block : restored) {
                ItemStack required = block.requiredItem();
                consume.merge(required.getItem(), required.getCount(), Integer::sum);
            }
        }

        for (int index = 0; index < CONTAINER_SIZE; index++) {
            ItemStack stack = container.removeItemNoUpdate(index);
            int amount = Math.min(stack.getCount(), consume.getOrDefault(stack.getItem(), 0));
            if (amount > 0) {
                stack.shrink(amount);
                consume.merge(stack.getItem(), -amount, Integer::sum);
            }
            returnToPlayer(stack);
        }
    }

    private void returnToPlayer(ItemStack stack) {
        if (!stack.isEmpty() && !player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
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
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (!player.level().isClientSide()) {
            for (int index = 0; index < CONTAINER_SIZE; index++) {
                returnToPlayer(container.removeItemNoUpdate(index));
            }
        }
    }
}
