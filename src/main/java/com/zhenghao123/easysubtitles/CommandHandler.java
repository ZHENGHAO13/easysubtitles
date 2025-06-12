package com.zhenghao123.easysubtitles;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import java.io.File;

public class CommandHandler {
    private static final File SUB_DIR = new File(
            Minecraft.getInstance().gameDirectory,
            "config/easysubtitles"
    );

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        if (!SUB_DIR.exists()) SUB_DIR.mkdirs();

        dispatcher.register(Commands.literal("easysub")
                .then(Commands.argument("filename", StringArgumentType.string())
                        .executes(ctx -> {
                            String name = StringArgumentType.getString(ctx, "filename");
                            File file = new File(SUB_DIR, name + ".srt");

                            if (!file.exists()) {
                                ctx.getSource().sendFailure(
                                        Component.literal("文件不存在: " + file.getAbsolutePath())
                                );
                                return 0;
                            }

                            SubtitlePlayer.play(file);
                            ctx.getSource().sendSuccess(
                                    () -> Component.literal("播放字幕: " + name),
                                    true
                            );
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
        );
    }
}