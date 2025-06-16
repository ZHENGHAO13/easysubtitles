package com.zhenghao123.easysubtitles;

import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.network.NetworkEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.function.Supplier;

public class StopSubtitlePacket {
    private static final Logger LOGGER = LogManager.getLogger();
    private final ResourceLocation soundId;
    private final SoundSource source;

    public StopSubtitlePacket(ResourceLocation soundId, SoundSource source) {
        this.soundId = soundId;
        this.source = source;
    }

    public StopSubtitlePacket(FriendlyByteBuf buf) {
        this.soundId = buf.readNullable(FriendlyByteBuf::readResourceLocation);
        this.source = buf.readEnum(SoundSource.class);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeNullable(soundId, FriendlyByteBuf::writeResourceLocation);
        buf.writeEnum(source);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            // 只在客户端执行
            if (ctx.get().getDirection().getReceptionSide().isClient()) {
                LOGGER.info("客户端收到停止字幕请求: sound={}, source={}", soundId, source);

                // 停止音频播放
                if (soundId != null) {
                    // 停止特定声音
                    Minecraft.getInstance().getSoundManager().stop(soundId, source);
                    // 停止对应字幕
                    SubtitlePlayer.stop(soundId);
                } else {
                    // 停止所有声音
                    Minecraft.getInstance().getSoundManager().stop(null, source);
                    // 停止所有字幕
                    SubtitlePlayer.stop(true);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
}