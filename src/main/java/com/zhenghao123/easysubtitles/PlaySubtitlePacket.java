package com.zhenghao123.easysubtitles;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class PlaySubtitlePacket {
    private final String fileName;

    public PlaySubtitlePacket(String fileName) {
        this.fileName = fileName;
    }

    public PlaySubtitlePacket(FriendlyByteBuf buf) {
        this.fileName = buf.readUtf();
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeUtf(fileName);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 只在客户端执行
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                CommandPlayListener.playSubtitleFile(fileName);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}