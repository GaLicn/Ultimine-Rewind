package com.ultimine_rewind.data;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/** 仅保存每名玩家最近一次、十分钟内有效的记录。 */
public final class RewindDataManager {
    private static final Map<UUID, UltimineRecord> PLAYER_RECORDS = new ConcurrentHashMap<>();

    private RewindDataManager() {
    }

    public static void record(ServerPlayer player, List<BlockRecord> blocks, BlockPos centerPos) {
        if (!blocks.isEmpty()) {
            PLAYER_RECORDS.put(player.getUUID(), new UltimineRecord(
                    player.getUUID(), System.currentTimeMillis(), blocks, centerPos));
        }
    }

    @Nullable
    public static UltimineRecord get(UUID playerId) {
        UltimineRecord record = PLAYER_RECORDS.get(playerId);
        if (record != null && record.isExpired()) {
            PLAYER_RECORDS.remove(playerId);
            return null;
        }
        return record;
    }

    public static void update(UUID playerId, @Nullable UltimineRecord record) {
        if (record == null) {
            PLAYER_RECORDS.remove(playerId);
        } else {
            PLAYER_RECORDS.put(playerId, record);
        }
    }
}
