package com.ultimine_rewind.network;

import com.ultimine_rewind.data.RewindDataManager;
import com.ultimine_rewind.data.UltimineRecord;
import com.ultimine_rewind.menu.RewindMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * 打开撤销界面的数据包（客户端→服务端）
 */
public class OpenRewindScreenPacket {
    
    public OpenRewindScreenPacket() {
    }
    
    public static void encode(OpenRewindScreenPacket packet, FriendlyByteBuf buf) {
        // 无需编码数据
    }
    
    public static OpenRewindScreenPacket decode(FriendlyByteBuf buf) {
        return new OpenRewindScreenPacket();
    }
    
    public static void handle(OpenRewindScreenPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            // 获取玩家的撤销记录
            UltimineRecord record = RewindDataManager.getRecord(player.getUUID());
            
            if (record == null) {
                player.displayClientMessage(
                    Component.literal("没有可撤销的连锁采集记录").withStyle(ChatFormatting.RED),
                    true
                );
                return;
            }
            
            // 打开容器界面
            player.openMenu(new SimpleMenuProvider(
                (containerId, playerInventory, p) -> new RewindMenu(containerId, playerInventory, record),
                Component.literal("连锁采集撤销")
            ));
        });
        context.setPacketHandled(true);
    }
}

