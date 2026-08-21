package com.ultimine_rewind.network;

import dev.ftb.mods.ftblibrary.util.NetworkHelper;

public final class NetworkHandler {
    private NetworkHandler() {
    }

    public static void register() {
        NetworkHelper.registerC2S(OpenRewindScreenPacket.TYPE, OpenRewindScreenPacket.STREAM_CODEC,
                OpenRewindScreenPacket::handle);
        NetworkHelper.registerC2S(ConfirmRewindPacket.TYPE, ConfirmRewindPacket.STREAM_CODEC,
                ConfirmRewindPacket::handle);
    }
}
