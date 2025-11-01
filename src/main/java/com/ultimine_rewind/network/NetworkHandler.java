package com.ultimine_rewind.network;

import com.ultimine_rewind.Ultimine_rewind;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

/**
 * 网络处理器
 */
public class NetworkHandler {
    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(Ultimine_rewind.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );
    
    private static int packetId = 0;
    
    private static int id() {
        return packetId++;
    }
    
    public static void register() {
        INSTANCE.registerMessage(
                id(),
                OpenRewindScreenPacket.class,
                OpenRewindScreenPacket::encode,
                OpenRewindScreenPacket::decode,
                OpenRewindScreenPacket::handle
        );
        
        INSTANCE.registerMessage(
                id(),
                ConfirmRewindPacket.class,
                ConfirmRewindPacket::encode,
                ConfirmRewindPacket::decode,
                ConfirmRewindPacket::handle
        );
        
        INSTANCE.registerMessage(
                id(),
                SyncRecordPacket.class,
                SyncRecordPacket::encode,
                SyncRecordPacket::decode,
                SyncRecordPacket::handle
        );
    }
}

