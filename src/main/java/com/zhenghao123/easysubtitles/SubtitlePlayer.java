package com.zhenghao123.easysubtitles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.client.event.sound.PlaySoundEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class SubtitlePlayer {
    private static List<SRTParser.Subtitle> currentSubtitles;
    private static ScheduledExecutorService scheduler;
    private static File currentFile;

    private static final Logger LOGGER = LogManager.getLogger();

    // 已移除暂停功能
    // private static boolean isPaused = false;
    // private static final AtomicLong totalPauseDuration = new AtomicLong(0);
    // private static final AtomicLong lastPauseStart = new AtomicLong(0);
    private static final AtomicLong playbackStartTime = new AtomicLong(0);
    private static final AtomicLong displayUntil = new AtomicLong(0);

    private static boolean firstSubtitleScheduled = false;

    public static void play(File srtFile) {
        if (Minecraft.getInstance() == null) return;

        LOGGER.info("播放字幕文件: {}", srtFile.getName());

        resetState();

        currentFile = srtFile;

        try {
            List<SRTParser.Subtitle> subs = SRTParser.parse(srtFile);
            if (subs == null || subs.isEmpty()) {
                LOGGER.warn("字幕文件无有效内容: {}", srtFile.getName());
                // 移除发送给玩家的提示
                // Minecraft.getInstance().gui.getChat().addMessage(
                //     Component.literal("字幕文件无有效内容或格式错误: " + srtFile.getName())
                // );
                return;
            }

            LOGGER.info("解析到 {} 个字", subs.size());
            currentSubtitles = subs;
            playbackStartTime.set(System.currentTimeMillis());
            firstSubtitleScheduled = false;
            displayUntil.set(0);

            scheduleFirstSubtitleIfNeeded();
            scheduleRemainingSubtitles();
        } catch (Exception e) {
            LOGGER.error("字幕加载失败: {} - {}", srtFile.getName(), e.getMessage(), e);
            // 移除发送给玩家的提示
            // Minecraft.getInstance().gui.getChat().addMessage(
            //     Component.literal("字幕加载失败: " + e.getMessage() + "\n文件: " + srtFile.getName())
            // );
        }
    }

    private static void resetState() {
        // isPaused = false;
        // totalPauseDuration.set(0);
        // lastPauseStart.set(0);
        displayUntil.set(0);
        stop(false);
    }

    public static void stop(boolean log) {
        if (log) {
            LOGGER.info("停止当前字幕播放");
        }

        if (scheduler != null) {
            scheduler.shutdownNow();
            try {
                if (!scheduler.awaitTermination(500, TimeUnit.MILLISECONDS) && log) {
                    LOGGER.warn("字幕调度器未能在500毫秒内终止");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            scheduler = null;
        }
        SubtitleRenderer.clearSubtitle();
        currentSubtitles = null;
        currentFile = null;
        // isPaused = false;
        firstSubtitleScheduled = false;

        playbackStartTime.set(0);
        // totalPauseDuration.set(0);
        // lastPauseStart.set(0);
        displayUntil.set(0);
    }

    public static void stop(ResourceLocation soundId) {
        if (soundId == null) {
            stop(true);
            return;
        }

        String fileNamePrefix = soundId.getPath().replace("subtitles.sound.", "");

        if (currentFile != null && currentFile.getName().startsWith(fileNamePrefix)) {
            LOGGER.info("停止特定字幕: {}", fileNamePrefix);
            stop(true);
        } else {
            LOGGER.debug("当前字幕与停止请求不匹配: {} vs {}",
                    currentFile != null ? currentFile.getName() : "null",
                    fileNamePrefix);
        }
    }

    public static void stop() {
        stop(true);
    }

    public static File getCurrentFile() {
        return currentFile;
    }

    public static boolean isPlaying() {
        return currentSubtitles != null && !currentSubtitles.isEmpty();
    }


    private static void showCurrentActiveSubtitle() {
        if (currentSubtitles == null || currentSubtitles.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        long adjustedTime = currentTime - playbackStartTime.get();
        // - totalPauseDuration.get(); // 移除暂停功能

        for (SRTParser.Subtitle sub : currentSubtitles) {
            if (adjustedTime >= sub.getStartMs() && adjustedTime <= sub.getEndMs()) {
                long remainingTime = sub.getEndMs() - adjustedTime;
                LOGGER.debug("显示当前活跃字幕: '{}' 剩余: {}ms", sub.getText(), remainingTime);
                SubtitleRenderer.showSubtitle(sub.getText(), remainingTime);
                displayUntil.set(System.currentTimeMillis() + remainingTime);
                break;
            }
        }
    }

    private static void scheduleFirstSubtitleIfNeeded() {
        if (currentSubtitles == null || currentSubtitles.isEmpty() || firstSubtitleScheduled)
            return;

        SRTParser.Subtitle firstSubtitle = currentSubtitles.get(0);
        if (firstSubtitle.getStartMs() == 0) {
            long duration = firstSubtitle.getEndMs() - firstSubtitle.getStartMs();
            LOGGER.debug("立即显示第一句字幕: '{}' 持续: {}ms",
                    firstSubtitle.getText(), duration);
            SubtitleRenderer.showSubtitle(firstSubtitle.getText(), duration);
            displayUntil.set(System.currentTimeMillis() + duration);
            firstSubtitleScheduled = true;
        }
    }

    private static void scheduleRemainingSubtitles() {
        if (scheduler != null) {
            scheduler.shutdownNow();
        }
        scheduler = Executors.newSingleThreadScheduledExecutor(
                r -> {
                    Thread t = new Thread(r, "Subtitle-Scheduler");
                    t.setDaemon(true);
                    t.setPriority(Thread.NORM_PRIORITY - 1);
                    return t;
                }
        );

        if (currentSubtitles == null) return;

        long startTimestamp = playbackStartTime.get();
        // long offset = totalPauseDuration.get(); // 移除暂停功能
        long currentTime = System.currentTimeMillis();

        LOGGER.debug("开始播放: startTime={}ms", startTimestamp);

        int subtitleIndex = firstSubtitleScheduled ? 1 : 0;

        for (int i = subtitleIndex; i < currentSubtitles.size(); i++) {
            final SRTParser.Subtitle sub = currentSubtitles.get(i);

            long adjustedStart = startTimestamp + sub.getStartMs(); // 移除暂停偏移
            long adjustedDelay = adjustedStart - currentTime;

            long duration = sub.getEndMs() - sub.getStartMs();

            if (adjustedDelay < 0) {
                long timePassed = currentTime - adjustedStart;
                long adjustedDuration = duration - timePassed;

                if (adjustedDuration > 50) {
                    LOGGER.debug("显示滞后字幕: '{}' 剩余: {}ms", sub.getText(), adjustedDuration);
                    SubtitleRenderer.showSubtitle(sub.getText(), adjustedDuration);
                    displayUntil.set(System.currentTimeMillis() + adjustedDuration);
                }
                continue;
            }

            if (i == 0 && !firstSubtitleScheduled) {
                LOGGER.debug("调度第一句字幕: '{}' 延迟: {}ms", sub.getText(), adjustedDelay);
                firstSubtitleScheduled = true;
            }

            scheduler.schedule(() -> {
                LOGGER.debug("显示字幕: '{}' 持续: {}ms", sub.getText(), duration);
                Minecraft.getInstance().execute(() -> {
                    SubtitleRenderer.showSubtitle(sub.getText(), duration);
                    displayUntil.set(System.currentTimeMillis() + duration);
                });
            }, adjustedDelay, TimeUnit.MILLISECONDS);

            LOGGER.trace("调度字幕: '{}' 延迟: {}ms", sub.getText(), adjustedDelay);
        }

        LOGGER.info("已调度 {} 个字幕播放", currentSubtitles.size() - subtitleIndex);
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) {
            return;
        }

        // 检查字幕是否超时
        if (displayUntil.get() > 0 && System.currentTimeMillis() > displayUntil.get()) {
            LOGGER.debug("字幕超时，清除显示");
            SubtitleRenderer.clearSubtitle();
            displayUntil.set(0);
        }

        // 已移除单人游戏暂停菜单检测
    }

    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        LOGGER.info("玩家退出世界，重置所有字幕状态");
        resetOnWorldExit();
    }

    public static void resetOnWorldExit() {
        LOGGER.info("完全重置字幕播放器");
        stop(false);
        playbackStartTime.set(0);
        // totalPauseDuration.set(0);
        // lastPauseStart.set(0);
        displayUntil.set(0);
        firstSubtitleScheduled = false;
    }

    public static long getDisplayUntil() {
        return displayUntil.get();
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        SoundInstance sound = event.getSound();
        if (sound == null) return;

        // 如果声音属于音乐类别，并且当前处于静音期，则取消播放
        if (sound.getSource() == SoundSource.MUSIC && System.currentTimeMillis() < MusicController.getMuteUntil()) {
            event.setCanceled(true);
        }
    }
}