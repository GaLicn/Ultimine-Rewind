package com.ultimine_rewind.network;


import com.ultimine_rewind.Ultimine_rewind;
import com.ultimine_rewind.logic.RewindExecutor;
import com.ultimine_rewind.menu.RewindMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * 确认恢复的数据包（客户端→服务端）
 */
public class ConfirmRewindPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ConfirmRewindPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Ultimine_rewind.MODID, "confirm_rewind"));

    public static final ConfirmRewindPacket INSTANCE = new ConfirmRewindPacket();

    public static final StreamCodec<FriendlyByteBuf, ConfirmRewindPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);


    private ConfirmRewindPacket() {}

    public static void handle(final ConfirmRewindPacket msg, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                // 检查玩家是否正在使用撤销菜单
                if (player.containerMenu instanceof RewindMenu menu) {
                    RewindExecutor.executeRewind(player, menu);
                    player.closeContainer();
                }
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

