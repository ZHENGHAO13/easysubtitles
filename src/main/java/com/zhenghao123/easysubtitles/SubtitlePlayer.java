package com.zhenghao123.easysubtitles;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
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

    private static final AtomicLong playbackStartTime = new AtomicLong(0);
    private static final AtomicLong displayUntil = new AtomicLong(0);

    private static boolean firstSubtitleScheduled = false;

    // 新增：暂停状态管理
    private static boolean isPaused = false;
    private static long pauseStartTime = 0;

    public static void play(File srtFile) {
        if (Minecraft.getInstance() == null) return;

        LOGGER.info("播放字幕文件: {}", srtFile.getName());

        resetState();

        currentFile = srtFile;

        try {
            List<SRTParser.Subtitle> subs = SRTParser.parse(srtFile);
            if (subs == null || subs.isEmpty()) {
                LOGGER.warn("字幕文件无有效内容: {}", srtFile.getName());
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
        }
    }

    private static void resetState() {
        isPaused = false;
        pauseStartTime = 0;
        displayUntil.set(0);
        stop(false);
    }

    public static void stop(boolean log) {
        if (log) {
            LOGGER.info("停止当前字幕播放");
        }

        // 确保如果处于暂停状态，停止时恢复声音管理器，以免声音引擎卡在暂停状态
        if (isPaused) {
            Minecraft.getInstance().getSoundManager().resume();
            isPaused = false;
            pauseStartTime = 0;
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
        firstSubtitleScheduled = false;

        playbackStartTime.set(0);
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

    // 新增：暂停方法
    public static void pause() {
        if (!isPlaying() || isPaused) return;

        isPaused = true;
        pauseStartTime = System.currentTimeMillis();

        // 停止调度器，暂停字幕更新
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }

        // 暂停声音引擎
        Minecraft.getInstance().getSoundManager().pause();
        LOGGER.info("音频和字幕播放已暂停");
    }

    // 新增：恢复方法
    public static void resume() {
        if (!isPaused) return;

        long now = System.currentTimeMillis();
        long pauseDuration = now - pauseStartTime;

        // 调整开始时间，补偿暂停的时长
        playbackStartTime.addAndGet(pauseDuration);

        // 如果当前有正在显示的字幕，延长它的显示结束时间
        if (displayUntil.get() > 0) {
            displayUntil.addAndGet(pauseDuration);
        }

        isPaused = false;
        pauseStartTime = 0;

        // 恢复声音引擎
        Minecraft.getInstance().getSoundManager().resume();

        // 重新调度剩余字幕
        scheduleRemainingSubtitles();
        LOGGER.info("音频和字幕播放已恢复");
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
        // 如果我们刚刚恢复播放，需要检查是否应该立即显示第一句（处理暂停在第一句之前的情况）
        long adjustedStart = playbackStartTime.get() + firstSubtitle.getStartMs();
        long currentTime = System.currentTimeMillis();

        if (firstSubtitle.getStartMs() == 0 || currentTime >= adjustedStart) {
            // 逻辑由 scheduleRemainingSubtitles 处理，这里主要标记
        }

        // 原有逻辑保持兼容
        if (firstSubtitle.getStartMs() == 0 && !isPaused) {
            // ...
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
        long currentTime = System.currentTimeMillis();

        LOGGER.debug("重新调度/开始播放: startTime={}ms", startTimestamp);

        int subtitleIndex = 0; // 重新扫描所有字幕以确保正确恢复

        for (int i = subtitleIndex; i < currentSubtitles.size(); i++) {
            final SRTParser.Subtitle sub = currentSubtitles.get(i);

            long adjustedStart = startTimestamp + sub.getStartMs();
            long adjustedDelay = adjustedStart - currentTime;

            long duration = sub.getEndMs() - sub.getStartMs();

            // 如果这一句已经应该开始播放了
            if (adjustedDelay < 0) {
                long timePassed = currentTime - adjustedStart;
                long adjustedDuration = duration - timePassed;

                // 如果这一句还没结束（或者是暂停时正在显示的那句）
                if (adjustedDuration > 50) {
                    LOGGER.debug("恢复/显示字幕: '{}' 剩余: {}ms", sub.getText(), adjustedDuration);
                    // 立即在主线程显示
                    long finalDuration = adjustedDuration;
                    Minecraft.getInstance().execute(() -> {
                        SubtitleRenderer.showSubtitle(sub.getText(), finalDuration);
                        displayUntil.set(System.currentTimeMillis() + finalDuration);
                    });

                    // 标记第一句已处理，防止重复
                    if (i == 0) firstSubtitleScheduled = true;
                }
                continue;
            }

            if (i == 0 && !firstSubtitleScheduled) {
                firstSubtitleScheduled = true;
            }

            scheduler.schedule(() -> {
                LOGGER.debug("显示字幕: '{}' 持续: {}ms", sub.getText(), duration);
                Minecraft.getInstance().execute(() -> {
                    SubtitleRenderer.showSubtitle(sub.getText(), duration);
                    displayUntil.set(System.currentTimeMillis() + duration);
                });
            }, adjustedDelay, TimeUnit.MILLISECONDS);
        }
    }

    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (isPaused) return; // 暂停时不处理超时清除

        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) {
            return;
        }

        // 检查字幕是否超时
        if (displayUntil.get() > 0 && System.currentTimeMillis() > displayUntil.get()) {
            LOGGER.debug("字幕超时，清除显示");
            SubtitleRenderer.clearSubtitle();
            displayUntil.set(0);
        }
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
        displayUntil.set(0);
        firstSubtitleScheduled = false;
        isPaused = false;
        pauseStartTime = 0;
    }

    public static long getDisplayUntil() {
        return displayUntil.get();
    }

    @SubscribeEvent
    public static void onPlaySound(PlaySoundEvent event) {
        // 原有音乐屏蔽逻辑
        if (event.getSound() != null &&
                event.getSound().getSource() == SoundSource.MUSIC &&
                System.currentTimeMillis() < MusicController.getMuteUntil()) {
            event.setCanceled(true);
        }
    }
}