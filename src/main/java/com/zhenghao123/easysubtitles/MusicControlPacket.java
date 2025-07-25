package com.zhenghao123.easysubtitles;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;
import java.util.function.Supplier;

public class MusicControlPacket {
    private final long muteDurationMs;

    public MusicControlPacket(long muteDurationMs) {
        this.muteDurationMs = muteDurationMs;
    }

    public MusicControlPacket(FriendlyByteBuf buf) {
        this.muteDurationMs = buf.readLong();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeLong(muteDurationMs);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 只在客户端执行
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                MusicController.scheduleMute(muteDurationMs);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}