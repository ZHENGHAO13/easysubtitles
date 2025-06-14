package com.zhenghao123.easysubtitles;

import com.zhenghao123.easysubtitles.config.ConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@OnlyIn(Dist.CLIENT)
public class CommandPlayListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SOUND_PREFIX = "subtitles.sound.";

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        if (!ConfigHandler.AUTO_SUBTITLE.get()) return;

        try {
            String fullCommand = event.getParseResults().getReader().getString().trim();
            LOGGER.info("客户端捕获命令: {}", fullCommand);

            if (!fullCommand.startsWith("/playsound ") && !fullCommand.startsWith("playsound ")) {
                LOGGER.debug("非/playsound命令，跳过处理");
                return;
            }

            if (fullCommand.startsWith("/")) {
                fullCommand = fullCommand.substring(1);
            }

            String[] parts = fullCommand.split("\\s+", 5);

            if (parts.length < 4) {
                LOGGER.warn("无效的/playsound命令格式: {} (参数不足)", fullCommand);
                return;
            }

            ResourceLocation soundId = ResourceLocation.tryParse(parts[1]);
            if (soundId == null) {
                LOGGER.warn("无法解析声音ID: {}", parts[1]);
                return;
            }
            LOGGER.info("声音ID: namespace={}, path={}", soundId.getNamespace(), soundId.getPath());

            // 使用正确的常量名
            if (!ConfigHandler.SUBTITLE_NAMESPACE.equals(soundId.getNamespace())) {
                LOGGER.debug("跳过非字幕声音: {}", soundId);
                return;
            }

            if (!soundId.getPath().startsWith(SOUND_PREFIX)) {
                LOGGER.warn("声音ID缺少字幕前缀: {}", soundId);
                return;
            }

            String soundName = soundId.getPath().substring(SOUND_PREFIX.length());
            LOGGER.info("提取声音名称: {}", soundName);

            event.setCanceled(true);
            LOGGER.info("已取消原始命令执行");

            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                LOGGER.warn("无法获取本地玩家");
                return;
            }

            String target = parts[3];

            if (!isForCurrentPlayer(target, player.getName().getString())) {
                LOGGER.debug("目标玩家 {} 不是当前玩家，跳过处理", target);
                return;
            }

            // 同时播放音频和字幕
            playSoundAndSubtitle(soundId, soundName);

        } catch (Exception e) {
            LOGGER.error("处理命令时出错", e);
        }
    }

    public static void playSubtitleFile(String fileName) {
        LOGGER.info("播放字幕文件: {}", fileName);
        File file = new File(CommandHandler.getSubDir(), fileName + ".srt");
        SubtitlePlayer.play(file);
    }

    private static boolean isForCurrentPlayer(String target, String currentPlayerName) {
        if (Minecraft.getInstance().isSingleplayer()) {
            return true;
        }

        String[] targets = target.split(",");
        for (String t : targets) {
            t = t.trim();
            if (t.equals("@a") ||
                    t.equals("@s") ||
                    t.equals("@p") ||
                    t.equals("*") ||
                    t.equalsIgnoreCase(currentPlayerName)) {
                return true;
            }
        }
        return false;
    }

    public static void playSoundAndSubtitle(ResourceLocation soundId, String soundName) {
        LOGGER.info("在客户端播放声音和字幕: {}", soundId);

        // 清除任何可能的过期字幕
        if (SubtitlePlayer.getDisplayUntil() > 0 && System.currentTimeMillis() > SubtitlePlayer.getDisplayUntil()) {
            LOGGER.info("清除过期字幕");
            SubtitlePlayer.stop();
            SubtitleRenderer.clearSubtitle();
        }

        // 播放音频
        try {
            SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundId);
            if (soundEvent == null) {
                LOGGER.error("无法创建声音事件: {}", soundId);
                return;
            }

            // 创建声音实例并播放
            SimpleSoundInstance soundInstance = SimpleSoundInstance.forUI(
                    soundEvent,
                    1.0f, // 音量
                    1.0f  // 音调
            );

            Minecraft.getInstance().getSoundManager().play(soundInstance);
        } catch (Exception e) {
            LOGGER.error("播放声音失败", e);
        }

        // 播放字幕
        playSubtitleFile(soundName);
    }
}