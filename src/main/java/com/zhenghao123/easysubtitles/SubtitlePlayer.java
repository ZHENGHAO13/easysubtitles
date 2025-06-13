package com.zhenghao123.easysubtitles;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger; // 确保导入正确的 Log4j Logger

import java.io.File;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SubtitlePlayer {
    private static List<SRTParser.Subtitle> currentSubtitles;
    private static ScheduledExecutorService scheduler;
    private static File currentFile;

    // 使用正确的 Log4j Logger
    private static final Logger LOGGER = LogManager.getLogger();

    public static void play(File srtFile) {
        if (LOGGER != null) {
            LOGGER.info("播放字幕文件: {}", srtFile.getName());
        }

        // 停止当前播放
        stop();

        // 更新当前文件
        currentFile = srtFile;

        try {
            List<SRTParser.Subtitle> subs = SRTParser.parse(srtFile);
            if (subs == null || subs.isEmpty()) {
                if (LOGGER != null) {
                    LOGGER.warn("字幕文件无有效内容: {}", srtFile.getName());
                }
                Minecraft.getInstance().gui.getChat().addMessage(
                        Component.literal("字幕文件无有效内容或格式错误: " + srtFile.getName())
                );
                return;
            }

            if (LOGGER != null) {
                LOGGER.info("解析到 {} 个字幕", subs.size());
            }
            currentSubtitles = subs;
            startPlayback();
        } catch (Exception e) {
            if (LOGGER != null) {
                LOGGER.error("字幕加载失败: {} - {}", srtFile.getName(), e.getMessage(), e);
            }
            Minecraft.getInstance().gui.getChat().addMessage(
                    Component.literal("字幕加载失败: " + e.getMessage() + "\n文件: " + srtFile.getName())
            );
        }
    }

    public static void stop() {
        if (LOGGER != null) {
            LOGGER.info("停止当前字幕播放");
        }
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
            if (LOGGER != null) {
                LOGGER.debug("字幕调度器已停止");
            }
        }
        SubtitleRenderer.showSubtitle("", 0);
        currentSubtitles = null;
        currentFile = null;
    }

    public static File getCurrentFile() {
        return currentFile;
    }

    public static boolean isPlaying() {
        return scheduler != null && !scheduler.isTerminated();
    }

    private static void startPlayback() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        long baseTime = System.currentTimeMillis();
        int count = 0;

        for (SRTParser.Subtitle sub : currentSubtitles) {
            count++;
            long delay = sub.getStartMs();
            long duration = sub.getEndMs() - sub.getStartMs();

            if (LOGGER != null && LOGGER.isTraceEnabled()) {
                LOGGER.trace("调度字幕 {}: '{}' 延迟: {}ms 持续: {}ms",
                        count, sub.getText(), delay, duration);
            }

            scheduler.schedule(() -> {
                if (LOGGER != null) {
                    LOGGER.debug("显示字幕: '{}'", sub.getText());
                }
                Minecraft.getInstance().execute(() -> {
                    SubtitleRenderer.showSubtitle(sub.getText(), duration);
                });
            }, delay, TimeUnit.MILLISECONDS);
        }

        if (LOGGER != null) {
            LOGGER.info("已调度 {} 个字幕播放", currentSubtitles.size());
        }
    }
}