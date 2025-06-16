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
        if (event.getParseResults().getContext().getSource().getServer() == null) {
            return;
        }

        try {
            String fullCommand = event.getParseResults().getReader().getString().trim();
            LOGGER.info("服务器捕获命令: {}", fullCommand);

            if (fullCommand.startsWith("/playsound ") || fullCommand.startsWith("playsound ")) {
                handlePlaySoundCommand(event, fullCommand);
            }
        } catch (Exception e) {
            LOGGER.error("处理命令时出错", e);
        }
    }

    @SubscribeEvent
    public static void onStopSoundCommand(CommandEvent event) {
        if (event.getParseResults().getContext().getSource().getServer() == null) {
            return;
        }

        String fullCommand = event.getParseResults().getReader().getString().trim();
        LOGGER.info("服务器捕获停止声音命令: {}", fullCommand);

        if (!fullCommand.startsWith("/stopsound ") && !fullCommand.startsWith("stopsound ")) {
            return;
        }

        try {
            if (fullCommand.startsWith("/")) {
                fullCommand = fullCommand.substring(1);
            }

            String[] tokens = fullCommand.split("\\s+");

            String targetPlayer = tokens.length >= 2 ? tokens[1] : "";
            SoundSource source = SoundSource.MASTER;
            if (tokens.length >= 3) {
                try {
                    source = SoundSource.valueOf(tokens[2].toUpperCase());
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("无效的声音来源: {}", tokens[2]);
                }
            }
            String soundId = tokens.length >= 4 ? tokens[3] : null;
            ResourceLocation soundLocation = soundId != null ? ResourceLocation.tryParse(soundId) : null;

            for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                if (isForPlayer(targetPlayer, player.getScoreboardName())) {
                    EasySubtitlesMod.NETWORK_CHANNEL.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                            new StopSubtitlePacket(soundLocation, source)
                    );
                }
            }

            event.setCanceled(true);

        } catch (Exception e) {
            LOGGER.error("处理/stopsound命令时出错", e);
        }
    }

    private static void handlePlaySoundCommand(CommandEvent event, String fullCommand) {
        try {
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

            CommandSourceStack source = event.getParseResults().getContext().getSource();
            String target = parts[3];

            for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
                if (isForPlayer(target, player.getScoreboardName())) {
                    LOGGER.info("播放声音和发送字幕给玩家: {}", player.getScoreboardName());

                    playServerSound(soundId, player);

                    EasySubtitlesMod.NETWORK_CHANNEL.send(
                            net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                            new PlaySubtitlePacket(soundName)
                    );
                }
            }

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

            Level playerLevel = player.level();

            if (playerLevel == null) {
                LOGGER.error("玩家世界级别为空: {}", player.getName().getString());
                return;
            }

            playerLevel.playSound(
                    player,
                    player.getX(), player.getY(), player.getZ(),
                    soundEvent,
                    SoundSource.MASTER,
                    1.0f,
                    1.0f
            );
        } catch (Exception e) {
            LOGGER.error("在服务器播放声音失败", e);
        }
    }

    private static boolean isForPlayer(String target, String playerName) {
        if (target == null || target.isEmpty() || target.equals("@a")) {
            return true;
        }

        String[] targets = target.split(",");
        for (String t : targets) {
            t = t.trim();
            if (t.equals("@s") ||
                    t.equals("@p") ||
                    t.equals("*") ||
                    t.equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        return false;
    }
}