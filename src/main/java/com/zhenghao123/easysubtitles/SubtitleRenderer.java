package com.zhenghao123.easysubtitles;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraftforge.client.event.RenderGuiOverlayEvent;
import net.minecraftforge.client.gui.overlay.VanillaGuiOverlay;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public class SubtitleRenderer {
    private static String currentSubtitle = "";
    private static long displayUntil = 0;
    private static int yOffset = -40; // 避免被聊天框遮挡

    public static void showSubtitle(String text, long durationMs) {
        Minecraft.getInstance().execute(() -> { // 确保主线程操作
            currentSubtitle = text;
            displayUntil = System.currentTimeMillis() + durationMs;
        });
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public void onRenderOverlay(RenderGuiOverlayEvent.Post event) {
        if (event.getOverlay() != VanillaGuiOverlay.CHAT_PANEL.type()) return;
        if (System.currentTimeMillis() > displayUntil || currentSubtitle.isEmpty()) return;

        GuiGraphics guiGraphics = event.getGuiGraphics();
        int screenWidth = event.getWindow().getGuiScaledWidth();
        int screenHeight = event.getWindow().getGuiScaledHeight();

        // 计算渲染位置（底部居中 + 偏移）
        int textWidth = Minecraft.getInstance().font.width(currentSubtitle);
        int x = (screenWidth - textWidth) / 2;
        int y = screenHeight + yOffset; // 从底部向上偏移

        // 绘制半透明背景
        int padding = 5;
        int bgWidth = textWidth + padding * 2;
        int bgHeight = Minecraft.getInstance().font.lineHeight + padding * 2;
        guiGraphics.fill(
                x - padding, y - padding,
                x + textWidth + padding, y + bgHeight,
                0x80000000 // 黑色半透明
        );

        // 渲染支持颜色代码的字幕
        Component component = Component.literal(currentSubtitle);
        guiGraphics.drawString(
                Minecraft.getInstance().font,
                component, x, y, 0xFFFFFF, false
        );
    }
}