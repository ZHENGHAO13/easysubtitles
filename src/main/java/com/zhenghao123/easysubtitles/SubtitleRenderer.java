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

    private static final List<FormattedCharSequence> subtitleLines = new ArrayList<>();
    private static String lastText = "";
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
            needsRedraw = true;

            subtitleLines.clear();
            lastText = "";
        });
    }

    public static void clearSubtitle() {
        currentSubtitle = "";
        displayStartedAt = 0;
        displayDuration = 0;
        subtitleLines.clear();
        lastText = "";
        needsRedraw = false;
        LOGGER.debug("清除当前字幕");
    }

    public static boolean shouldRender() {
        if (currentSubtitle.isEmpty()) return false;
        long remainingTime = getRemainingTime();
        return remainingTime > 0 || needsRedraw;
    }

    private static long getRemainingTime() {
        if (currentSubtitle.isEmpty()) return 0;
        if (displayStartedAt == 0) return 0;
        return displayDuration - (System.currentTimeMillis() - displayStartedAt);
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

        if (!shouldRender()) {
            if (!currentSubtitle.isEmpty() && !needsRedraw) {
                LOGGER.debug("字幕时间到期，自动清除: '{}'", currentSubtitle);
                clearSubtitle();
            }
            return;
        }

        long remainingTime = getRemainingTime();
        if (remainingTime <= 0) {
            LOGGER.debug("字幕时间到期，自动清除: '{}'", currentSubtitle);
            clearSubtitle();
            return;
        }

        if (needsRedraw) {
            needsRedraw = false;
        }

        Minecraft minecraft = Minecraft.getInstance();
        Font font = minecraft.font;
        GuiGraphics gui = event.getGuiGraphics();
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();

        updateSubtitleLines(font);

        int[] position = calculatePosition(screenWidth, screenHeight, font);
        int x = position[0];
        int y = position[1];

        renderBackground(gui, screenWidth, screenHeight, font, x, y);
        renderText(gui, font, x, y);
    }

    private static void updateSubtitleLines(Font font) {
        if (currentSubtitle.equals(lastText)) {
            return;
        }
        subtitleLines.clear();
        int maxWidth = ConfigHandler.MAX_WIDTH.get();
        Component formattedText = Component.literal(currentSubtitle);
        subtitleLines.addAll(font.split(formattedText, maxWidth));
        lastText = currentSubtitle;
    }

    private static int[] calculatePosition(int screenWidth, int screenHeight, Font font) {
        ConfigHandler.PositionPreset position = ConfigHandler.POSITION_PRESET.get();
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
            case CUSTOM:
                double xOffset = ConfigHandler.X_OFFSET.get();
                double yOffset = ConfigHandler.Y_OFFSET.get();
                xPos = (int) (xOffset * (screenWidth - maxLineWidth));
                yPos = (int) (yOffset * (screenHeight - totalHeight));
                break;
            default:
                xPos = (screenWidth - maxLineWidth) / 2;
                yPos = screenHeight - totalHeight - 50;
                break;
        }

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
        if (!ConfigHandler.SHOW_BACKGROUND.get()) {
            return;
        }
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

        if (backgroundTexture == null) {
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

    public static String getCurrentSubtitle() {
        return currentSubtitle;
    }

    public static long getRemainingTimeMs() {
        return getRemainingTime();
    }

    // 注意：原来的 onScreenOpen 事件监听已被移除，以避免与 CommandPlayListener 中的新逻辑冲突。
}