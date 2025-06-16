package com.zhenghao123.easysubtitles;

import com.zhenghao123.easysubtitles.config.ConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = EasySubtitlesMod.MODID, value = Dist.CLIENT)
public class SubtitleRenderer {
    private static String currentSubtitle = "";
    private static final Logger LOGGER = LogManager.getLogger();
    private static long displayStartedAt = 0;
    private static long displayDuration = 0;
    private static ResourceLocation backgroundTexture;
    private static boolean textureLoaded = false;

    // 暂停状态管理
    private static boolean isPaused = false;
    private static long pausedAt = 0;

    // 多行字幕处理
    private static final List<FormattedCharSequence> subtitleLines = new ArrayList<>();
    private static String lastText = "";

    // 强制重绘标志 - 确保暂停后恢复时立即渲染
    private static boolean needsRedraw = false;

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
            displayStartedAt = System.currentTimeMillis();
            displayDuration = duration;
            isPaused = false;
            needsRedraw = true; // 设置需要重绘标志

            // 清除旧的行
            subtitleLines.clear();
            lastText = "";
        });
    }

    public static void clearSubtitle() {
        currentSubtitle = "";
        displayStartedAt = 0;
        displayDuration = 0;
        isPaused = false;
        pausedAt = 0;
        subtitleLines.clear();
        lastText = "";
        needsRedraw = false;
        LOGGER.debug("清除当前字幕");
    }

    // 暂停方法
    public static void pause() {
        if (isPaused) return;
        if (currentSubtitle.isEmpty()) return; // 没有字幕时不处理

        LOGGER.debug("暂停字幕: '{}'", currentSubtitle);

        isPaused = true;
        pausedAt = System.currentTimeMillis();
        needsRedraw = true; // 需要重绘

        LOGGER.debug("字幕暂停，剩余时间: {}ms", getRemainingTime());
    }

    // 恢复方法
    public static void resume() {
        if (!isPaused) return;
        if (pausedAt == 0) return; // 从未暂停过

        long remainingTime = getRemainingTime();
        // 如果暂停时间已超过剩余时间，清除字幕
        if (remainingTime <= 0) {
            LOGGER.debug("字幕暂停时间超过剩余时间，清除字幕");
            clearSubtitle();
            return;
        }

        LOGGER.debug("恢复字幕: '{}'", currentSubtitle);
        LOGGER.debug("暂停持续时间: {}ms", (System.currentTimeMillis() - pausedAt));

        // 计算暂停期间消耗的时间
        long pausedDuration = System.currentTimeMillis() - pausedAt;

        // 调整开始时间，使得剩余时间不变
        displayStartedAt += pausedDuration;

        isPaused = false;
        pausedAt = 0;
        needsRedraw = true; // 需要重绘

        LOGGER.debug("字幕恢复，剩余时间: {}ms", getRemainingTime());
    }

    // 计算剩余播放时间
    private static long getRemainingTime() {
        if (currentSubtitle.isEmpty()) return 0;
        if (displayStartedAt == 0) return 0;

        if (isPaused) {
            return displayDuration - (pausedAt - displayStartedAt);
        }
        return displayDuration - (System.currentTimeMillis() - displayStartedAt);
    }

    // 获取渲染状态
    public static boolean shouldRender() {
        // 没有字幕内容不需要渲染
        if (currentSubtitle.isEmpty()) return false;

        // 计算剩余时间
        long remainingTime = getRemainingTime();

        // 需要渲染的情况：
        // 1. 剩余时间 > 0（字幕正在播放）
        // 2. 暂停状态（字幕需要保持显示）
        // 3. 需要强制重绘（状态变化后第一帧）
        return remainingTime > 0 || isPaused || needsRedraw;
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
    public static void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CHAT_PANEL.type()) {
            return;
        }

        // 判断是否需要渲染
        if (!shouldRender()) {
            // 如果有字幕内容但剩余时间为负且不强制重绘，清除字幕
            if (!currentSubtitle.isEmpty() && !needsRedraw) {
                LOGGER.debug("字幕时间到期，自动清除: '{}'", currentSubtitle);
                clearSubtitle();
            }
            return;
        }

        // 如果有字幕但剩余时间为负且不是暂停状态，清除字幕
        long remainingTime = getRemainingTime();
        if (!isPaused && remainingTime <= 0) {
            LOGGER.debug("字幕时间到期，自动清除: '{}'", currentSubtitle);
            clearSubtitle();
            return;
        }

        // 标记已经处理过当前强制重绘请求
        if (needsRedraw) {
            needsRedraw = false;
        }

        // 执行实际渲染
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

        // 如果字幕暂停，显示暂停指示器
        if (isPaused) {
            String pausedText = "ESC菜单暂停播放";
            int centerX = (screenWidth - font.width(pausedText)) / 2;
            int pauseY = y - font.lineHeight * 3;
            gui.drawString(font, pausedText, centerX, pauseY, 0xFFFF00, true);
        }
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

        int xPos;
        int yPos ;

        switch (position) {
            case TOP_LEFT:
                xPos = 10;
                yPos = 10;
                break;
            case TOP_CENTER:
                xPos = (screenWidth - maxLineWidth) / 2;
                yPos = 10;
                break;
            case TOP_RIGHT:
                xPos = screenWidth - maxLineWidth - 10;
                yPos = 10;
                break;
            case CENTER_LEFT:
                xPos = 10;
                yPos = (screenHeight - totalHeight) / 2;
                break;
            case CENTER:
                xPos = (screenWidth - maxLineWidth) / 2;
                yPos = (screenHeight - totalHeight) / 2;
                break;
            case CENTER_RIGHT:
                xPos = screenWidth - maxLineWidth - 10;
                yPos = (screenHeight - totalHeight) / 2;
                break;
            case BOTTOM_LEFT:
                xPos = 10;
                yPos = screenHeight - totalHeight - 30;
                break;
            case BOTTOM_CENTER:
                xPos = (screenWidth - maxLineWidth) / 2;
                yPos = screenHeight - totalHeight - 50;
                break;
            case BOTTOM_RIGHT:
                xPos = screenWidth - maxLineWidth - 10;
                yPos = screenHeight - totalHeight - 30;
                break;
            case CUSTOM: // 自定义位置
                double xOffset = ConfigHandler.X_OFFSET.get();
                double yOffset = ConfigHandler.Y_OFFSET.get();
                xPos = (int) (xOffset * (screenWidth - maxLineWidth));
                yPos = (int) (yOffset * (screenHeight - totalHeight));
                break;
            default: // 默认是BOTTOM_CENTER
                xPos = (screenWidth - maxLineWidth) / 2;
                yPos = screenHeight - totalHeight - 50;
                break;
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

        if (ConfigHandler.USE_IMAGE_BG.get()) {
            renderImageBackground(gui, maxWidth, totalHeight, x, y);
        } else {
            renderSolidBackground(gui, maxWidth, totalHeight, x, y);
        }
    }

    private static void renderImageBackground(GuiGraphics gui, int textWidth, int textHeight, int x, int y) {
        if (!textureLoaded) {
            loadBackgroundTexture();
            textureLoaded = true;
        }

        // 如果无法加载图片背景，回退到纯色背景
        if (backgroundTexture == null) {
            LOGGER.warn("背景纹理不可用，回退到纯色背景");
            renderSolidBackground(gui, textWidth, textHeight, x, y);
            return;
        }

        int padding = 5;
        double scale = ConfigHandler.BG_SCALE.get();
        int scaledWidth = (int) ((textWidth + padding * 2) * scale);
        int scaledHeight = (int) ((textHeight + padding * 2) * scale);
        int offsetX = x - padding - (scaledWidth - textWidth - padding * 2) / 2;
        int offsetY = y - padding - (scaledHeight - textHeight) / 2;

        gui.blit(
                backgroundTexture,
                offsetX, offsetY,
                0, 0,
                scaledWidth, scaledHeight,
                scaledWidth, scaledHeight
        );
    }

    private static void renderSolidBackground(GuiGraphics gui, int textWidth, int textHeight, int x, int y) {
        int padding = 5;
        float opacity = (float) (double) ConfigHandler.BACKGROUND_OPACITY.get();

        // 计算背景色 (0xRRGGBBAA)
        int alpha = (int) (opacity * 255);
        int backgroundColor = (alpha << 24) | 0x000000;

        gui.fill(
                x - padding,
                y - padding,
                x + textWidth + padding,
                y + textHeight + padding,
                backgroundColor
        );
    }

    private static void renderText(GuiGraphics gui, Font font, int x, int y) {
        int textColor = ConfigHandler.TEXT_COLOR.get();
        boolean shadow = ConfigHandler.ENABLE_TEXT_SHADOW.get();
        int lineHeight = font.lineHeight + 2;

        // 应用字体高度缩放
        float fontSize = (float) ConfigHandler.FONT_HEIGHT.get();
        if (fontSize > 0) {
            float scaleFactor = fontSize / font.lineHeight;
            gui.pose().pushPose();
            gui.pose().scale(scaleFactor, scaleFactor, scaleFactor);
            x = (int) (x / scaleFactor);
            y = (int) (y / scaleFactor);
        }

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

        if (fontSize > 0) {
            gui.pose().popPose();
        }
    }

    // 获取当前字幕文本
    public static String getCurrentSubtitle() {
        return currentSubtitle;
    }

    // 获取剩余时间（毫秒）
    public static long getRemainingTimeMs() {
        return getRemainingTime();
    }

    // 获取是否暂停状态
    public static boolean isPaused() {
        return isPaused;
    }

    // 自动暂停与恢复逻辑 - 集成到游戏中
    @SubscribeEvent
    public static void onScreenOpen(ScreenEvent.Opening event) {
        if (event.getScreen().isPauseScreen()) {
            LOGGER.debug("检测到ESC菜单打开");
            pause(); // 暂停当前字幕
        }
    }

    @SubscribeEvent
    public static void onScreenClose(ScreenEvent.Closing event) {
        if (event.getScreen().isPauseScreen()) {
            LOGGER.debug("检测到ESC菜单关闭");
            resume(); // 恢复当前字幕
        }
    }
}