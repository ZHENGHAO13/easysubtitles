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
                // 重点：不在此处添加 .executes，强制逻辑流向下一步参数输入
                .then(Commands.argument("duration", IntegerArgumentType.integer(1, 3600))
                        .executes(ctx -> {
                            int duration = IntegerArgumentType.getInteger(ctx, "duration");
                            return executeStopMusic(ctx.getSource(), duration);
                        })
                )
        );
    }

    private static int executeStopMusic(CommandSourceStack source, int duration) {
        // 将秒转换为毫秒发往客户端
        long durationMs = duration * 1000L;
        if (source.getEntity() instanceof ServerPlayer player) {
            sendPacket(player, durationMs);
        } else {
            for (ServerPlayer player : source.getServer().getPlayerList().getPlayers()) {
                sendPacket(player, durationMs);
            }
        }
        return 1;
    }

    private static void sendPacket(ServerPlayer player, long ms) {
        EasySubtitlesMod.NETWORK_CHANNEL.send(
                PacketDistributor.PLAYER.with(() -> player),
                new MusicControlPacket(ms)
        );
    }
}