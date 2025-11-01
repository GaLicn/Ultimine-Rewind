package com.ultimine_rewind.network;

import com.ultimine_rewind.data.UltimineRecord;
import com.ultimine_rewind.menu.RewindMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.Item;
import net.minecraftforge.network.NetworkEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Supplier;

/**
 * 同步撤销记录到客户端的数据包
 */
public class SyncRecordPacket {
    
    private final Map<Item, Integer> requiredItems;
    private final int blockCount;
    
    public SyncRecordPacket(UltimineRecord record) {
        this.requiredItems = record != null ? record.getRequiredItems() : new HashMap<>();
        this.blockCount = record != null ? record.getBlockCount() : 0;
    }
    
    public SyncRecordPacket(Map<Item, Integer> requiredItems, int blockCount) {
        this.requiredItems = requiredItems;
        this.blockCount = blockCount;
    }
    
    public static void encode(SyncRecordPacket packet, FriendlyByteBuf buf) {
        buf.writeInt(packet.blockCount);
        buf.writeInt(packet.requiredItems.size());
        
        for (Map.Entry<Item, Integer> entry : packet.requiredItems.entrySet()) {
            buf.writeInt(Item.getId(entry.getKey()));
            buf.writeInt(entry.getValue());
        }
    }
    
    public static SyncRecordPacket decode(FriendlyByteBuf buf) {
        int blockCount = buf.readInt();
        int mapSize = buf.readInt();
        Map<Item, Integer> requiredItems = new HashMap<>();
        
        for (int i = 0; i < mapSize; i++) {
            int itemId = buf.readInt();
            int count = buf.readInt();
            Item item = Item.byId(itemId);
            if (item != null) {
                requiredItems.put(item, count);
            }
        }
        
        return new SyncRecordPacket(requiredItems, blockCount);
    }
    
    public static void handle(SyncRecordPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && minecraft.player.containerMenu instanceof RewindMenu menu) {
                // 将数据设置到菜单中
                menu.setClientRecordData(packet.requiredItems, packet.blockCount);
                
                // 调试日志
                System.out.println("[Ultimine Rewind] 客户端接收到数据: " + 
                    packet.requiredItems.size() + " 种物品, " + packet.blockCount + " 个方块");
            }
        });
        context.setPacketHandled(true);
    }
}

