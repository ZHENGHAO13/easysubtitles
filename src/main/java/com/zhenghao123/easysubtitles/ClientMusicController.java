package com.zhenghao123.easysubtitles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = EasySubtitlesMod.MODID, value = Dist.CLIENT)
public class ClientMusicController {
    private static final Logger LOGGER = LogManager.getLogger();
    private static long muteUntil = 0;
    private static final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null) return;

        if (sound.getSource() == SoundSource.MUSIC && System.currentTimeMillis() < muteUntil) {
            event.setCanceled(true);
            LOGGER.debug("已阻止背景音乐播放: {}", sound.getLocation());
        }
    }

    public static void scheduleMute(long durationMs) {
        muteUntil = System.currentTimeMillis() + durationMs;

        scheduler.schedule(() -> {
            muteUntil = 0;
            LOGGER.info("背景音乐静音结束");
        }, durationMs, TimeUnit.MILLISECONDS);

        Minecraft.getInstance().getSoundManager().stop(null, SoundSource.MUSIC);
        LOGGER.info("背景音乐已静音 {} 毫秒", durationMs);
    }

    public static long getMuteUntil() {
        return muteUntil;
    }
}