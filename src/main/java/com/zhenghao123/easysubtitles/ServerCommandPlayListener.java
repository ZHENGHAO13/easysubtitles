package com.zhenghao123.easysubtitles;

import com.zhenghao123.easysubtitles.config.ConfigHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraftforge.event.CommandEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod.EventBusSubscriber
public class ServerCommandPlayListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SOUND_PREFIX = "subtitles.sound.";

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        // 确保在服务器端运行
        if (event.getParseResults().getContext().getSource().getServer() == null) {
            return;
        }

        try {
            String fullCommand = event.getParseResults().getReader().getString().trim();
            LOGGER.info("服务器捕获命令: {}", fullCommand);

            if (!fullCommand.startsWith("/playsound ") && !fullCommand.startsWith("playsound ")) {
                return;
            }

            if (fullCommand.startsWith("/")) {
                fullCommand = fullCommand.substring(1);
            }

            String[] parts = fullCommand.split("\\s+", 5);

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

            if (!ConfigHandler.SUBTITLE_NAMESPACE.equals(soundId.getNamespace())) {
                LOGGER.debug("跳过非字幕声音: {}", soundId);
                return;
            }

            if (!soundId.getPath().startsWith(SOUND_PREFIX)) {
                LOGGER.warn("声音ID缺少字幕前缀: {}", soundId);
                return;
            }

            String soundName = soundId.getPath().substring(SOUND_PREFIX.length());
            LOGGER.info("提取声音名称: {}", soundName);

            // 使用正确的 CommandSourceStack 类型
            CommandSourceStack source = event.getParseResults().getContext().getSource();
            String target = parts[3];

            // 获取目标玩家
            for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                if (isForPlayer(target, player.getScoreboardName())) {
                    LOGGER.info("播放声音和发送字幕给玩家: {}", player.getScoreboardName());

                    // 在服务器上播放声音
                    playServerSound(soundId, player);

                    // 发送字幕包
                    EasySubtitlesMod.NETWORK_CHANNEL.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                            new PlaySubtitlePacket(soundName)
                    );
                }
            }

            // 成功处理命令后取消原始执行
            event.setCanceled(true);
            LOGGER.info("已处理自定义声音命令");

        } catch (Exception e) {
            LOGGER.error("处理命令时出错", e);
        }
    }

    private static void playServerSound(ResourceLocation soundId, ServerPlayer player) {
        try {
            SoundEvent soundEvent = SoundEvent.createVariableRangeEvent(soundId);
            if (soundEvent == null) {
                LOGGER.error("无法创建声音事件: {}", soundId);
                return;
            }

            // 使用 player.level() 而不是 player.serverLevel
            Level playerLevel = player.level();

            if (playerLevel == null) {
                LOGGER.error("玩家世界级别为空: {}", player.getName().getString());
                return;
            }

            // 在玩家世界播放声音
            playerLevel.playSound(
                    player, // 玩家
                    player.getX(), player.getY(), player.getZ(), // 位置
                    soundEvent, // 声音事件
                    SoundSource.MASTER, // 声音类别
                    1.0f, // 音量
                    1.0f  // 音调
            );
        } catch (Exception e) {
            LOGGER.error("在服务器播放声音失败", e);
        }
    }

    private static boolean isForPlayer(String target, String playerName) {
        String[] targets = target.split(",");
        for (String t : targets) {
            t = t.trim();
            if (t.equals("@a") ||
                    t.equals("@s") ||
                    t.equals("@p") ||
                    t.equals("*") ||
                    t.equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        return false;
    }
}