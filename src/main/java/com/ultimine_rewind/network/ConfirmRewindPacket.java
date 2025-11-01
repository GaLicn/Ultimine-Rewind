package com.ultimine_rewind.network;

import com.ultimine_rewind.logic.RewindExecutor;
import com.ultimine_rewind.menu.RewindMenu;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 确认恢复的数据包（客户端→服务端）
 */
public class ConfirmRewindPacket {
    
    public ConfirmRewindPacket() {
    }
    
    public static void encode(ConfirmRewindPacket packet, FriendlyByteBuf buf) {
        // 无需编码数据
    }
    
    public static ConfirmRewindPacket decode(FriendlyByteBuf buf) {
        return new ConfirmRewindPacket();
    }
    
    public static void handle(ConfirmRewindPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            // 检查玩家是否正在使用撤销菜单
            if (player.containerMenu instanceof RewindMenu menu) {
                RewindExecutor.executeRewind(player, menu);
                player.closeContainer();
            }
        });
        context.setPacketHandled(true);
    }
}

