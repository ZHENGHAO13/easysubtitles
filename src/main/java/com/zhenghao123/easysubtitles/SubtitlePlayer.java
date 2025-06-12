package com.zhenghao123.easysubtitles;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import java.io.File;
import java.util.*;
import java.util.concurrent.*;

public class SubtitlePlayer {
    private static List<SRTParser.Subtitle> currentSubtitles;
    private static ScheduledExecutorService scheduler;

    public static void play(File srtFile) {
        stop();
        try {
            List<SRTParser.Subtitle> subs = SRTParser.parse(srtFile);
            if (subs.isEmpty()) {
                Minecraft.getInstance().gui.getChat().addMessage(
                        Component.literal("字幕文件无有效内容")
                );
                return;
            }
            currentSubtitles = subs;
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
        long baseTime = System.currentTimeMillis();

        for (SRTParser.Subtitle sub : currentSubtitles) {
            long delay = sub.getStartMs();
            long duration = sub.getEndMs() - sub.getStartMs();

            scheduler.schedule(() -> {
                Minecraft.getInstance().execute(() -> {
                    SubtitleRenderer.showSubtitle(sub.getText(), duration);
                });
            }, delay, TimeUnit.MILLISECONDS);
        }
    }
}