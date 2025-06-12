package com.zhenghao123.easysubtitles;

import com.zhenghao123.easysubtitles.config.ConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager; // 新增日志导入

public class SubtitleRenderer {
    private static String currentSubtitle = "";
    private static long displayUntil = 0;
    private static ResourceLocation backgroundTexture;
    private static boolean textureLoaded = false;

    public static void showSubtitle(String text, long duration) {
        Minecraft.getInstance().execute(() -> {
            currentSubtitle = text;
            displayUntil = System.currentTimeMillis() + duration;
        });
    }

    private static void loadBackgroundTexture() {
        try {
            String path = ConfigHandler.BG_IMAGE_PATH.get();
            backgroundTexture = new ResourceLocation(path);
            // 使用Log4j日志系统替代错误写法
            if (!Minecraft.getInstance().getResourceManager()
                    .getResource(backgroundTexture).isPresent()) {
                LogManager.getLogger().error("字幕背景资源不存在: {}", path);
            }
        } catch (Exception e) {
            // 使用标准Log4j日志记录器
            LogManager.getLogger().error("字幕背景加载失败", e);
            backgroundTexture = null;
        }
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CHAT_PANEL.type()) return;
        if (System.currentTimeMillis() > displayUntil || currentSubtitle.isEmpty()) return;

        GuiGraphics gui = event.getGuiGraphics();
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();
        int textWidth = Minecraft.getInstance().font.width(currentSubtitle);
        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight - 40;

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
        // 纯色背景回退
        gui.fill(
                x - padding, y - padding,
                x + textWidth + padding, y + bgHeight,
                0x80000000
        );
    }
}