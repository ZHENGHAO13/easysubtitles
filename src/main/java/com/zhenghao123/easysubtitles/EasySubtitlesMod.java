package com.zhenghao123.easysubtitles;

import com.zhenghao123.easysubtitles.config.ConfigHandler;
import com.zhenghao123.easysubtitles.config.ConfigMenuIntegration;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(EasySubtitlesMod.MODID)
public class EasySubtitlesMod {
    public static final String MODID = "easysubtitles";
    public static final Logger LOGGER = LogManager.getLogger();

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel NETWORK_CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    public EasySubtitlesMod() {
        LOGGER.info("EasySubtitles 模组正在初始化...");

        ensureSubtitleDirectoryExists();

        ModLoadingContext.get().registerConfig(
                ModConfig.Type.CLIENT,
                ConfigHandler.SPEC,
                "easysubtitles-client.toml"
        );

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::onCommonSetup);
        if (FMLEnvironment.dist == Dist.CLIENT) {
            modEventBus.addListener(this::onClientSetup);

            modEventBus.addListener((FMLClientSetupEvent event) -> {
                ConfigMenuIntegration.registerConfigMenu();
            });

            modEventBus.addListener(ConfigMenuIntegration::onConfigReload);
        }

        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(MusicController.class); // 注册音乐控制器
        LOGGER.info("主类初始化完成");
    }

    private void ensureSubtitleDirectoryExists() {
        File subDir = FMLPaths.CONFIGDIR.get().resolve("easysubtitles").toFile();
        if (!subDir.exists()) {
            LOGGER.info("创建字幕目录: {}", subDir.getAbsolutePath());
            if (subDir.mkdirs()) {
                LOGGER.info("目录创建成功");
            } else {
                LOGGER.error("目录创建失败: {}", subDir.getAbsolutePath());
            }
        }
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.debug("正在执行通用设置...");
        // 注册网络包
        NETWORK_CHANNEL.registerMessage(0, PlaySubtitlePacket.class,
                PlaySubtitlePacket::encode,
                PlaySubtitlePacket::new,
                PlaySubtitlePacket::handle);

        // 新增停止字幕包注册
        NETWORK_CHANNEL.registerMessage(1, StopSubtitlePacket.class,
                StopSubtitlePacket::encode,
                StopSubtitlePacket::new,
                StopSubtitlePacket::handle);

        // 新增音乐控制包注册
        NETWORK_CHANNEL.registerMessage(2, MusicControlPacket.class,
                MusicControlPacket::encode,
                MusicControlPacket::new,
                MusicControlPacket::handle);

        LOGGER.info("网络通道已注册，支持 {} 种消息类型", 3);
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        LOGGER.info("正在初始化EasySubtitles客户端组件...");

        CommandPlayListener commandPlayListener = new CommandPlayListener();
        SubtitleRenderer subtitleRenderer = new SubtitleRenderer();

        MinecraftForge.EVENT_BUS.register(commandPlayListener);
        MinecraftForge.EVENT_BUS.register(subtitleRenderer);

        LOGGER.info("已注册命令监听器: {}", commandPlayListener != null);
        LOGGER.info("已注册字幕渲染器: {}", subtitleRenderer != null);
        LOGGER.info("客户端组件初始化完成");
    }

    // 应用新配置
    public static void applyNewConfig() {
        LOGGER.info("Applying new subtitle configuration");
        if (FMLEnvironment.dist == Dist.CLIENT) {
            Minecraft.getInstance().tell(() -> {
                if (Minecraft.getInstance().player != null) {
                    Minecraft.getInstance().player.displayClientMessage(
                            Component.literal("EasySubtitles configuration reloaded!"),
                            true
                    );
                }
            });
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("正在注册EasySubtitles命令处理器...");
        CommandHandler.register(event.getDispatcher());
        MusicControlCommand.register(event.getDispatcher()); // 注册新命令
        LOGGER.info("命令处理器注册完成");
    }
}
