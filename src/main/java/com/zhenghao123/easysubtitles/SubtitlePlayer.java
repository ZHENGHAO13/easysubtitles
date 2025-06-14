package com.zhenghao123.easysubtitles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Mod.EventBusSubscriber
public class SubtitlePlayer {
    private static List<SRTParser.Subtitle> currentSubtitles;
    private static ScheduledExecutorService scheduler;
    private static File currentFile;

    private static final Logger LOGGER = LogManager.getLogger();

    // 添加暂停状态管理
    private static boolean isPaused = false;

    // 关键修复：使用原子长整型保证跨线程安全
    private static final AtomicLong totalPauseDuration = new AtomicLong(0);
    private static final AtomicLong lastPauseStart = new AtomicLong(0);
    private static final AtomicLong playbackStartTime = new AtomicLong(0);

    // 新添加：记录第一句字幕状态
    private static boolean firstSubtitleScheduled = false;

    public static void play(File srtFile) {
        LOGGER.info("播放字幕文件: {}", srtFile.getName());

        // 重置所有状态
        resetState();

        currentFile = srtFile;

        try {
            List<SRTParser.Subtitle> subs = SRTParser.parse(srtFile);
            if (subs == null || subs.isEmpty()) {
                LOGGER.warn("字幕文件无有效内容: {}", srtFile.getName());
                Minecraft.getInstance().gui.getChat().addMessage(
                        Component.literal("字幕文件无有效内容或格式错误: " + srtFile.getName())
                );
                return;
            }

            LOGGER.info("解析到 {} 个字幕", subs.size());
            currentSubtitles = subs;
            playbackStartTime.set(System.currentTimeMillis());
            firstSubtitleScheduled = false; // 重置第一句字幕状态

            // 立即显示第一句字幕（如果开始时间为0）
            scheduleFirstSubtitleIfNeeded();

            // 调度后续字幕
            scheduleRemainingSubtitles();
        } catch (Exception e) {
            LOGGER.error("字幕加载失败: {} - {}", srtFile.getName(), e.getMessage(), e);
            Minecraft.getInstance().gui.getChat().addMessage(
                    Component.literal("字幕加载失败: " + e.getMessage() + "\n文件: " + srtFile.getName())
            );
        }
    }

    private static void resetState() {
        isPaused = false;
        totalPauseDuration.set(0);
        lastPauseStart.set(0);
        stop();
    }

