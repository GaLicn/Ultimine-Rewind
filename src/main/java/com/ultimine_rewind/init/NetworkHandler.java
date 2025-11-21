package com.ultimine_rewind.init;

import com.ultimine_rewind.Ultimine_rewind;
import com.ultimine_rewind.network.ConfirmRewindPacket;
import com.ultimine_rewind.network.OpenRewindScreenPacket;
import com.ultimine_rewind.network.SyncRecordPacket;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * 网络处理器
 */
public class NetworkHandler {
    public static void registerPayloadHandlers(final RegisterPayloadHandlersEvent event) {
        var registrar = event.registrar(Ultimine_rewind.MODID).versioned("1");;
        registrar.playToServer(ConfirmRewindPacket.TYPE, ConfirmRewindPacket.STREAM_CODEC, ConfirmRewindPacket::handle);
        registrar.playToServer(OpenRewindScreenPacket.TYPE, OpenRewindScreenPacket.STREAM_CODEC, OpenRewindScreenPacket::handle);
        registrar.playToClient(SyncRecordPacket.TYPE, SyncRecordPacket.STREAM_CODEC, SyncRecordPacket::handle);
    }
}

