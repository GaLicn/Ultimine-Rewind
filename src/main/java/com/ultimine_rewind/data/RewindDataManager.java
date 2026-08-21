package com.ultimine_rewind.data;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 保存每名玩家最近五次、十分钟内有效的记录。 */
public final class RewindDataManager {
    private static final int MAX_RECORDS = 5;
    private static final Map<UUID, Deque<UltimineRecord>> PLAYER_RECORDS = new ConcurrentHashMap<>();

    private RewindDataManager() {
    }

    public static synchronized void record(ServerPlayer player, List<BlockRecord> blocks, BlockPos centerPos) {
        if (!blocks.isEmpty()) {
            Deque<UltimineRecord> records = PLAYER_RECORDS.computeIfAbsent(
                    player.getUUID(), ignored -> new ArrayDeque<>());
            records.removeIf(UltimineRecord::isExpired);
            records.addLast(new UltimineRecord(
                    player.getUUID(), System.currentTimeMillis(), blocks, centerPos));
            // 新记录进入队尾，超出上限时丢弃最早的一条。
            while (records.size() > MAX_RECORDS) {
                records.removeFirst();
            }
        }
    }

    @Nullable
    public static synchronized UltimineRecord get(UUID playerId) {
        Deque<UltimineRecord> records = PLAYER_RECORDS.get(playerId);
        if (records == null) {
            return null;
        }
        records.removeIf(UltimineRecord::isExpired);
        if (records.isEmpty()) {
            PLAYER_RECORDS.remove(playerId);
            return null;
        }
        return records.peekLast();
    }

    public static synchronized void update(UUID playerId, @Nullable UltimineRecord record) {
        Deque<UltimineRecord> records = PLAYER_RECORDS.get(playerId);
        if (records == null) {
            return;
        }
        if (!records.isEmpty()) {
            records.removeLast();
        }
        if (record != null) {
            records.addLast(record);
        }
        if (records.isEmpty()) {
            PLAYER_RECORDS.remove(playerId);
        }
    }
}