    public static void stop() {
        LOGGER.info("停止当前字幕播放");
        if (scheduler != null) {
            scheduler.shutdownNow();
            try {
                if (!scheduler.awaitTermination(500, TimeUnit.MILLISECONDS)) {
                    LOGGER.warn("字幕调度器未能在500毫秒内终止");
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            scheduler = null;
        }
        SubtitleRenderer.clearSubtitle(); // 确保清除字幕
        currentSubtitles = null;
        currentFile = null;
        isPaused = false;
        firstSubtitleScheduled = false;
    }

    public static File getCurrentFile() {
        return currentFile;
    }

    public static boolean isPlaying() {
        return currentSubtitles != null && !currentSubtitles.isEmpty();
    }

    // 新增暂停播放方法
    public static void pausePlayback() {
        if (!isPlaying() || isPaused) return;

        LOGGER.debug("字幕播放器暂停");
        isPaused = true;
        lastPauseStart.set(System.currentTimeMillis());
        SubtitleRenderer.pause();

        // 暂停调度器
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
    }

    // 新增恢复播放方法
    public static void resumePlayback() {
        if (!isPlaying() || !isPaused) return;

        LOGGER.debug("字幕播放器恢复");
        long pauseDuration = System.currentTimeMillis() - lastPauseStart.get();
        totalPauseDuration.addAndGet(pauseDuration);

        isPaused = false;

        // 修复：立即显示当前应该显示的字幕
        showCurrentActiveSubtitle();

        // 重新调度剩余字幕
        scheduleRemainingSubtitles();

        SubtitleRenderer.resume();
    }

    // 新添加：确保显示当前活跃的字幕
    private static void showCurrentActiveSubtitle() {
        if (currentSubtitles == null || currentSubtitles.isEmpty()) return;

        long currentTime = System.currentTimeMillis();
        long adjustedTime = currentTime - playbackStartTime.get() - totalPauseDuration.get();

        // 查找当前应该显示的字幕
        for (SRTParser.Subtitle sub : currentSubtitles) {
            // 修复：使用getStartMs()和getEndMs()
            if (adjustedTime >= sub.getStartMs() && adjustedTime <= sub.getEndMs()) {
                // 计算剩余显示时间
                long remainingTime = sub.getEndMs() - adjustedTime;
                LOGGER.debug("显示当前活跃字幕: '{}' 剩余: {}ms", sub.getText(), remainingTime);
                SubtitleRenderer.showSubtitle(sub.getText(), remainingTime);
                break;
            }
        }
    }

    // 新添加：单独处理第一句字幕
    private static void scheduleFirstSubtitleIfNeeded() {
        if (currentSubtitles == null || currentSubtitles.isEmpty() || firstSubtitleScheduled)
            return;

        SRTParser.Subtitle firstSubtitle = currentSubtitles.get(0);
        if (firstSubtitle.getStartMs() == 0) {
            // 修复：计算持续时间
            long duration = firstSubtitle.getEndMs() - firstSubtitle.getStartMs();
            LOGGER.debug("立即显示第一句字幕: '{}' 持续: {}ms",
                    firstSubtitle.getText(), duration);
            SubtitleRenderer.showSubtitle(firstSubtitle.getText(), duration);
            firstSubtitleScheduled = true;
        }
    }

    // 修复：重构字幕调度方法
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
        long offset = totalPauseDuration.get();
        long currentTime = System.currentTimeMillis();

        LOGGER.debug("开始播放: offset={}ms, startTime={}ms", offset, startTimestamp);

        int subtitleIndex = firstSubtitleScheduled ? 1 : 0;

        for (int i = subtitleIndex; i < currentSubtitles.size(); i++) {
            final SRTParser.Subtitle sub = currentSubtitles.get(i);

            // 修复：更精确的时间计算
            long adjustedStart = startTimestamp + offset + sub.getStartMs();
            long adjustedDelay = adjustedStart - currentTime;

            // 修复：计算持续时间
            long duration = sub.getEndMs() - sub.getStartMs();

            // 如果字幕已经过期，直接显示或跳过
            if (adjustedDelay < 0) {
                long timePassed = currentTime - adjustedStart;
                long adjustedDuration = duration - timePassed;

                if (adjustedDuration > 50) { // 大于50ms才显示
                    LOGGER.debug("显示滞后字幕: '{}' 剩余: {}ms", sub.getText(), adjustedDuration);
                    SubtitleRenderer.showSubtitle(sub.getText(), adjustedDuration);
                }
                continue;
            }

            // 特殊处理：如果这是第一个字幕且还没有显示（在正常播放情况下）
            if (i == 0 && !firstSubtitleScheduled) {
                LOGGER.debug("调度第一句字幕: '{}' 延迟: {}ms", sub.getText(), adjustedDelay);
                firstSubtitleScheduled = true;
            }

            // 调度字幕
            scheduler.schedule(() -> {
                if (!isPaused) {
                    LOGGER.debug("显示字幕: '{}' 持续: {}ms", sub.getText(), duration);
                    Minecraft.getInstance().execute(() -> {
                        SubtitleRenderer.showSubtitle(sub.getText(), duration);
                    });
                }
            }, adjustedDelay, TimeUnit.MILLISECONDS);

            LOGGER.trace("调度字幕: '{}' 延迟: {}ms", sub.getText(), adjustedDelay);
        }

        LOGGER.info("已调度 {} 个字幕播放", currentSubtitles.size() - subtitleIndex);
    }

    // 使用兼容的事件监听器
    @SubscribeEvent
    public static void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;

        // 检查游戏是否加载完成
        if (Minecraft.getInstance().level == null || Minecraft.getInstance().player == null) {
            return;
        }

        // 获取当前屏幕状态
        boolean isPauseScreen = Minecraft.getInstance().screen instanceof PauseScreen;

        // 处理暂停菜单逻辑
        if (isPauseScreen && isPlaying()) {
            // 首次检测到暂停菜单打开
            if (!isPaused) {
                LOGGER.debug("检测到暂停菜单打开，暂停字幕");
                pausePlayback();
            }
        } else if (isPlaying()) {
            // 恢复时 - 只在有暂停状态时才恢复
            if (isPaused) {
                LOGGER.debug("检测到暂停菜单关闭，恢复字幕");
                resumePlayback();
            }
        }
    }

    // 新添加：当玩家退出世界时完全重置
    @SubscribeEvent
    public static void onClientDisconnect(ClientPlayerNetworkEvent.LoggingOut event) {
        LOGGER.info("玩家退出世界，重置所有字幕状态");
        resetOnWorldExit();
    }

    // 新添加：重置方法
    public static void resetOnWorldExit() {
        LOGGER.info("完全重置字幕播放器");
        stop(); // 正常停止播放
        playbackStartTime.set(0); // 重置播放起始时间
        totalPauseDuration.set(0); // 重置暂停时间
        lastPauseStart.set(0); // 重置最后暂停时间
        firstSubtitleScheduled = false; // 重置第一句字幕状态
    }
}