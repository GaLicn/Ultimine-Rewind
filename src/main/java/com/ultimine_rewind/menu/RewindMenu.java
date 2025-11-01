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
    
    // 容器大小 (6行x9列 = 54格，与原版大箱子一致)
    private static final int CONTAINER_SIZE = 54;
    private static final int CONTAINER_ROWS = 6;
    private static final int CONTAINER_COLS = 9;
    
    // 构造函数 - 服务端使用
    public RewindMenu(int containerId, Inventory playerInventory, UltimineRecord record) {
        super(ModMenuTypes.REWIND_MENU.get(), containerId);
        this.record = record;
        this.player = playerInventory.player;
        this.container = new SimpleContainer(CONTAINER_SIZE);
        
        // 添加容器槽位 (6行x9列，与原版大箱子一致)
        for (int row = 0; row < CONTAINER_ROWS; row++) {
            for (int col = 0; col < CONTAINER_COLS; col++) {
                this.addSlot(new Slot(container, row * CONTAINER_COLS + col, 
                    8 + col * 18, 18 + row * 18));
            }
        }
        
        // 添加玩家背包槽位 (3行) - Y坐标需要根据容器行数调整
        int inventoryY = 18 + CONTAINER_ROWS * 18 + 14; // 容器底部 + 间隔
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                this.addSlot(new Slot(playerInventory, 
                    col + row * 9 + 9, 8 + col * 18, inventoryY + row * 18));
            }
        }
        
        // 添加玩家快捷栏槽位
        int hotbarY = inventoryY + 58; // 背包下方 + 间隔
        for (int col = 0; col < 9; col++) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, hotbarY));
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
     * 验证物品是否足够（现在只要有物品就可以，支持部分恢复）
     */
    public boolean validateItems() {
        if (record == null) {
            return false;
        }
        
        // 创造模式玩家无需提供物品
        if (player != null && player.isCreative()) {
            return true;
        }
        
        // 修改：只要容器中有任何物品就允许恢复（部分恢复）
        // 或者至少有一种需要的物品
        Map<Item, Integer> required = record.getRequiredItems();
        
        for (int i = 0; i < CONTAINER_SIZE; i++) {
            ItemStack stack = this.getSlot(i).getItem();
            if (!stack.isEmpty() && required.containsKey(stack.getItem())) {
                // 只要有至少一个需要的物品，就允许恢复
                return true;
            }
        }
        
        // 如果没有任何需要的物品，禁用按钮
        return false;
    }
    
    /**
     * 计算可以恢复多少个方块
     * @return 可恢复的方块数量
     */
    public int getRestorableBlockCount() {
        if (record == null) {
            return 0;
        }
        
        // 创造模式可以恢复所有方块
        if (player != null && player.isCreative()) {
            return record.getBlockCount();
        }
        
        // 统计容器中的物品
        Map<Item, Integer> provided = new HashMap<>();
        for (int i = 0; i < CONTAINER_SIZE; i++) {
            ItemStack stack = this.getSlot(i).getItem();
            if (!stack.isEmpty()) {
                provided.merge(stack.getItem(), stack.getCount(), Integer::sum);
            }
        }
        
        // 计算每种物品最多能恢复多少个方块
        Map<Item, Integer> required = record.getRequiredItems();
        int totalRequired = required.values().stream().mapToInt(Integer::intValue).sum();
        
        if (totalRequired == 0) {
            return 0;
        }
        
        // 计算最小比例
        int canRestore = Integer.MAX_VALUE;
        for (Map.Entry<Item, Integer> entry : required.entrySet()) {
            int need = entry.getValue();
            int have = provided.getOrDefault(entry.getKey(), 0);
            
            if (need > 0) {
                // 这种物品能恢复的方块数比例
                int ratio = (have * record.getBlockCount()) / need;
                canRestore = Math.min(canRestore, ratio);
            }
        }
        
        return canRestore == Integer.MAX_VALUE ? 0 : canRestore;
    }
    
    /**
     * 消耗容器中的物品并返还剩余部分（支持部分消耗）
     */
    public void consumeItemsAndReturnRest() {
        if (record == null || player == null || player.level().isClientSide) {
            return;
        }
        
        // 创造模式：返还所有物品，不消耗
        if (player.isCreative()) {
            for (int i = 0; i < CONTAINER_SIZE; i++) {
                ItemStack stack = container.removeItemNoUpdate(i);
                if (!stack.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(stack);
                }
            }
            return;
        }
        
        // 生存模式：根据可恢复的方块数量计算需要消耗的物品
        int restorableCount = getRestorableBlockCount();
        int totalBlocks = record.getBlockCount();
        
        if (restorableCount <= 0) {
            // 材料完全不足，返还所有物品
            for (int i = 0; i < CONTAINER_SIZE; i++) {
                ItemStack stack = container.removeItemNoUpdate(i);
                if (!stack.isEmpty()) {
                    player.getInventory().placeItemBackInInventory(stack);
                }
            }
            return;
        }
        
        // 计算每种物品需要消耗的数量（按比例）
        Map<Item, Integer> originalRequired = record.getRequiredItems();
        Map<Item, Integer> actualConsume = new HashMap<>();
        
        for (Map.Entry<Item, Integer> entry : originalRequired.entrySet()) {
            int originalAmount = entry.getValue();
            // 按比例计算实际消耗量
            int consumeAmount = (originalAmount * restorableCount) / totalBlocks;
            actualConsume.put(entry.getKey(), consumeAmount);
        }
        
        // 消耗物品并返还剩余
        for (int i = 0; i < CONTAINER_SIZE; i++) {
            ItemStack stack = container.removeItemNoUpdate(i);
            
            if (!stack.isEmpty()) {
                Item item = stack.getItem();
                int count = stack.getCount();
                
                // 检查这个物品是否是需要的
                if (actualConsume.containsKey(item) && actualConsume.get(item) > 0) {
                    int needed = actualConsume.get(item);
                    int toConsume = Math.min(needed, count);
                    int toReturn = count - toConsume;
                    
                    // 更新需要的数量
                    actualConsume.put(item, needed - toConsume);
                    
                    // 返还多余的部分
                    if (toReturn > 0) {
                        ItemStack returnStack = new ItemStack(item, toReturn);
                        player.getInventory().placeItemBackInInventory(returnStack);
                    }
                } else {
                    // 不是需要的物品或已经消耗够了，全部返还
                    player.getInventory().placeItemBackInInventory(stack);
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

