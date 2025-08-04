package com.zhenghao123.easysubtitles;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class CommandHandler {
    private static final Logger LOGGER = LogManager.getLogger();

    // 修改为 public static 方法，以便从其他类访问
    public static void ensureSubtitleDirectoryExists() {
        File subDir = getSubDir();
        if (!subDir.exists()) {
            LOGGER.info("创建字幕目录: {}", subDir.getAbsolutePath());
            if (subDir.mkdirs()) {
                LOGGER.info("目录创建成功");
            } else {
                LOGGER.error("目录创建失败: {}", subDir.getAbsolutePath());
            }
        }
    }

    public static File getSubDir() {
        return FMLPaths.CONFIGDIR.get().resolve("easysubtitles").toFile();
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // 确保目录存在
        ensureSubtitleDirectoryExists();

        LOGGER.info("注册/easysub命令");
        dispatcher.register(Commands.literal("easysub")
                .then(Commands.argument("filename", StringArgumentType.string())
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "filename");
                            playSubtitleFile(ctx.getSource(), name);
                            return 1;
                        })
                )
                .then(Commands.literal("stop")
                        .executes(ctx -> {
                            if (FMLEnvironment.dist == Dist.CLIENT) {
                                SubtitlePlayer.stop();
                            }
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("已停止播放"),
                                    true
                            );
                            return 1;
                        })
                )
                .then(Commands.literal("debug")
                        .executes(ctx -> {
                            // 创建临时final变量
                            final String currentFileStr;
                            final String playingStatusStr;

                            if (FMLEnvironment.dist == Dist.CLIENT) {
                                File file = SubtitlePlayer.getCurrentFile();
                                currentFileStr = (file != null) ? file.getName() : "无";
                                playingStatusStr = SubtitlePlayer.isPlaying() ? "播放中" : "未播放";
                            } else {
                                currentFileStr = "无 (仅客户端)";
                                playingStatusStr = "未播放 (仅客户端)";
                            }

                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("调试信息:")
                                            .append("\n当前字幕文件: " + currentFileStr)
                                            .append("\n播放状态: " + playingStatusStr),
                                    true
                            );
                            return 1;
                        })
                )
        );

        LOGGER.info("/easysub命令注册完成");
    }

    private static void playSubtitleFile(CommandSourceStack source, String fileName) {
        LOGGER.info("尝试播放字幕文件: {} (路径: {})", fileName, getSubDir().getAbsolutePath());

        File file = new File(getSubDir(), fileName + ".srt");
        LOGGER.info("查找文件路径: {}", file.getAbsolutePath());

        if (!file.exists()) {
            LOGGER.warn("文件不存在: {}", file.getAbsolutePath());
            // 移除发送给玩家的提示
            // source.sendFailure(Component.literal("文件不存在: " + file.getAbsolutePath()));
            return;
        }

        // 如果是服务器，则发送数据包给客户端
        if (!source.getLevel().isClientSide) {
            // 获取触发命令的玩家（命令执行者）
            if (source.getEntity() instanceof ServerPlayer player) {
                // 只向触发命令的玩家发送数据包
                EasySubtitlesMod.NETWORK_CHANNEL.send(
                        net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                        new PlaySubtitlePacket(fileName)
                );
            } else {
                // 如果不是玩家（比如控制台），则可以选择不发送或发送给所有玩家（根据需求）
                LOGGER.warn("命令执行者不是玩家，无法播放字幕");
            }
        } else {
            // 客户端直接播放
            CommandPlayListener.playSubtitleFile(fileName);
        }

        source.sendSuccess(
                () -> Component.literal("播放字幕: " + fileName),
                true
        );
        LOGGER.info("字幕文件加载成功");
    }
}