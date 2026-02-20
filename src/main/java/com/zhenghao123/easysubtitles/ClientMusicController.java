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

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = EasySubtitlesMod.MODID, value = Dist.CLIENT)
public class ClientMusicController {
    private static long muteUntil = 0;
    private static boolean isMuted = false;
    private static SoundInstance pausedMusic = null; // 记录被暂停的音乐实例

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase == TickEvent.Phase.END && isMuted) {
            if (System.currentTimeMillis() > muteUntil) {
                isMuted = false;
                // 时间到，立刻恢复播放
                if (pausedMusic != null) {
                    Minecraft.getInstance().getSoundManager().play(pausedMusic);
                    pausedMusic = null; // 恢复后清除引用
                }
            }
        }
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        if (isMuted && event.getSound() != null) {
            // 如果在静音期间有新的背景音乐尝试播放，将其拦截并存入待播放队列
            if (event.getSound().getSource() == SoundSource.MUSIC) {
                pausedMusic = event.getSound();
                event.setSound(null); // 阻止当前播放
            }
        }
    }

    public static void scheduleMute(long durationMs) {
        muteUntil = System.currentTimeMillis() + durationMs;
        isMuted = true;

        // 停止当前音乐前，不尝试捕获它（因为原生引擎无法直接提取正在播放的实例并续播）
        // 但我们会拦截接下来的播放请求
        Minecraft.getInstance().getSoundManager().stop(null, SoundSource.MUSIC);
    }
}