package com.ultimine_rewind.logic;

import com.ultimine_rewind.data.BlockRecord;
import com.ultimine_rewind.data.RewindDataManager;
import com.ultimine_rewind.data.UltimineRecord;
import com.ultimine_rewind.menu.RewindMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * 方块恢复执行器
 */
public class RewindExecutor {
    
    /**
     * 执行撤销恢复
     * @param player 玩家
     * @param menu 撤销菜单
     * @return 是否成功恢复
     */
    public static boolean executeRewind(ServerPlayer player, RewindMenu menu) {
        UltimineRecord record = menu.record;
        if (record == null) {
            return false;
        }
        
        ServerLevel level = player.serverLevel();
        boolean isCreative = player.isCreative();
        
        // 1. 验证物品（创造模式跳过）
        if (!menu.validateItems()) {
            player.displayClientMessage(
                Component.translatable("message.ultimine_rewind.no_materials").withStyle(ChatFormatting.RED),
                false
            );
            return false;
        }
        
        // 计算可以恢复多少个方块
        int restorableCount = menu.getRestorableBlockCount();
        if (restorableCount <= 0 && !isCreative) {
            player.displayClientMessage(
                Component.translatable("message.ultimine_rewind.insufficient_materials").withStyle(ChatFormatting.RED),
                false
            );
            return false;
        }
        
        // 2. 确定要恢复的方块列表（部分或全部）
        int blocksToRestore = isCreative ? record.getBlockCount() : Math.min(restorableCount, record.getBlockCount());
        java.util.List<BlockRecord> blocksToProcess = record.getBlocks().subList(0, blocksToRestore);
        
        // 3. 检查方块位置是否可以放置
        for (BlockRecord blockRecord : blocksToProcess) {
            BlockPos pos = blockRecord.getPos();
            
            // 检查区块是否加载
            if (!level.isLoaded(pos)) {
                // 静默跳过，不显示消息（避免刷屏）
                continue;
            }
            
            // 检查位置是否可以放置方块
            BlockState currentState = level.getBlockState(pos);
            // 只允许在空气或可替换的方块位置恢复
            if (!currentState.isAir() && !currentState.canBeReplaced()) {
                // 跳过已被占用的位置，继续恢复其他方块
                continue;
            }
        }
        
        // 4. 恢复方块
        int restoredCount = 0;
        for (BlockRecord blockRecord : blocksToProcess) {
            BlockPos pos = blockRecord.getPos();
            BlockState state = blockRecord.getState();
            
            // 检查区块是否加载
            if (!level.isLoaded(pos)) {
                continue;
            }
            
            // 检查位置是否可用
            BlockState currentState = level.getBlockState(pos);
            if (!currentState.isAir() && !currentState.canBeReplaced()) {
                continue;
            }
            
            // 放置方块
            level.setBlock(pos, state, Block.UPDATE_ALL | Block.UPDATE_CLIENTS);
            
            // 恢复方块实体数据
            if (blockRecord.getBlockEntityData() != null) {
                BlockEntity blockEntity = level.getBlockEntity(pos);
                if (blockEntity != null) {
                    blockEntity.load(blockRecord.getBlockEntityData());
                    blockEntity.setChanged();
                }
            }
            
            restoredCount++;
        }
        
        // 5. 消耗物品并返还剩余物品（在恢复完成后）
        menu.consumeItemsAndReturnRest();
        
        // 6. 更新或清除记录
        int totalBlocks = record.getBlockCount();
        if (restoredCount < totalBlocks) {
            // 部分恢复，保留剩余方块的记录
            UltimineRecord newRecord = record.removeRestoredBlocks(restoredCount);
            RewindDataManager.updateRecord(player.getUUID(), newRecord);
            
            int remaining = totalBlocks - restoredCount;
            player.displayClientMessage(
                Component.translatable("message.ultimine_rewind.partial_restore", restoredCount, totalBlocks)
                    .withStyle(ChatFormatting.YELLOW),
                false
            );
            player.displayClientMessage(
                Component.translatable("message.ultimine_rewind.remaining", remaining)
                    .withStyle(ChatFormatting.GRAY),
                true
            );
        } else {
            // 完全恢复，清除记录
            RewindDataManager.clearRecord(player.getUUID());
            
            if (isCreative) {
                player.displayClientMessage(
                    Component.translatable("message.ultimine_rewind.success_creative", restoredCount)
                        .withStyle(ChatFormatting.GREEN),
                    false
                );
            } else {
                player.displayClientMessage(
                    Component.translatable("message.ultimine_rewind.success", restoredCount)
                        .withStyle(ChatFormatting.GREEN),
                    false
                );
            }
        }
        
        return true;
    }
}

