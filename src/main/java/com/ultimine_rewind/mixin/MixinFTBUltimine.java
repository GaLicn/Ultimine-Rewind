package com.ultimine_rewind.mixin;

import com.ultimine_rewind.data.BlockRecord;
import com.ultimine_rewind.data.RewindDataManager;
import dev.ftb.mods.ftbultimine.FTBUltimine;
import dev.ftb.mods.ftbultimine.FTBUltiminePlayerData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** 在 FTB Ultimine 破坏方块前后截取本次连锁的方块快照。 */
@Mixin(value = FTBUltimine.class, remap = false)
public abstract class MixinFTBUltimine {
    @Shadow
    public abstract FTBUltiminePlayerData getOrCreatePlayerData(net.minecraft.world.entity.player.Player player);

    @Unique
    private List<BlockRecord> ultimineRewind$records;

    @Inject(
            method = "handleBlockBreak",
            at = @At(
                    value = "FIELD",
                    target = "Ldev/ftb/mods/ftbultimine/FTBUltimine;isBreakingBlock:Z",
                    opcode = Opcodes.PUTFIELD,
                    ordinal = 0,
                    shift = At.Shift.AFTER))
    private void ultimineRewind$capture(LevelAccessor level, BlockPos origin, BlockState state,
                                       ServerPlayer player, CallbackInfoReturnable<Boolean> callback) {
        if (!(level instanceof ServerLevel serverLevel)) {
            return;
        }

        FTBUltiminePlayerData data = getOrCreatePlayerData(player);
        if (!data.hasCachedPositions()) {
            return;
        }

        ultimineRewind$records = new ArrayList<>();
        for (BlockPos pos : Objects.requireNonNull(data.cachedPositions())) {
            BlockState blockState = serverLevel.getBlockState(pos);
            BlockEntity blockEntity = serverLevel.getBlockEntity(pos);
            CompoundTag blockEntityData = blockEntity == null ? null : blockEntity.saveWithFullMetadata(serverLevel.registryAccess());
            // 克隆物品栈可正确保留模组方块映射与物品组件。
            var requiredItem = blockState.getBlock().getCloneItemStack(
                    serverLevel, pos, blockState, false, player);
            ultimineRewind$records.add(new BlockRecord(pos, blockState, requiredItem, blockEntityData));
        }
    }

    @Inject(
            method = "handleBlockBreak",
            at = @At(
                    value = "FIELD",
                    target = "Ldev/ftb/mods/ftbultimine/FTBUltimine;isBreakingBlock:Z",
                    opcode = Opcodes.PUTFIELD,
                    ordinal = 1,
                    shift = At.Shift.AFTER))
    private void ultimineRewind$save(LevelAccessor level, BlockPos origin, BlockState state,
                                    ServerPlayer player, CallbackInfoReturnable<Boolean> callback) {
        if (ultimineRewind$records == null || ultimineRewind$records.isEmpty()) {
            return;
        }

        // 只保留实际已被破坏的位置，避免工具损坏等中断造成错误记录。
        ultimineRewind$records.removeIf(record -> !level.getBlockState(record.pos()).isAir());
        if (!ultimineRewind$records.isEmpty()) {
            RewindDataManager.record(player, ultimineRewind$records, origin);
            player.sendOverlayMessage(Component.translatable(
                    "message.ultimine_rewind.recorded", ultimineRewind$records.size()).withStyle(ChatFormatting.GRAY));
        }
        ultimineRewind$records = null;
    }
}
