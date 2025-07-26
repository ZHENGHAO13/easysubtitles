package com.zhenghao123.easysubtitles;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

public class MusicControlCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("stopmusic")
                .requires(source -> source.hasPermission(2))
                .then(Commands.argument("duration", IntegerArgumentType.integer(1, 3600))
                        .executes(ctx -> {
                            int duration = IntegerArgumentType.getInteger(ctx, "duration");
                            executeStopMusic(duration);
                            return 1;
                        })
                )
                .executes(ctx -> 0)
        );
    }

    private static void executeStopMusic(int duration) {
        for (ServerPlayer player : ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayers()) {
            EasySubtitlesMod.NETWORK_CHANNEL.send(
                    net.minecraftforge.network.PacketDistributor.PLAYER.with(() -> player),
                    new MusicControlPacket(duration * 1000L)
            );
        }
    }
}