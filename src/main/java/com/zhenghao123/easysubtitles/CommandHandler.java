package com.zhenghao123.easysubtitles;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import java.io.File;

public class CommandHandler {
    private static final File SUBTITLE_DIR = new File(
            Minecraft.getInstance().gameDirectory, "config/easysubtitles"
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // 确保字幕目录存在
        if (!SUBTITLE_DIR.exists()) SUBTITLE_DIR.mkdirs();

        dispatcher.register(Commands.literal("easysub")
                .then(Commands.argument("filename", StringArgumentType.string())
                        .executes(context -> {
                            String filename = StringArgumentType.getString(context, "filename");
                            File srtFile = new File(SUBTITLE_DIR, filename + ".srt");

                            if (!srtFile.exists()) {
                                context.getSource().sendFailure(
                                        Component.literal("字幕文件不存在: " + srtFile.getPath())
                                );
                                context.getSource().sendFailure(
                                        Component.literal("请将.srt文件放入 config/easysubtitles/ 目录")
                                );
                                return 0;
                            }

                            SubtitlePlayer.play(srtFile);
                            context.getSource().sendSuccess(
                                    () -> Component.literal("播放字幕: " + filename),
                                    true
                            );
                            return 1;
                        })
                )
                .then(Commands.literal("stop")
                        .executes(context -> {
                            SubtitlePlayer.stop();
                            context.getSource().sendSuccess(
                                    () -> Component.literal("已停止字幕播放"),
                                    true
                            );
                            return 1;
                        })
                )
        );
    }
}