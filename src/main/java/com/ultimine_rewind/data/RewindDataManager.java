package com.ultimine_rewind.data;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 撤销数据管理器
 * 管理所有玩家的连锁采集记录
 */
public class RewindDataManager {
    private static final int MAX_RECORDS = 5;
    private static final Map<UUID, Deque<UltimineRecord>> playerRecords = new HashMap<>();
    
    /**
     * 记录一次连锁采集
     */
    public static synchronized void recordUltimine(ServerPlayer player, List<BlockRecord> blocks, BlockPos centerPos) {
        if (blocks == null || blocks.isEmpty()) {
            return;
        }
        
        UUID playerId = player.getUUID();
        Deque<UltimineRecord> records = playerRecords.computeIfAbsent(playerId, ignored -> new ArrayDeque<>());
        records.removeIf(UltimineRecord::isExpired);
        records.addLast(new UltimineRecord(playerId, System.currentTimeMillis(), blocks, centerPos));
        // 仅保留最近五次记录。
        while (records.size() > MAX_RECORDS) {
            records.removeFirst();
        }
    }
    
    /**
     * 获取玩家的撤销记录
     * @return 记录，如果不存在或已过期则返回null
     */
    @Nullable
    public static synchronized UltimineRecord getRecord(UUID playerId) {
        Deque<UltimineRecord> records = playerRecords.get(playerId);
        if (records == null) {
            return null;
        }
        records.removeIf(UltimineRecord::isExpired);
        if (records.isEmpty()) {
            playerRecords.remove(playerId);
            return null;
        }
        return records.peekLast();
    }
    
    /**
     * 清除玩家的撤销记录
     */
    public static synchronized void clearRecord(UUID playerId) {
        updateRecord(playerId, null);
    }
    
    /**
     * 更新玩家的撤销记录（部分恢复后）
     * @param playerId 玩家ID
     * @param newRecord 新的记录（如果为null则清除记录）
     */
    public static synchronized void updateRecord(UUID playerId, UltimineRecord newRecord) {
        Deque<UltimineRecord> records = playerRecords.get(playerId);
        if (records == null) {
            return;
        }
        if (!records.isEmpty()) {
            records.removeLast();
        }
        if (newRecord != null && !newRecord.getBlocks().isEmpty()) {
            records.addLast(newRecord);
        }
        if (records.isEmpty()) {
            playerRecords.remove(playerId);
        }
    }
    
    /**
     * 清除所有过期的记录
     */
    public static synchronized void cleanupExpiredRecords() {
        playerRecords.entrySet().removeIf(entry -> {
            entry.getValue().removeIf(UltimineRecord::isExpired);
            return entry.getValue().isEmpty();
        });
    }
    
    /**
     * 检查玩家是否有可用的撤销记录
     */
    public static boolean hasRecord(UUID playerId) {
        return getRecord(playerId) != null;
    }
}
