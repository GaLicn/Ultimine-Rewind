package com.ultimine_rewind.mixin;

import com.ultimine_rewind.data.BlockRecord;
import com.ultimine_rewind.data.RewindDataManager;
import dev.architectury.event.EventResult;
import dev.architectury.utils.value.IntValue;
import dev.ftb.mods.ftbultimine.FTBUltimine;
import dev.ftb.mods.ftbultimine.FTBUltiminePlayerData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

/**
 * Mixin到FTBUltimine，拦截连锁采集过程并记录方块信息
 */
@Mixin(value = FTBUltimine.class, remap = false)
public abstract class MixinFTBUltimine {
    
    @Shadow
    private boolean isBreakingBlock;
    
    @Shadow
    public abstract FTBUltiminePlayerData getOrCreatePlayerData(net.minecraft.world.entity.player.Player player);
    
    @Unique
    private List<BlockRecord> ultimine_rewind$currentBlockRecords = null;
    
    @Unique
    private BlockPos ultimine_rewind$currentCenterPos = null;
    
    /**
     * 在连锁开始时初始化记录列表并记录所有方块
     * 在isBreakingBlock被设置为true之前立即注入，此时方块还未被破坏
     */
    @Inject(
        method = "blockBroken",
        at = @At(
            value = "FIELD",
            target = "Ldev/ftb/mods/ftbultimine/FTBUltimine;isBreakingBlock:Z",
            ordinal = 0,
            shift = At.Shift.BEFORE
        ),
        remap = false
    )
    private void ultimine_rewind$onUltimineStart(Level world, BlockPos origPos, BlockState state,
                                  ServerPlayer player, @Nullable IntValue xp, CallbackInfoReturnable<EventResult> cir) {
        // 检查是否即将开始连锁（通过检查条件）
        if (world instanceof ServerLevel serverLevel && !isBreakingBlock) {
            // 获取玩家数据
            FTBUltiminePlayerData data = getOrCreatePlayerData(player);
            
            // 检查是否有缓存的方块位置（说明即将进行连锁）
            if (data.hasCachedPositions()) {
                ultimine_rewind$currentBlockRecords = new ArrayList<>();
                ultimine_rewind$currentCenterPos = origPos.immutable();
                
                // 记录所有要破坏的方块（此时它们还未被破坏）
                for (BlockPos pos : data.cachedPositions()) {
                    ultimine_rewind$recordBlock(serverLevel, pos);
                }
            }
        }
    }
    
    /**
     * 在连锁结束时保存记录
     * 在isBreakingBlock被设置回false之后立即注入
     */
    @Inject(
        method = "blockBroken",
        at = @At(
            value = "FIELD",
            target = "Ldev/ftb/mods/ftbultimine/FTBUltimine;isBreakingBlock:Z",
            ordinal = 1,
            shift = At.Shift.AFTER
        ),
        remap = false
    )
    private void ultimine_rewind$onUltimineEnd(Level world, BlockPos origPos, BlockState state,
                                ServerPlayer player, @Nullable IntValue xp, CallbackInfoReturnable<EventResult> cir) {
        // 连锁结束，保存记录
        if (ultimine_rewind$currentBlockRecords != null && !ultimine_rewind$currentBlockRecords.isEmpty()) {
            RewindDataManager.recordUltimine(player, ultimine_rewind$currentBlockRecords, ultimine_rewind$currentCenterPos);
            
            // 发送提示消息
            player.displayClientMessage(
                Component.translatable("message.ultimine_rewind.recorded", ultimine_rewind$currentBlockRecords.size())
                    .withStyle(ChatFormatting.GRAY),
                true
            );
            
            ultimine_rewind$currentBlockRecords = null;
            ultimine_rewind$currentCenterPos = null;
        }
    }
    
    /**
     * 记录方块信息的辅助方法
     */
    @Unique
    private void ultimine_rewind$recordBlock(ServerLevel level, BlockPos pos) {
        if (ultimine_rewind$currentBlockRecords == null) {
            return;
        }
        
        // 获取方块状态
        BlockState blockState = level.getBlockState(pos);
        
        // 获取方块实体数据 (如果存在)
        CompoundTag beData = null;
        BlockEntity blockEntity = level.getBlockEntity(pos);
        if (blockEntity != null) {
            beData = blockEntity.saveWithFullMetadata();
        }
        
        // 记录方块信息
        BlockRecord record = new BlockRecord(pos, blockState, beData);
        ultimine_rewind$currentBlockRecords.add(record);
    }
}

