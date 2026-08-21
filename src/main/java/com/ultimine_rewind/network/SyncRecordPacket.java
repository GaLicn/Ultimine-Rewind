package com.ultimine_rewind.network;

import com.ultimine_rewind.UltimineRewind;
import com.ultimine_rewind.data.UltimineRecord;
import com.ultimine_rewind.menu.RewindMenu;
import dev.ftb.mods.ftblibrary.platform.network.PacketContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;

import java.util.LinkedHashMap;
import java.util.Map;

public record SyncRecordPacket(Map<Item, Integer> requiredItems, int blockCount) implements CustomPacketPayload {
    public static final Type<SyncRecordPacket> TYPE = new Type<>(
            Identifier.fromNamespaceAndPath(UltimineRewind.MODID, "sync_record"));
    public static final StreamCodec<FriendlyByteBuf, SyncRecordPacket> STREAM_CODEC = StreamCodec.of(
            SyncRecordPacket::encode, SyncRecordPacket::decode);

    public static SyncRecordPacket from(UltimineRecord record) {
        return new SyncRecordPacket(record.requiredItems(), record.blockCount());
    }

    private static void encode(FriendlyByteBuf buffer, SyncRecordPacket packet) {
        buffer.writeVarInt(packet.blockCount);
        buffer.writeVarInt(packet.requiredItems.size());
        packet.requiredItems.forEach((item, count) -> {
            buffer.writeVarInt(BuiltInRegistries.ITEM.getId(item));
            buffer.writeVarInt(count);
        });
    }

    private static SyncRecordPacket decode(FriendlyByteBuf buffer) {
        int blockCount = buffer.readVarInt();
        Map<Item, Integer> requiredItems = new LinkedHashMap<>();
        int size = buffer.readVarInt();
        for (int index = 0; index < size; index++) {
            requiredItems.put(BuiltInRegistries.ITEM.byId(buffer.readVarInt()), buffer.readVarInt());
        }
        return new SyncRecordPacket(requiredItems, blockCount);
    }

    @Override
    public Type<SyncRecordPacket> type() {
        return TYPE;
    }

    public static void handle(SyncRecordPacket packet, PacketContext context) {
        if (context.player().containerMenu instanceof RewindMenu menu) {
            menu.setClientData(packet.requiredItems, packet.blockCount);
        }
    }
}
