package com.zhenghao123.easysubtitles;

import com.zhenghao123.easysubtitles.config.ConfigHandler;
import net.minecraft.resources.ResourceLocation;
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

        // 在模组启动时确保字幕目录存在
        ensureSubtitleDirectoryExists();

        // 注册配置
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.CLIENT,
                ConfigHandler.SPEC,
                "easysubtitles-client.toml"
        );

        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        modEventBus.addListener(this::onCommonSetup);
        if (FMLEnvironment.dist == net.minecraftforge.api.distmarker.Dist.CLIENT) {
            modEventBus.addListener(this::onClientSetup);
        }

        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("主类初始化完成");
    }

    // 新建方法，直接在EasySubtitlesMod类中处理目录创建
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

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("正在注册EasySubtitles命令处理器...");
        CommandHandler.register(event.getDispatcher());
        LOGGER.info("命令处理器注册完成");
    }
}