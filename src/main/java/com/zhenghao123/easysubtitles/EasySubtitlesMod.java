package com.zhenghao123.easysubtitles;

import com.zhenghao123.easysubtitles.config.ConfigHandler;
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
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(EasySubtitlesMod.MODID)
public class EasySubtitlesMod {
    public static final String MODID = "easysubtitles";
    public static final Logger LOGGER = LogManager.getLogger();

    private SubtitleRenderer subtitleRenderer;
    private CommandPlayListener commandPlayListener;

    public EasySubtitlesMod() {
        LOGGER.info("EasySubtitles 模组正在初始化...");


        ModLoadingContext.get().registerConfig(
                ModConfig.Type.CLIENT,
                ConfigHandler.SPEC,
                "easysubtitles-client.toml"
        );


        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();


        modEventBus.addListener(this::onCommonSetup);
        modEventBus.addListener(this::onClientSetup);


        MinecraftForge.EVENT_BUS.register(this);

        LOGGER.info("主类初始化完成");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        LOGGER.debug("正在执行通用设置...");
    }

    private void onClientSetup(FMLClientSetupEvent event) {
        LOGGER.info("正在初始化EasySubtitles客户端组件...");


        this.subtitleRenderer = new SubtitleRenderer();
        this.commandPlayListener = new CommandPlayListener();


        MinecraftForge.EVENT_BUS.register(subtitleRenderer);
        MinecraftForge.EVENT_BUS.register(commandPlayListener);


        LOGGER.info("已注册字幕渲染器: {}", subtitleRenderer != null);
        LOGGER.info("已注册命令监听器: {}", commandPlayListener != null);
        LOGGER.info("客户端组件初始化完成");
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        LOGGER.info("正在注册EasySubtitles命令处理器...");
        CommandHandler.register(event.getDispatcher());
        LOGGER.info("命令处理器注册完成");
    }

    public SubtitleRenderer getSubtitleRenderer() {
        return subtitleRenderer;
    }

    public CommandPlayListener getCommandPlayListener() {
        return commandPlayListener;
    }
}