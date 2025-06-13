package com.zhenghao123.easysubtitles;

import com.zhenghao123.easysubtitles.config.ConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.phys.Vec3;
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

            // 检测命令格式（同时支持带斜杠和不带斜杠）
            if (!fullCommand.startsWith("/playsound ") && !fullCommand.startsWith("playsound ")) {
                LOGGER.debug("非/playsound命令，跳过处理");
                return;
            }

            // 标准化命令：移除前导斜杠
            if (fullCommand.startsWith("/")) {
                fullCommand = fullCommand.substring(1);
            }

            // 分割命令参数
            String[] parts = fullCommand.split("\\s+", 5);

            // 确保有足够的参数
            if (parts.length < 4) {
                LOGGER.warn("无效的/playsound命令格式: {} (参数不足)", fullCommand);
                return;
            }

            ResourceLocation soundId = ResourceLocation.tryParse(parts[1]);
            if (soundId == null) {
                LOGGER.warn("无法解析声音ID: {}", parts[1]);
                return;
            }
            LOGGER.info("声音ID: namespace={}, path={}", soundId.getNamespace(), soundId.getPath());

            // 检查是否属于字幕命名空间
            if (!ConfigHandler.SUBTITLE_NAMESPACE.equals(soundId.getNamespace())) {
                LOGGER.debug("跳过非字幕声音: {}", soundId);
                return;
            }

            // 检查路径前缀
            if (!soundId.getPath().startsWith(SOUND_PREFIX)) {
                LOGGER.warn("声音ID缺少字幕前缀: {}", soundId);
                return;
            }

            // 提取声音名称
            String soundName = soundId.getPath().substring(SOUND_PREFIX.length());
            LOGGER.info("提取声音名称: {}", soundName);

            // 阻止原始命令的执行（这样就不会显示提示信息）
            event.setCanceled(true);
            LOGGER.info("已取消原始命令执行");

            // 获取本地玩家
            LocalPlayer player = Minecraft.getInstance().player;
            if (player == null) {
                LOGGER.warn("无法获取本地玩家");
                return;
            }

            // 解析目标玩家
            String target = parts[3];

            // 检查是否是针对当前玩家的命令
            if (!isForCurrentPlayer(target, player.getName().getString())) {
                LOGGER.debug("目标玩家 {} 不是当前玩家，跳过处理", target);
                return;
            }

            // 播放声音（模拟原始命令）
            playSoundLocally(soundId, player);

            // 播放字幕
            playSubtitleFile(soundName);

        } catch (Exception e) {
            LOGGER.error("处理命令时出错", e);
        }
    }

    /**
     * 检查目标玩家是否包括当前玩家
     */
    private boolean isForCurrentPlayer(String target, String currentPlayerName) {
        // 单人游戏总是返回true
        if (Minecraft.getInstance().isSingleplayer()) {
            return true;
        }

        // 多目标处理
        String[] targets = target.split(",");
        for (String t : targets) {
            t = t.trim();
            if (t.equals("@a") ||
                    t.equals("@s") ||
                    t.equals("@p") ||
                    t.equals("*") ||
                    t.equalsIgnoreCase(currentPlayerName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 在客户端模拟播放声音
     */
    private void playSoundLocally(ResourceLocation soundId, LocalPlayer player) {
        LOGGER.info("在客户端播放声音: {}", soundId);

        try {
            // 创建声音事件（使用固定音量和音高）
            SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundId);

            // 获取玩家位置
            Vec3 pos = player.position();

            // 在玩家位置播放声音
            player.clientLevel.playSound(
                    player,
                    pos.x, pos.y, pos.z,
                    soundEvent,
                    SoundSource.MASTER, // 使用主声音通道
                    1.0f,     // 音量
                    1.0f      // 音高
            );

        } catch (Exception e) {
            LOGGER.error("播放声音失败", e);
        }
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

        LOGGER.warn("无法找到字幕文件: {}", soundName);
        Minecraft.getInstance().gui.getChat().addMessage(
                Component.literal("未找到字幕文件: " + soundName)
        );
    }

    private void playSubtitle(File subFile) {
        LOGGER.info("播放字幕: {}", subFile.getName());
        SubtitlePlayer.play(subFile);
    }
}