package com.ultimine_rewind.network;

import com.ultimine_rewind.data.MaterialRequirement;
import com.ultimine_rewind.data.UltimineRecord;
import com.ultimine_rewind.menu.RewindMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/** 同步撤销记录到客户端的数据包。 */
public class SyncRecordPacket {
    private final List<MaterialRequirement> requiredMaterials;
    private final int blockCount;

    public SyncRecordPacket(UltimineRecord record) {
        this.requiredMaterials = record != null ? record.requiredMaterials() : List.of();
        this.blockCount = record != null ? record.getBlockCount() : 0;
    }

    public SyncRecordPacket(List<MaterialRequirement> requiredMaterials, int blockCount) {
        this.requiredMaterials = List.copyOf(requiredMaterials);
        this.blockCount = blockCount;
    }

    public static void encode(SyncRecordPacket packet, FriendlyByteBuf buf) {
        buf.writeVarInt(packet.blockCount);
        buf.writeVarInt(packet.requiredMaterials.size());
        for (MaterialRequirement material : packet.requiredMaterials) {
            buf.writeItem(material.stack());
            buf.writeVarInt(material.count());
        }
    }

    public static SyncRecordPacket decode(FriendlyByteBuf buf) {
        int blockCount = buf.readVarInt();
        int materialCount = buf.readVarInt();
        List<MaterialRequirement> materials = new ArrayList<>();
        for (int index = 0; index < materialCount; index++) {
            ItemStack stack = buf.readItem();
            materials.add(new MaterialRequirement(stack, buf.readVarInt()));
        }
        return new SyncRecordPacket(materials, blockCount);
    }

    public static void handle(SyncRecordPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            if (minecraft.player != null && minecraft.player.containerMenu instanceof RewindMenu menu) {
                menu.setClientRecordData(packet.requiredMaterials, packet.blockCount);
            }
        });
        context.setPacketHandled(true);
    }
}
