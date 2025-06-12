package com.zhenghao123.easysubtitles;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import java.io.File;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class SubtitlePlayer {
    private static List<SRTParser.Subtitle> currentSubtitles;
    private static ScheduledExecutorService scheduler;

    public static void play(File srtFile) {
        stop(); // 停止当前播放

        try {
            List<SRTParser.Subtitle> subtitles = SRTParser.parse(srtFile);
            if (subtitles.isEmpty()) {
                Minecraft.getInstance().gui.getChat().addMessage(
                        Component.literal("字幕文件无有效内容")
                );
                return;
            }

            currentSubtitles = subtitles;
            startPlayback();
        } catch (Exception e) {
            Minecraft.getInstance().gui.getChat().addMessage(
                    Component.literal("字幕加载失败: " + e.getMessage())
            );
        }
    }

    public static void stop() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        SubtitleRenderer.showSubtitle("", 0);
    }

    private static void startPlayback() {
        scheduler = Executors.newSingleThreadScheduledExecutor();
        final long startTime = System.currentTimeMillis();

        for (SRTParser.Subtitle sub : currentSubtitles) {
            // 计算相对于播放开始的延迟
            long delay = sub.getStartMs();
            long duration = sub.getEndMs() - sub.getStartMs();

            scheduler.schedule(() -> {
                SubtitleRenderer.showSubtitle(sub.getText(), duration);
            }, delay, TimeUnit.MILLISECONDS);
        }
    }
}