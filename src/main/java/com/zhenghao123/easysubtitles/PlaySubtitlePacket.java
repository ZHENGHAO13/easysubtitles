package com.zhenghao123.easysubtitles;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class PlaySubtitlePacket {
    private static final Logger LOGGER = LogManager.getLogger();
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
                LOGGER.info("客户端收到播放字幕请求: {}", fileName);

                // 清除任何正在显示的字幕
                if (SubtitlePlayer.getDisplayUntil() > 0 && System.currentTimeMillis() > SubtitlePlayer.getDisplayUntil()) {
                    LOGGER.info("清除过期字幕");
                    SubtitlePlayer.stop();
                    SubtitleRenderer.clearSubtitle();
                }

                // 生成声音ID
                ResourceLocation soundId = new ResourceLocation(
                        EasySubtitlesMod.MODID,
                        "subtitles.sound." + fileName
                );

                // 同时播放声音和字幕
                CommandPlayListener.playSoundAndSubtitle(soundId, fileName);
            }
        });
        ctx.get().setPacketHandled(true);
    }
}