package com.zhenghao123.easysubtitles;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.PacketDistributor;

public class MusicControlCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("stopmusic")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("duration", IntegerArgumentType.integer(1, 3600))
                        .executes(ctx -> {
                            int duration = IntegerArgumentType.getInteger(ctx, "duration");
                            return executeStopMusic(ctx.getSource(), duration);
                        })
                )
                .executes(ctx -> 0) // 无参数时不做任何操作
        );
    }

    private static int executeStopMusic(CommandSourceStack source, int duration) {
        // 只向触发命令的玩家发送控制包
        if (source.getEntity() instanceof ServerPlayer player) {
            sendMusicControlPacket(player, duration * 1000L);
            return 1;
        } else {
            // 控制台执行时发送给所有玩家
            for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
                sendMusicControlPacket(player, duration * 1000L);
            }
            return 1;
        }
    }

    private static void sendMusicControlPacket(ServerPlayer player, long durationMs) {
        EasySubtitlesMod.NETWORK_CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new MusicControlPacket(durationMs)
        );
    }
}