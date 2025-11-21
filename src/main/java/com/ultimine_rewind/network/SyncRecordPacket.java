package com.ultimine_rewind.network;

import com.ultimine_rewind.Ultimine_rewind;
import com.ultimine_rewind.data.UltimineRecord;
import com.ultimine_rewind.menu.RewindMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

/**
 * 同步撤销记录到客户端的数据包
 */
public record SyncRecordPacket(Map<Item, Integer> requiredItems, int blockCount) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<SyncRecordPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Ultimine_rewind.MODID, "sync_record"));

    // Holder<Item> 的 Map 编解码器（使用注册表友好缓冲区）
    private static final StreamCodec<RegistryFriendlyByteBuf, Map<Holder<Item>, Integer>> HOLDER_MAP_CODEC =
            ByteBufCodecs.map(
                    HashMap::new,                                   // 空 Map 工厂
                    ByteBufCodecs.holderRegistry(Registries.ITEM),  // Item Holder 编解码器
                    ByteBufCodecs.INT,                              // Integer 值
                    8192                                            // 最大条目数，防止滥用（根据需求调整）
            );

    // 转换编解码器：Map<Holder<Item>, Integer> ↔ Map<Item, Integer>
    private static final StreamCodec<RegistryFriendlyByteBuf, Map<Item, Integer>> ITEM_MAP_CODEC =
            HOLDER_MAP_CODEC.map(
                    // decode: HolderMap → ItemMap
                    holderMap -> holderMap.entrySet().stream()
                            .collect(toMap(entry -> entry.getKey().value(), Map.Entry::getValue)),
                    // encode: ItemMap → HolderMap
                    itemMap -> {
                        Map<Holder<Item>, Integer> holderMap = new HashMap<>();
                        itemMap.forEach((item, count) -> holderMap.put(Holder.direct(item), count));
                        return holderMap;
                    }
            );

    // 完整 Payload 编解码器（复合：Map + int）
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncRecordPacket> STREAM_CODEC =
            StreamCodec.composite(
                    ITEM_MAP_CODEC,                // requiredItems
                    SyncRecordPacket::requiredItems,
                    ByteBufCodecs.INT,             // blockCount
                    SyncRecordPacket::blockCount,
                    SyncRecordPacket::new          // 构造函数
            );

    // 工厂方法：从 UltimineRecord 创建（处理 null）
    public static SyncRecordPacket from(UltimineRecord record) {
        if (record == null) {
            return new SyncRecordPacket(Map.of(), 0);
        }
        return new SyncRecordPacket(record.getRequiredItems(), record.getBlockCount());
    }

    // 静态处理方法（客户端接收）
    public static void handle(SyncRecordPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && player.containerMenu instanceof RewindMenu menu) {
                menu.setClientRecordData(msg.requiredItems(), msg.blockCount());
            }
        });
    }

    @Override
    public CustomPacketPayload.@NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}

