package com.zhenghao123.easysubtitles;

import com.zhenghao123.easysubtitles.config.ConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@OnlyIn(Dist.CLIENT)
public class CommandPlayListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SOUND_PREFIX = "subtitles.sound.";

    @SubscribeEvent
    public void onCommand(CommandEvent event) {
        if (!ConfigHandler.AUTO_SUBTITLE.get()) return;

        try {
            String fullCommand = event.getParseResults().getReader().getString().trim();
            LOGGER.info("捕获命令: {}", fullCommand);

            // 修复命令检测：同时支持带斜杠和不带斜杠的命令格式
            if (!fullCommand.startsWith("/playsound ") &&
                    !fullCommand.startsWith("playsound ")) {
                LOGGER.debug("非/playsound命令，跳过处理");
                return;
            }

            // 标准化命令：移除前导斜杠
            if (fullCommand.startsWith("/")) {
                fullCommand = fullCommand.substring(1);
            }

            // 分割命令参数（参数前4个）
            String[] parts = fullCommand.split("\\s+", 5); // 只分割最多5部分

            // 确保有足够的参数
            if (parts.length < 4) {
                LOGGER.warn("无效的/playsound命令格式: {} (参数不足)", fullCommand);
                return;
            }

            // 解析声音ID
            ResourceLocation soundId = ResourceLocation.tryParse(parts[1]);
            if (soundId == null) {
                LOGGER.warn("无法解析声音ID: {}", parts[1]);
                return;
            }
            LOGGER.info("声音ID: namespace={}, path={}", soundId.getNamespace(), soundId.getPath());

            // 检查命名空间
            if (!ConfigHandler.SUBTITLE_NAMESPACE.equals(soundId.getNamespace())) {
                LOGGER.debug("跳过非字幕声音: {} (命名空间不匹配)", soundId);
                return;
            }

            // 检查路径前缀
            if (!soundId.getPath().startsWith(SOUND_PREFIX)) {
                LOGGER.warn("声音ID缺少字幕前缀: {}, 应为: {}", soundId, SOUND_PREFIX);
                return;
            }

            // 提取声音名称
            String soundName = soundId.getPath().substring(SOUND_PREFIX.length());
            LOGGER.info("提取声音名称: {}", soundName);

            // 解析目标玩家
            String target = parts[3];
            String localPlayer = Minecraft.getInstance().player != null ?
                    Minecraft.getInstance().player.getName().getString() : "unknown";

            LOGGER.info("当前玩家: {}, 命令目标: {}", localPlayer, target);

            // 检查目标是否为当前玩家
            if (isCurrentPlayerTarget(target, localPlayer)) {
                LOGGER.info("准备为玩家 {} 播放字幕", localPlayer);
                // 确保在主线程播放字幕
                Minecraft.getInstance().submit(() -> playSubtitleFile(soundName));
            } else {
                LOGGER.debug("目标玩家 {} 不是当前玩家，跳过字幕显示", target);
            }
        } catch (Exception e) {
            LOGGER.error("处理命令时出错", e);
        }
    }

    /**
     * 检查命令目标是否包含当前玩家
     */
    private boolean isCurrentPlayerTarget(String target, String currentPlayer) {
        // 单人游戏总是显示
        if (Minecraft.getInstance().isSingleplayer()) return true;

        // 多目标处理
        String[] targets = target.split(",");
        for (String t : targets) {
            t = t.trim();
            if (t.equals("@a") ||
                    t.equals("@s") ||
                    t.equals("@p") ||
                    t.equals("*") ||
                    t.equalsIgnoreCase(currentPlayer)) {
                return true;
            }
        }
        return false;
    }

    private void playSubtitleFile(String soundName) {
        LOGGER.info("查找字幕文件: {}", soundName);

        // 尝试原始文件名
        File subFile = new File(CommandHandler.SUB_DIR, soundName + ".srt");
        if (subFile.exists() && subFile.isFile()) {
            LOGGER.info("找到字幕文件: {}", subFile.getName());
            playSubtitle(subFile);
            return;
        }

        // 尝试安全文件名
        String safeName = soundName.replace(':', '_').replace('/', '_');
        subFile = new File(CommandHandler.SUB_DIR, safeName + ".srt");
        if (subFile.exists() && subFile.isFile()) {
            LOGGER.info("找到安全名称字幕文件: {}", subFile.getName());
            playSubtitle(subFile);
            return;
        }

        // 尝试无后缀名
        subFile = new File(CommandHandler.SUB_DIR, safeName);
        if (subFile.exists() && subFile.isFile()) {
            LOGGER.info("找到无后缀字幕文件: {}", subFile.getName());
            playSubtitle(subFile);
            return;
        }

        LOGGER.warn("无法找到字幕文件: {}", subFile.getAbsolutePath());
        Minecraft.getInstance().gui.getChat().addMessage(
                Component.literal("未找到字幕文件: " + soundName)
        );
    }

    private void playSubtitle(File subFile) {
        LOGGER.info("播放字幕: {}", subFile.getName());
        SubtitlePlayer.play(subFile);
    }
}