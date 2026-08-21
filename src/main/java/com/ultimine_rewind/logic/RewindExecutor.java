package com.ultimine_rewind.logic;

import com.ultimine_rewind.data.BlockRecord;
import com.ultimine_rewind.data.RewindDataManager;
import com.ultimine_rewind.data.UltimineRecord;
import com.ultimine_rewind.menu.RewindMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.List;

public final class RewindExecutor {
    private RewindExecutor() {
    }

    public static boolean execute(ServerPlayer player, RewindMenu menu) {
        UltimineRecord record = menu.record();
        if (record == null || !menu.validateItems()) {
            player.sendSystemMessage(Component.translatable(
                    "message.ultimine_rewind.no_materials").withStyle(ChatFormatting.RED), false);
            return false;
        }

        ServerLevel level = player.level();
        List<BlockRecord> candidates = menu.restorableBlocks();
        List<BlockRecord> restored = new ArrayList<>();
        for (BlockRecord block : candidates) {
            BlockState current = level.getBlockState(block.pos());
            if (!level.isLoaded(block.pos()) || (!current.isAir() && !current.canBeReplaced())) {
                continue;
            }

            if (level.setBlock(block.pos(), block.state(), Block.UPDATE_ALL)) {
                // 方块实体数据仅在创造模式恢复，避免复制容器内容。
                if (player.isCreative() && block.blockEntityData() != null) {
                    BlockEntity blockEntity = level.getBlockEntity(block.pos());
                    BlockEntity restoredEntity = BlockEntity.loadStatic(
                            block.pos(), block.state(), block.blockEntityData(), level.registryAccess());
                    if (blockEntity != null && restoredEntity != null) {
                        level.setBlockEntity(restoredEntity);
                    }
                }
                restored.add(block);
            }
        }

        menu.consumeFor(restored);
        UltimineRecord remaining = record.withoutRestored(restored);
        RewindDataManager.update(player.getUUID(), remaining);

        if (remaining == null) {
            String key = player.isCreative()
                    ? "message.ultimine_rewind.success_creative"
                    : "message.ultimine_rewind.success";
            player.sendSystemMessage(Component.translatable(key, restored.size()).withStyle(ChatFormatting.GREEN));
        } else {
            player.sendSystemMessage(Component.translatable(
                    "message.ultimine_rewind.partial_restore", restored.size(), record.blockCount())
                    .withStyle(ChatFormatting.YELLOW));
            player.sendOverlayMessage(Component.translatable(
                    "message.ultimine_rewind.remaining", remaining.blockCount()).withStyle(ChatFormatting.GRAY));
        }
        return !restored.isEmpty();
    }
}
