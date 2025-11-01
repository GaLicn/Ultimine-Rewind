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
                Component.literal("物品不足，无法恢复").withStyle(ChatFormatting.RED),
                false
            );
            return false;
        }
        
        // 2. 检查所有方块位置是否可以放置
        for (BlockRecord blockRecord : record.getBlocks()) {
            BlockPos pos = blockRecord.getPos();
            
            // 检查区块是否加载
            if (!level.isLoaded(pos)) {
                player.displayClientMessage(
                    Component.literal("某些区块未加载，无法恢复").withStyle(ChatFormatting.RED),
                    false
                );
                return false;
            }
            
            // 检查位置是否可以放置方块
            BlockState currentState = level.getBlockState(pos);
            // 只允许在空气或可替换的方块位置恢复
            if (!currentState.isAir() && !currentState.canBeReplaced()) {
                player.displayClientMessage(
                    Component.literal("某些位置已被占用，无法恢复").withStyle(ChatFormatting.RED),
                    false
                );
                return false;
            }
        }
        
        // 3. 消耗物品（创造模式不消耗）
        menu.consumeItems();
        
        // 4. 恢复所有方块
        int restoredCount = 0;
        for (BlockRecord blockRecord : record.getBlocks()) {
            BlockPos pos = blockRecord.getPos();
            BlockState state = blockRecord.getState();
            
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
        
        // 5. 清除记录
        RewindDataManager.clearRecord(player.getUUID());
        
        // 6. 发送成功消息
        if (isCreative) {
            player.displayClientMessage(
                Component.literal("成功恢复 " + restoredCount + " 个方块（创造模式）")
                    .withStyle(ChatFormatting.GREEN),
                false
            );
        } else {
            player.displayClientMessage(
                Component.literal("成功恢复 " + restoredCount + " 个方块")
                    .withStyle(ChatFormatting.GREEN),
                false
            );
        }
        
        return true;
    }
}

