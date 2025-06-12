package com.zhenghao123.easysubtitles;

import com.zhenghao123.easysubtitles.config.ConfigHandler;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;

@Mod(EasySubtitlesMod.MODID)
public class EasySubtitlesMod {
    public static final String MODID = "easysubtitles";

    public EasySubtitlesMod() {
        // 注册配置文件
        ModLoadingContext.get().registerConfig(
                ModConfig.Type.CLIENT,
                ConfigHandler.SPEC,
                "easysubtitles-client.toml"
        );
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new SubtitleRenderer());
    }

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {
        CommandHandler.register(event.getDispatcher());
    }
}