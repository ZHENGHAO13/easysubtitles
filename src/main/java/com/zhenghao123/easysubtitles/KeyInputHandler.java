package com.zhenghao123.easysubtitles;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = EasySubtitlesMod.MODID, value = Dist.CLIENT)
public class KeyInputHandler {

    // 默认按键为 DELETE
    public static final KeyMapping STOP_SOUND_KEY = new KeyMapping(
            "key.easysubtitles.stopsound",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_DELETE,
            "category.easysubtitles"
    );

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        if (STOP_SOUND_KEY.consumeClick()) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                // 1. 停止所有声音 (Master频道)
                mc.getSoundManager().stop(null, SoundSource.MASTER);

                // 2. 停止所有正在运行的字幕任务 (解决你提到的“字留”问题)
                SubtitlePlayer.stop(true);

                // 3. 立即强制清空屏幕上的渲染内容
                SubtitleRenderer.clearSubtitle();

                // 提示玩家
                mc.player.displayClientMessage(
                        Component.literal("§c[EasySubtitles] 已强行停止所有声音与字幕"),
                        true
                );
            }
        }
    }
}