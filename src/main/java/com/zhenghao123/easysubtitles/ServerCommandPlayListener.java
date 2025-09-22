package com.zhenghao123.easysubtitles;

import com.zhenghao123.easysubtitles.config.ConfigHandler;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
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

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.function.BooleanSupplier;

@Mod.EventBusSubscriber
public class ServerCommandPlayListener {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String SOUND_PREFIX = "subtitles.sound.";
    private static final BooleanSupplier LOG_SOUND_COMMANDS = () -> false; // 默认关闭日志

    @SubscribeEvent
    public static void onCommand(CommandEvent event) {
        if (event.getParseResults().getContext().getSource().getServer() == null) {
            return;
        }

        try {
            String fullCommand = event.getParseResults().getReader().getString().trim();

            // 跳过空命令
            if (fullCommand.isEmpty()) {
                return;
            }

            // 精确匹配播放声音命令
            if (isValidPlaySoundCommand(fullCommand)) {
                if (LOG_SOUND_COMMANDS.getAsBoolean()) {
                    LOGGER.info("服务器捕获播放声音命令: {}", fullCommand);
                }
                handlePlaySoundCommand(event, fullCommand);
            }
            // 精确匹配停止声音命令
            else if (isValidStopSoundCommand(fullCommand)) {
                if (LOG_SOUND_COMMANDS.getAsBoolean()) {
                    LOGGER.info("服务器捕获停止声音命令: {}", fullCommand);
                }
                handleStopSoundCommand(event, fullCommand);
            }
        } catch (Exception e) {
            LOGGER.error("处理命令时出错", e);
        }
    }

    // 精确验证播放声音命令格式
    private static boolean isValidPlaySoundCommand(String command) {
        // 格式: /playsound <sound> <source> <targets> [x] [y] [z] [volume] [pitch] [minVolume]
        return command.matches("^/?playsound\\s+\\S+\\s+\\S+.*");
    }

    // 精确验证停止声音命令格式
    private static boolean isValidStopSoundCommand(String command) {
        // 格式: /stopsound <targets> [source] [sound]
        return command.matches("^/?stopsound\\s+\\S+(\\s+\\S+)?(\\s+\\S+)?");
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

            if (LOG_SOUND_COMMANDS.getAsBoolean()) {
                LOGGER.info("声音ID: namespace={}, path={}", soundId.getNamespace(), soundId.getPath());
            }

            if (!ConfigHandler.SUBTITLE_NAMESPACE.equals(soundId.getNamespace())) {
                if (LOG_SOUND_COMMANDS.getAsBoolean()) {
                    LOGGER.debug("跳过非字幕声音: {}", soundId);
                }
                return;
            }

            if (!soundId.getPath().startsWith(SOUND_PREFIX)) {
                LOGGER.warn("声音ID缺少字幕前缀: {}", soundId);
                return;
            }

            String soundName = soundId.getPath().substring(SOUND_PREFIX.length());
            if (LOG_SOUND_COMMANDS.getAsBoolean()) {
                LOGGER.info("提取声音名称: {}", soundName);
            }

            CommandSourceStack source = event.getParseResults().getContext().getSource();
            String target = parts[3];

            // 获取目标玩家列表
            Collection<ServerPlayer> targetPlayers = getTargetPlayers(target, source);

            if (targetPlayers.isEmpty()) {
                LOGGER.warn("未找到匹配的目标玩家: {}", target);
                return;
            }

            for (ServerPlayer player : targetPlayers) {
                if (LOG_SOUND_COMMANDS.getAsBoolean()) {
                    LOGGER.info("发送字幕给玩家: {}", player.getScoreboardName());
                }

                // 只发送网络包，不在服务器端播放声音
                EasySubtitlesMod.NETWORK_CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new PlaySubtitlePacket(soundName)
                );
            }

            event.setCanceled(true);
            if (LOG_SOUND_COMMANDS.getAsBoolean()) {
                LOGGER.info("已处理自定义声音命令");
            }

        } catch (Exception e) {
            LOGGER.error("处理命令时出错", e);
        }
    }

    private static void handleStopSoundCommand(CommandEvent event, String fullCommand) {
        try {
            if (fullCommand.startsWith("/")) {
                fullCommand = fullCommand.substring(1);
            }

            String[] tokens = fullCommand.split("\\s+");

            // 验证命令格式
            if (tokens.length < 2) {
                LOGGER.warn("无效的/stopsound命令格式: {}", fullCommand);
                return;
            }

            CommandSourceStack source = event.getParseResults().getContext().getSource();
            String targetPlayer = tokens.length >= 2 ? tokens[1] : "";
            SoundSource sourceCategory = SoundSource.MASTER;
            if (tokens.length >= 3) {
                try {
                    sourceCategory = SoundSource.valueOf(tokens[2].toUpperCase());
                } catch (IllegalArgumentException e) {
                    LOGGER.warn("无效的声音来源: {}", tokens[2]);
                }
            }
            String soundId = tokens.length >= 4 ? tokens[3] : null;
            ResourceLocation soundLocation = soundId != null ? ResourceLocation.tryParse(soundId) : null;

            // 获取目标玩家列表
            Collection<ServerPlayer> targetPlayers = getTargetPlayers(targetPlayer, source);

            for (ServerPlayer player : targetPlayers) {
                EasySubtitlesMod.NETWORK_CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new StopSubtitlePacket(soundLocation, sourceCategory)
                );
            }

            event.setCanceled(true);

        } catch (Exception e) {
            LOGGER.error("处理/stopsound命令时出错", e);
        }
    }

    // 获取目标玩家列表
    private static Collection<ServerPlayer> getTargetPlayers(String target, CommandSourceStack source) {
        try {
            // 处理特殊选择器
            if (target.equals("@a")) {
                return ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers();
            }

            if (target.equals("@p")) {
                // 获取最近的玩家
                ServerPlayer nearestPlayer = source.getPlayer();
                if (nearestPlayer != null) {
                    return Collections.singletonList(nearestPlayer);
                }
                return Collections.emptyList();
            }

            if (target.equals("@s")) {
                // 获取命令执行者
                if (source.getEntity() instanceof ServerPlayer) {
                    return Collections.singletonList((ServerPlayer) source.getEntity());
                }
                return Collections.emptyList();
            }

            // 处理玩家名称列表
            String[] playerNames = target.split(",");
            List<ServerPlayer> players = new java.util.ArrayList<>();

            for (String name : playerNames) {
                name = name.trim();
                ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayerByName(name);
                if (player != null) {
                    players.add(player);
                }
            }

            return players;
        } catch (Exception e) {
            LOGGER.warn("无法解析目标选择器: {}", target, e);
            return Collections.emptyList();
        }
    }

}