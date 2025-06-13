package com.zhenghao123.easysubtitles;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

public class CommandHandler {
    public static final File SUB_DIR = new File(
            Minecraft.getInstance().gameDirectory,
            "config/easysubtitles"
    );


    private static final Logger LOGGER = LogManager.getLogger();

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!SUB_DIR.exists()) {

            LOGGER.info("创建字幕目录: {}", SUB_DIR.getAbsolutePath());
            SUB_DIR.mkdirs();
        }

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
                            SubtitlePlayer.stop();
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("已停止播放"),
                                    true
                            );
                            return 1;
                        })
                )
                .then(Commands.literal("debug")
                        .executes(ctx -> {
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("调试信息:")
                                            .append("\n当前字幕文件: " + (SubtitlePlayer.getCurrentFile() != null ?
                                                    SubtitlePlayer.getCurrentFile().getName() : "无"))
                                            .append("\n播放状态: " + (SubtitlePlayer.isPlaying() ? "播放中" : "未播放")),
                                    true
                            );
                            return 1;
                        })
                )
        );

        LOGGER.info("/easysub命令注册完成");
    }

    private static void playSubtitleFile(CommandSourceStack source, String fileName) {

        LOGGER.info("尝试播放字幕文件: {} (路径: {})", fileName, SUB_DIR.getAbsolutePath());

        File file = new File(SUB_DIR, fileName + ".srt");


        LOGGER.info("查找文件路径: {}", file.getAbsolutePath());

        if (!file.exists()) {

            LOGGER.warn("文件不存在: {}", file.getAbsolutePath());
            source.sendFailure(
                    Component.literal("文件不存在: " + file.getAbsolutePath())
            );
            return;
        }

        SubtitlePlayer.play(file);
        source.sendSuccess(
                () -> Component.literal("播放字幕: " + fileName),
                true
        );

        // 确认文件被加载
        LOGGER.info("字幕文件加载成功");
    }
}