package com.zhenghao123.easysubtitles;

import com.zhenghao123.easysubtitles.config.ConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
public class SubtitleRenderer {
    private static String currentSubtitle = "";
    private static final Logger LOGGER = LogManager.getLogger();
    private static long displayUntil = 0;
    private static ResourceLocation backgroundTexture;
    private static boolean textureLoaded = false;

    // 暂停状态管理
    private static boolean isPaused = false;
    private static long remainingTimeOnPause = 0;

    // 多行字幕处理
    private static final List<FormattedCharSequence> subtitleLines = new ArrayList<>();
    private static double lastUpdateTime = 0;
    private static String lastText = "";

    public SubtitleRenderer() {
        LOGGER.info("字幕渲染器初始化");
    }

    public static void showSubtitle(String text, long duration) {
        if (Minecraft.getInstance() == null) {
            LOGGER.error("尝试在未初始化时显示字幕!");
            return;
        }

        Minecraft.getInstance().execute(() -> {
            LOGGER.debug("设置字幕显示: '{}' 持续 {}ms", text, duration);
            currentSubtitle = text;
            displayUntil = System.currentTimeMillis() + duration;
            isPaused = false; // 重置暂停状态

            // 清除旧的行
            subtitleLines.clear();
        });
    }

    public static void clearSubtitle() {
        currentSubtitle = "";
        displayUntil = 0;
        isPaused = false;
        subtitleLines.clear();
        LOGGER.debug("清除当前字幕");
    }

    // 暂停方法
    public static void pause() {
        if (isPaused) return;

        isPaused = true;
        long currentTime = System.currentTimeMillis();
        if (displayUntil > currentTime) {
            remainingTimeOnPause = displayUntil - currentTime;
        } else {
            remainingTimeOnPause = 0;
        }

        LOGGER.debug("渲染器暂停 - 剩余时间: {}ms", remainingTimeOnPause);
    }

    // 恢复方法
    public static void resume() {
        if (!isPaused) return;

        isPaused = false;
        if (remainingTimeOnPause > 0) {
            displayUntil = System.currentTimeMillis() + remainingTimeOnPause;
            LOGGER.debug("渲染器恢复 - 剩余时间: {}ms", remainingTimeOnPause);
        } else {
            clearSubtitle();
            LOGGER.debug("渲染器恢复 - 无剩余时间，清除字幕");
        }
    }

    public static void loadBackgroundTexture() {
        try {
            String path = ConfigHandler.BG_IMAGE_PATH.get();
            backgroundTexture = new ResourceLocation(path);
            LOGGER.debug("尝试加载背景纹理: {}", path);

            if (Minecraft.getInstance().getResourceManager().getResource(backgroundTexture).isEmpty()) {
                LOGGER.error("字幕背景资源不存在: {}", path);
                backgroundTexture = null;
            }
        } catch (Exception e) {
            LOGGER.error("字幕背景加载失败", e);
            backgroundTexture = null;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CHAT_PANEL.type()) {
            return;
        }

        // 双重检查字幕是否应该消失
        if (!isPaused && displayUntil > 0 && System.currentTimeMillis() > displayUntil) {
            clearSubtitle();
            return;
        }

        boolean shouldRender = !currentSubtitle.isEmpty() &&
                (isPaused || System.currentTimeMillis() <= displayUntil);

        if (!shouldRender) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        GuiGraphics gui = event.getGuiGraphics();
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();

        // 更新字幕行（根据配置）
        updateSubtitleLines(font);

        // 计算字幕位置（根据配置）
        int[] position = calculatePosition(screenWidth, screenHeight, font);
        int x = position[0];
        int y = position[1];

        // 渲染背景
        renderBackground(gui, screenWidth, screenHeight, font, x, y);

        // 渲染字幕文本
        renderText(gui, font, x, y);
    }

    private static void updateSubtitleLines(Font font) {
        // 如果字幕文本没有改变，不需要更新
        if (currentSubtitle.equals(lastText)) {
            return;
        }

        // 清除旧行
        subtitleLines.clear();

        // 根据配置的最大宽度进行文本分割
        int maxWidth = ConfigHandler.MAX_WIDTH.get();
        Component formattedText = Component.literal(currentSubtitle);

        // 直接使用 font.split() 返回的 FormattedCharSequence 列表
        subtitleLines.addAll(font.split(formattedText, maxWidth));

        lastText = currentSubtitle;
    }

    private static int[] calculatePosition(int screenWidth, int screenHeight, Font font) {
        // 使用配置的位置预设
        ConfigHandler.PositionPreset position = ConfigHandler.POSITION_PRESET.get();

        // 计算字幕总高度（包括行间距）
        int totalHeight = font.lineHeight * subtitleLines.size() + (subtitleLines.size() - 1) * 2;
        int maxLineWidth = getMaxLineWidth(font);

        int xPos = 0;
        int yPos = 0;

        // 默认行为（底部居中）
        if (position == ConfigHandler.PositionPreset.BOTTOM_CENTER) {
            // 保留原始位置计算逻辑
            xPos = (screenWidth - maxLineWidth) / 2;
            yPos = screenHeight - totalHeight - 50;
        }
        // 其他预设位置
        else {
            // 根据配置计算位置...
        }

        // 应用整体缩放
        double scale = ConfigHandler.SCALE.get();
        xPos = (int) (xPos * scale);
        yPos = (int) (yPos * scale);

        return new int[]{xPos, yPos};
    }

    private static int getMaxLineWidth(Font font) {
        int maxWidth = 0;
        for (FormattedCharSequence line : subtitleLines) {
            int lineWidth = font.width(line);
            if (lineWidth > maxWidth) {
                maxWidth = lineWidth;
            }
        }
        return maxWidth;
    }

    private static void renderBackground(GuiGraphics gui, int screenWidth, int screenHeight, Font font, int x, int y) {
        // 如果不显示背景，直接返回
        if (!ConfigHandler.SHOW_BACKGROUND.get()) {
            return;
        }

        // 如果没有行，不需要渲染背景
        if (subtitleLines.isEmpty()) {
            return;
        }

        int maxWidth = getMaxLineWidth(font);
        int totalHeight = font.lineHeight * subtitleLines.size() + (subtitleLines.size() - 1) * 2;

        // 保留原始纯色背景渲染逻辑
        int padding = 5;
        int alpha = 128; // 0x80 = 50% 不透明度

        gui.fill(
                x - padding,
                y - padding,
                x + maxWidth + padding,
                y + totalHeight + padding,
                (alpha << 24) | 0x000000
        );
    }

    private static void renderText(GuiGraphics gui, Font font, int x, int y) {
        // 保留原始文字渲染逻辑
        int textColor = 0xFFFFFF; // 白色
        boolean shadow = true; // 启用阴影
        int lineHeight = font.lineHeight + 2;

        for (int i = 0; i < subtitleLines.size(); i++) {
            FormattedCharSequence line = subtitleLines.get(i);
            int lineY = y + i * lineHeight;

            gui.drawString(
                    font,
                    line,
                    x,
                    lineY,
                    textColor,
                    shadow
            );
        }
    }

    // 获取当前字幕文本
    public static String getCurrentSubtitle() {
        return currentSubtitle;
    }

    // 获取显示结束时间
    public static long getDisplayUntil() {
        return displayUntil;
    }
}