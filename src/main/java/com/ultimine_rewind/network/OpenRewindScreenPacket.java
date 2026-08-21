package com.ultimine_rewind.network;

import com.ultimine_rewind.UltimineRewind;
import com.ultimine_rewind.data.RewindDataManager;
import com.ultimine_rewind.data.UltimineRecord;
import com.ultimine_rewind.menu.RewindMenu;
import dev.ftb.mods.ftblibrary.platform.network.PacketContext;
import dev.ftb.mods.ftblibrary.platform.network.Server2PlayNetworking;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;

public record OpenRewindScreenPacket() implements CustomPacketPayload {
    public static final Type<OpenRewindScreenPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(UltimineRewind.MODID, "open_screen"));
    public static final StreamCodec<FriendlyByteBuf, OpenRewindScreenPacket> STREAM_CODEC =
            StreamCodec.unit(new OpenRewindScreenPacket());

    @Override
    public Type<OpenRewindScreenPacket> type() {
        return TYPE;
    }

    public static void handle(OpenRewindScreenPacket packet, PacketContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        UltimineRecord record = RewindDataManager.get(player.getUUID());
        if (record == null) {
            player.sendOverlayMessage(Component.translatable(
                    "message.ultimine_rewind.no_record").withStyle(ChatFormatting.RED));
            return;
        }

        player.openMenu(new SimpleMenuProvider(
                (containerId, inventory, ignored) -> new RewindMenu(containerId, inventory, record),
                Component.translatable("container.ultimine_rewind.rewind_menu")));
        Server2PlayNetworking.send(player, SyncRecordPacket.from(record));
    }
}
