package com.zhenghao123.easysubtitles;

import com.zhenghao123.easysubtitles.config.ConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@OnlyIn(Dist.CLIENT)
public class SubtitleRenderer {
    private static final Logger LOGGER = LogManager.getLogger();
    private static String currentSubtitle = "";
    private static long displayUntil = 0;
    private static ResourceLocation backgroundTexture;
    private static boolean textureLoaded = false;

    // 暂停状态管理
    private static boolean isPaused = false;
    private static long remainingTimeOnPause = 0;

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
        });
    }

    public static void clearSubtitle() {
        currentSubtitle = "";
        displayUntil = 0;
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

    private static void loadBackgroundTexture() {
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

        boolean shouldRender = !currentSubtitle.isEmpty() &&
                (isPaused || System.currentTimeMillis() <= displayUntil);

        if (!shouldRender) {
            return;
        }

        GuiGraphics gui = event.getGuiGraphics();
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();
        int textWidth = Minecraft.getInstance().font.width(currentSubtitle);
        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight - 70; // 比聊天框稍高

        renderBackground(gui, x, y, textWidth);

        gui.drawString(
                Minecraft.getInstance().font,
                currentSubtitle, x, y, 0xFFFFFF, true
        );
    }

    private void renderBackground(GuiGraphics gui, int x, int y, int textWidth) {
        int padding = 5;
        int bgHeight = Minecraft.getInstance().font.lineHeight + padding * 2;

        if (ConfigHandler.USE_IMAGE_BG.get()) {
            if (!textureLoaded) {
                loadBackgroundTexture();
                textureLoaded = true;
            }
            if (backgroundTexture != null) {
                int scaledWidth = (int) ((textWidth + padding * 2) * ConfigHandler.BG_SCALE.get());
                int scaledHeight = (int) (bgHeight * ConfigHandler.BG_SCALE.get());
                int offsetX = x - padding - (scaledWidth - textWidth - padding * 2) / 2;
                int offsetY = y - padding - (scaledHeight - bgHeight) / 2;

                gui.blit(
                        backgroundTexture,
                        offsetX, offsetY,
                        0, 0,
                        scaledWidth, scaledHeight,
                        scaledWidth, scaledHeight
                );
                return;
            }
        }

        gui.fill(
                x - padding, y - padding,
                x + textWidth + padding, y + bgHeight,
                0x80000000
        );
    }
}