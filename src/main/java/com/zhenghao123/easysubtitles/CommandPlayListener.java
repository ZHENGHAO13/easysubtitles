package com.zhenghao123.easysubtitles;

import com.zhenghao123.easysubtitles.config.ConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = EasySubtitlesMod.MODID, value = Dist.CLIENT)
public class CommandPlayListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SOUND_PREFIX = "subtitles.sound.";

    // 跟踪当前播放的声音实例和字幕信息
    private static final Map<ResourceLocation, PlaybackInfo> activePlaybacks = new HashMap<>();

    // 记录播放信息类
    private static class PlaybackInfo {
        ResourceLocation soundId;
        String soundName;
        SimpleSoundInstance soundInstance;

        PlaybackInfo(ResourceLocation soundId, String soundName, SimpleSoundInstance soundInstance) {
            this.soundId = soundId;
            this.soundName = soundName;
            this.soundInstance = soundInstance;
        }
    }

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        String fullCommand = event.getParseResults().getReader().getString().trim();
        LOGGER.info("客户端捕获命令: {}", fullCommand);

        if (isPlaySoundCommand(fullCommand)) {
            if (ConfigHandler.AUTO_SUBTITLE.get()) {
                handlePlaySoundCommand(event, fullCommand);
            } else {
                LOGGER.debug("自动字幕功能已禁用，跳过处理");
            }
        } else if (isStopSoundCommand(fullCommand)) {
            handleStopSoundCommand(fullCommand);
        }
    }

    // 在ESC菜单打开时停止所有播放
    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (event.getScreen().isPauseScreen()) {
            LOGGER.debug("检测到ESC菜单打开 - 终止所有音频和字幕播放");
            stopAllPlaybacks();
        }
    }

    private boolean isPlaySoundCommand(String fullCommand) {
        return fullCommand.startsWith("/playsound ") || fullCommand.startsWith("playsound ");
    }

    private boolean isStopSoundCommand(String fullCommand) {
        return fullCommand.startsWith("/stopsound ") || fullCommand.startsWith("stopsound ");
    }

    private void handlePlaySoundCommand(CommandEvent event, String fullCommand) {
        try {
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

            playSoundAndSubtitle(soundId, soundName);

        } catch (Exception e) {
            LOGGER.error("处理命令时出错", e);
        }
    }

    private void handleStopSoundCommand(String fullCommand) {
        try {
            LOGGER.info("检测到/stopsound命令，解析并停止声音与字幕");

            if (fullCommand.startsWith("/")) {
                fullCommand = fullCommand.substring(1);
            }

            String[] tokens = fullCommand.split("\\s+");

            String targetPlayer = tokens.length >= 2 ? tokens[1] : "";
            SoundSource source = SoundSource.MASTER;
            if (tokens.length >= 3) {
                try {
                    source = SoundSource.valueOf(tokens[2].toUpperCase());
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("无效的声音来源: {}", tokens[2]);
                }
            }
            String soundIdStr = tokens.length >= 4 ? tokens[3] : null;
            ResourceLocation soundLocation = soundIdStr != null ? ResourceLocation.tryParse(soundIdStr) : null;

            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                LOGGER.warn("无法获取本地玩家");
                return;
            }

            if (isForCurrentPlayer(targetPlayer, player.getName().getString())) {
                LOGGER.info("停止声音命令针对当前玩家，停止所有字幕播放");

                if (soundLocation != null) {
                    stopSpecificPlayback(soundLocation);
                } else {
                    stopAllPlaybacks();
                }
            } else {
                LOGGER.debug("停止声音命令不针对当前玩家，跳过处理");
            }
        } catch (Exception e) {
            LOGGER.error("处理/stopsound命令时出错", e);
        }
    }

    // 停止特定声音的字幕和音频
    private static void stopSpecificPlayback(ResourceLocation soundId) {
        PlaybackInfo info = activePlaybacks.get(soundId);
        if (info != null) {
            LOGGER.info("停止特定播放: {}", soundId);

            // 停止音频
            if (info.soundInstance != null) {
                Minecraft.getInstance().getSoundManager().stop(info.soundInstance);
            }

            // 停止字幕
            SubtitlePlayer.stop(soundId);

            // 移除跟踪
            activePlaybacks.remove(soundId);
        } else {
            LOGGER.debug("找不到特定播放: {}", soundId);
        }
    }

    // 停止所有播放（字幕和音频）
    public static void stopAllPlaybacks() {
        LOGGER.info("停止所有音频和字幕播放");

        // 停止所有音频
        for (PlaybackInfo info : activePlaybacks.values()) {
            if (info.soundInstance != null) {
                Minecraft.getInstance().getSoundManager().stop(info.soundInstance);
            }
        }

        // 停止所有字幕
        SubtitlePlayer.stop(true);

        // 清空跟踪
        activePlaybacks.clear();
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

        if (target == null || target.isEmpty() ||
                target.equals("@s") || target.equals("@p") || target.equals("@a")) {
            return true;
        }

        String[] targets = target.split(",");
        for (String t : targets) {
            t = t.trim();
            if (t.equalsIgnoreCase(currentPlayerName)) {
                return true;
            }
        }
        return false;
    }

    public static void playSoundAndSubtitle(ResourceLocation soundId, String soundName) {
        LOGGER.info("在客户端播放声音和字幕: {}", soundId);

        // 如果已有相同的播放，先停止
        if (activePlaybacks.containsKey(soundId)) {
            LOGGER.info("声音已存在，先停止: {}", soundId);
            stopSpecificPlayback(soundId);
        }

        try {
            SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundId);
            if (soundEvent == null) {
                LOGGER.error("无法创建声音事件: {}", soundId);
                return;
            }

            SimpleSoundInstance soundInstance = SimpleSoundInstance.forUI(
                    soundEvent,
                    1.0f,
                    1.0f
            );

            // 播放音频
            Minecraft.getInstance().getSoundManager().play(soundInstance);

            // 保存播放信息
            PlaybackInfo info = new PlaybackInfo(soundId, soundName, soundInstance);
            activePlaybacks.put(soundId, info);

        } catch (Exception e) {
            LOGGER.error("播放声音失败", e);
        }

        // 播放字幕
        playSubtitleFile(soundName);
    }
}