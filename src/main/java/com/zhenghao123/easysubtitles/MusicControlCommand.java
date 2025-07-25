package com.zhenghao123.easysubtitles;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

public class MusicControlCommand {

    private static final SimpleCommandExceptionType INVALID_DURATION =
            new SimpleCommandExceptionType(Component.literal("时长必须在 1-3600 秒之间"));

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("stopmusic")
                .requires(source -> source.hasPermission(2)) // 需要OP权限
                .then(Commands.argument("duration", IntegerArgumentType.integer(1, 3600)) // 1秒到1小时
                        .executes(ctx -> {
                            int duration = IntegerArgumentType.getInteger(ctx, "duration");
                            return executeStopMusic(ctx, duration);
                        })
                )
                .executes(ctx -> {
                    // 不发送任何提示
                    return 0;
                })
        );
    }

    private static int executeStopMusic(CommandContext<CommandSourceStack> ctx, int duration) throws CommandSyntaxException {
        // 向所有玩家发送控制包
        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            EasySubtitlesMod.NETWORK_CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new MusicControlPacket(duration * 1000L) // 转换为毫秒
            );
        }

        return 1;
    }
}