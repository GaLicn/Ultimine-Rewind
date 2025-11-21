package com.ultimine_rewind.network;

import com.ultimine_rewind.Ultimine_rewind;
import com.ultimine_rewind.data.RewindDataManager;
import com.ultimine_rewind.data.UltimineRecord;
import com.ultimine_rewind.menu.RewindMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

/**
 * 打开撤销界面的数据包（客户端→服务端）
 */
public class OpenRewindScreenPacket implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<OpenRewindScreenPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Ultimine_rewind.MODID, "open_rewind_screen"));

    public static final OpenRewindScreenPacket INSTANCE = new OpenRewindScreenPacket();

    public static final StreamCodec<FriendlyByteBuf, OpenRewindScreenPacket> STREAM_CODEC =
            StreamCodec.unit(INSTANCE);

    private OpenRewindScreenPacket() {}

    public static void handle(final OpenRewindScreenPacket msg, final IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (ctx.player() instanceof ServerPlayer player) {
                // 获取玩家的撤销记录
                UltimineRecord record = RewindDataManager.getRecord(player.getUUID());

                if (record == null) {
                    player.displayClientMessage(
                            Component.translatable("message.ultimine_rewind.no_record").withStyle(ChatFormatting.RED),
                            true
                    );
                    return;
                }

                // 打开容器界面并发送同步数据
                player.openMenu(new SimpleMenuProvider(
                        (containerId, playerInventory, p) -> new RewindMenu(containerId, playerInventory, record),
                        Component.translatable("container.ultimine_rewind.rewind_menu")
                ));

                // 菜单打开后发送数据同步包到客户端
                PacketDistributor.sendToPlayer(player, SyncRecordPacket.from(record));
            }
        });
    }

    @Override
    public @NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

