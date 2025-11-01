package com.ultimine_rewind.menu;

import com.ultimine_rewind.data.UltimineRecord;
import com.ultimine_rewind.init.ModMenuTypes;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.HashMap;
import java.util.Map;

/**
 * 撤销菜单 - 服务端容器逻辑
 */
public class RewindMenu extends AbstractContainerMenu {
    public final UltimineRecord record;
    private final Container container;
    private final Player player;
    
    // 客户端缓存的数据
    private Map<Item, Integer> clientRequiredItems;
    private int clientBlockCount;
    
    // 容器大小 (6行x6列 = 36格)
    private static final int CONTAINER_SIZE = 36;
    private static final int CONTAINER_ROWS = 6;
    private static final int CONTAINER_COLS = 6;
    
    // 构造函数 - 服务端使用
    public RewindMenu(int containerId, Inventory playerInventory, UltimineRecord record) {
        super(ModMenuTypes.REWIND_MENU.get(), containerId);
        this.record = record;
        this.player = playerInventory.player;
        this.container = new SimpleContainer(CONTAINER_SIZE);
        
        // 添加容器槽位 (6行x6列)
        for (int row = 0; row < CONTAINER_ROWS; row++) {
            for (int col = 0; col < CONTAINER_COLS; col++) {
                this.addSlot(new Slot(container, row * CONTAINER_COLS + col, 
                    8 + col * 18, 18 + row * 18));
            }
        }
        
        // 添加玩家背包槽位 (3行)
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, 
                    col + row * 9 + 9, 8 + col * 18, 140 + row * 18));
            }
        }
        
        // 添加玩家快捷栏槽位
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 198));
        }
    }
    
    // 构造函数 - 客户端使用
    public RewindMenu(int containerId, Inventory playerInventory) {
        this(containerId, playerInventory, null);
        this.clientRequiredItems = new HashMap<>();
        this.clientBlockCount = 0;
    }
    
    /**
     * 设置客户端数据（由网络包调用）
     */
    public void setClientRecordData(Map<Item, Integer> requiredItems, int blockCount) {
        this.clientRequiredItems = new HashMap<>(requiredItems);
        this.clientBlockCount = blockCount;
        
        // 调试日志
        System.out.println("[Ultimine Rewind] 菜单设置客户端数据: " + 
            this.clientRequiredItems.size() + " 种物品");
        for (Map.Entry<Item, Integer> entry : this.clientRequiredItems.entrySet()) {
            System.out.println("  - " + entry.getKey().getDescription().getString() + " x" + entry.getValue());
        }
    }
    
    /**
     * 获取需要的物品（兼容客户端和服务端）
     */
    public Map<Item, Integer> getRequiredItems() {
        if (record != null) {
            return record.getRequiredItems();
        }
        return clientRequiredItems != null ? clientRequiredItems : new HashMap<>();
    }
    
    /**
     * 获取方块数量（兼容客户端和服务端）
     */
    public int getBlockCount() {
        if (record != null) {
            return record.getBlockCount();
        }
        return clientBlockCount;
    }
    
    /**
     * 检查是否有数据
     */
    public boolean hasData() {
        if (record != null) {
            Map<Item, Integer> items = record.getRequiredItems();
            return items != null && !items.isEmpty();
        }
        return clientRequiredItems != null && !clientRequiredItems.isEmpty();
    }
    
    /**
     * 验证物品是否足够
     */
    public boolean validateItems() {
        if (record == null) {
            return false;
        }
        
        // 创造模式玩家无需提供物品
        if (player != null && player.isCreative()) {
            return true;
        }
        
        Map<Item, Integer> required = record.getRequiredItems();
        Map<Item, Integer> provided = new HashMap<>();
        
        // 统计容器中的物品
        for (int i = 0; i < CONTAINER_SIZE; i++) {
            ItemStack stack = this.getSlot(i).getItem();
            if (!stack.isEmpty()) {
                provided.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        
        // 检查每种物品是否足够
        for (Map.Entry<Item, Integer> entry : required.entrySet()) {
            int providedCount = provided.getOrDefault(entry.getKey(), 0);
            if (providedCount < entry.getValue()) {
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * 消耗容器中的物品
     */
    public void consumeItems() {
        if (record == null) {
            return;
        }
        
        // 创造模式玩家无需消耗物品
        if (player != null && player.isCreative()) {
            return;
        }
        
        Map<Item, Integer> required = new HashMap<>(record.getRequiredItems());
        
        for (int i = 0; i < CONTAINER_SIZE; i++) {
            ItemStack stack = this.getSlot(i).getItem();
            if (!stack.isEmpty() && required.containsKey(stack.getItem())) {
                int needed = required.get(stack.getItem());
                int toRemove = Math.min(needed, stack.getCount());
                
                stack.shrink(toRemove);
                required.put(stack.getItem(), needed - toRemove);
                
                if (required.get(stack.getItem()) <= 0) {
                    required.remove(stack.getItem());
                }
                
                if (required.isEmpty()) {
                    break;
                }
            }
        }
    }
    
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        ItemStack itemstack = ItemStack.EMPTY;
        Slot slot = this.slots.get(index);
        
        if (slot != null && slot.hasItem()) {
            ItemStack stack = slot.getItem();
            itemstack = stack.copy();
            
            if (index < CONTAINER_SIZE) {
                // 从容器移动到玩家背包
                if (!this.moveItemStackTo(stack, CONTAINER_SIZE, this.slots.size(), true)) {
                    return ItemStack.EMPTY;
                }
            } else {
                // 从玩家背包移动到容器
                if (!this.moveItemStackTo(stack, 0, CONTAINER_SIZE, false)) {
                    return ItemStack.EMPTY;
                }
            }
            
            if (stack.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                slot.setChanged();
            }
        }
        
        return itemstack;
    }
    
    @Override
    public boolean stillValid(Player player) {
        return true;
    }
    
    @Override
    public void removed(Player player) {
        super.removed(player);
        
        // 关闭时返还容器中的物品
        if (!player.level().isClientSide) {
            for (int i = 0; i < CONTAINER_SIZE; i++) {
                ItemStack stack = container.removeItemNoUpdate(i);
                if (!stack.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(stack);
                }
            }
        }
    }
}

