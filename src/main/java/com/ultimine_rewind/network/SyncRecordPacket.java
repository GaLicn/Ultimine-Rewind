package com.ultimine_rewind.network;

import com.ultimine_rewind.Ultimine_rewind;
import com.ultimine_rewind.data.MaterialRequirement;
import com.ultimine_rewind.data.UltimineRecord;
import com.ultimine_rewind.menu.RewindMenu;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;


/**
 * 同步撤销记录到客户端的数据包
 */
public record SyncRecordPacket(java.util.List<MaterialRequirement> requiredMaterials,
                               int blockCount) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<SyncRecordPacket> TYPE = new CustomPacketPayload.Type<>(
            ResourceLocation.fromNamespaceAndPath(Ultimine_rewind.MODID, "sync_record"));

    private static final StreamCodec<RegistryFriendlyByteBuf, MaterialRequirement> MATERIAL_CODEC =
            StreamCodec.composite(
                    ItemStack.STREAM_CODEC, MaterialRequirement::stack,
                    ByteBufCodecs.VAR_INT, MaterialRequirement::count,
                    MaterialRequirement::new
            );

    // 完整 Payload 编解码器（复合：Map + int）
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncRecordPacket> STREAM_CODEC =
            StreamCodec.composite(
                    MATERIAL_CODEC.apply(ByteBufCodecs.list()),
                    SyncRecordPacket::requiredMaterials,
                    ByteBufCodecs.INT,             // blockCount
                    SyncRecordPacket::blockCount,
                    SyncRecordPacket::new          // 构造函数
            );

    // 工厂方法：从 UltimineRecord 创建（处理 null）
    public static SyncRecordPacket from(UltimineRecord record) {
        if (record == null) {
            return new SyncRecordPacket(java.util.List.of(), 0);
        }
        return new SyncRecordPacket(record.requiredMaterials(), record.getBlockCount());
    }

    // 静态处理方法（客户端接收）
    public static void handle(SyncRecordPacket msg, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            LocalPlayer player = Minecraft.getInstance().player;
            if (player != null && player.containerMenu instanceof RewindMenu menu) {
                menu.setClientRecordData(msg.requiredMaterials(), msg.blockCount());
            }
        });
    }

    @Override
    public CustomPacketPayload.@NotNull Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
