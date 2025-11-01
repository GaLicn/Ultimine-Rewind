package com.ultimine_rewind.data;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 撤销数据管理器
 * 管理所有玩家的连锁采集记录
 */
public class RewindDataManager {
    // 每个玩家只保存最后一次连锁采集记录
    private static final Map<UUID, UltimineRecord> playerRecords = new HashMap<>();
    
    /**
     * 记录一次连锁采集
     */
    public static void recordUltimine(ServerPlayer player, List<BlockRecord> blocks, BlockPos centerPos) {
        if (blocks == null || blocks.isEmpty()) {
            return;
        }
        
        UUID playerId = player.getUUID();
        UltimineRecord record = new UltimineRecord(playerId, System.currentTimeMillis(), blocks, centerPos);
        playerRecords.put(playerId, record);
    }
    
    /**
     * 获取玩家的撤销记录
     * @return 记录，如果不存在或已过期则返回null
     */
    @Nullable
    public static UltimineRecord getRecord(UUID playerId) {
        UltimineRecord record = playerRecords.get(playerId);
        if (record != null && record.isExpired()) {
            playerRecords.remove(playerId);
            return null;
        }
        return record;
    }
    
    /**
     * 清除玩家的撤销记录
     */
    public static void clearRecord(UUID playerId) {
        playerRecords.remove(playerId);
    }
    
    /**
     * 更新玩家的撤销记录（部分恢复后）
     * @param playerId 玩家ID
     * @param newRecord 新的记录（如果为null则清除记录）
     */
    public static void updateRecord(UUID playerId, UltimineRecord newRecord) {
        if (newRecord == null || newRecord.getBlocks().isEmpty()) {
            playerRecords.remove(playerId);
        } else {
            playerRecords.put(playerId, newRecord);
        }
    }
    
    /**
     * 清除所有过期的记录
     */
    public static void cleanupExpiredRecords() {
        playerRecords.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }
    
    /**
     * 检查玩家是否有可用的撤销记录
     */
    public static boolean hasRecord(UUID playerId) {
        return getRecord(playerId) != null;
    }
}

