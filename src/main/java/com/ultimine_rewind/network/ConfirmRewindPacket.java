package com.ultimine_rewind.network;

import com.ultimine_rewind.UltimineRewind;
import com.ultimine_rewind.logic.RewindExecutor;
import com.ultimine_rewind.menu.RewindMenu;
import dev.ftb.mods.ftblibrary.platform.network.PacketContext;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

public record ConfirmRewindPacket() implements CustomPacketPayload {
    public static final Type<ConfirmRewindPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(UltimineRewind.MODID, "confirm_rewind"));
    public static final StreamCodec<FriendlyByteBuf, ConfirmRewindPacket> STREAM_CODEC =
            StreamCodec.unit(new ConfirmRewindPacket());

    @Override
    public Type<ConfirmRewindPacket> type() {
        return TYPE;
    }

    public static void handle(ConfirmRewindPacket packet, PacketContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        if (player.containerMenu instanceof RewindMenu menu) {
            RewindExecutor.execute(player, menu);
            player.closeContainer();
        }
    }
}
