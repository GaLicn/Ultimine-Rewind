package com.ultimine_rewind.menu;

import com.ultimine_rewind.data.BlockRecord;
import com.ultimine_rewind.data.MaterialRequirement;
import com.ultimine_rewind.data.UltimineRecord;
import com.ultimine_rewind.init.ModMenuTypes;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/** 放入恢复材料的 6×9 容器菜单。 */
public class RewindMenu extends AbstractContainerMenu {
    public static final int CONTAINER_SIZE = 54;
    private final Container container = new SimpleContainer(CONTAINER_SIZE);
    private final Player player;
    private final @Nullable UltimineRecord record;
    private List<MaterialRequirement> clientRequiredMaterials = List.of();
    private int clientBlockCount;

    public RewindMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, (UltimineRecord) null);
    }

    public RewindMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, (UltimineRecord) null);
        clientBlockCount = buffer.readVarInt();
        int materialCount = buffer.readVarInt();
        List<MaterialRequirement> materials = new ArrayList<>(materialCount);
        for (int index = 0; index < materialCount; index++) {
            materials.add(new MaterialRequirement(ItemStack.STREAM_CODEC.decode(buffer), buffer.readVarInt()));
        }
        clientRequiredMaterials = List.copyOf(materials);
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

    public static void writeClientData(RegistryFriendlyByteBuf buffer, UltimineRecord record) {
        List<MaterialRequirement> materials = record.requiredMaterials();
        buffer.writeVarInt(record.blockCount());
        buffer.writeVarInt(materials.size());
        for (MaterialRequirement material : materials) {
            ItemStack.STREAM_CODEC.encode(buffer, material.stack());
            buffer.writeVarInt(material.count());
        }
    }

    public List<MaterialRequirement> requiredMaterials() {
        return record == null ? clientRequiredMaterials : record.requiredMaterials();
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

    /** 按记录顺序模拟消耗，返回本次能够恢复的具体方块。 */
    public java.util.List<BlockRecord> restorableBlocks() {
        if (record == null) {
            return java.util.List.of();
        }
        if (player.isCreative()) {
            return record.blocks();
        }

        List<MaterialRequirement> available = new ArrayList<>();
        for (int index = 0; index < CONTAINER_SIZE; index++) {
            ItemStack stack = container.getItem(index);
            addMaterial(available, stack, stack.getCount());
        }

        java.util.List<BlockRecord> result = new java.util.ArrayList<>();
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

    public void consumeFor(java.util.List<BlockRecord> restored) {
        if (record == null || player.level().isClientSide()) {
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
