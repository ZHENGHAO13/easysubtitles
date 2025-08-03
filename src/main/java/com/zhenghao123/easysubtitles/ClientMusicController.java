package com.zhenghao123.easysubtitles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = EasySubtitlesMod.MODID, value = Dist.CLIENT)
public class ClientMusicController {
    private static final Logger LOGGER = LogManager.getLogger();
    private static long muteUntil = 0;
    private static boolean isMuted = false;

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null) return;

        // 安全阻止音乐播放
        if (sound.getSource() == SoundSource.MUSIC && isMusicMuted()) {
            event.setSound(null); // 正确阻止音乐播放
            LOGGER.debug("已阻止背景音乐播放: {}", sound.getLocation());
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            // 检查静音是否已经过期
            if (isMuted && System.currentTimeMillis() > muteUntil) {
                LOGGER.info("背景音乐静音结束");
                isMuted = false;
            }
        }
    }

    public static void scheduleMute(long durationMs) {
        muteUntil = System.currentTimeMillis() + durationMs;
        isMuted = true;
        LOGGER.info("背景音乐已静音 {} 毫秒", durationMs);

        // 立即停止当前正在播放的背景音乐
        Minecraft.getInstance().getSoundManager().stop(null, SoundSource.MUSIC);
    }

    public static boolean isMusicMuted() {
        return isMuted && System.currentTimeMillis() < muteUntil;
    }
}