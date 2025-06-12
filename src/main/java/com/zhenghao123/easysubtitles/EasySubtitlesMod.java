package com.zhenghao123.easysubtitles;

import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod(EasySubtitlesMod.MODID)
public class EasySubtitlesMod {
    public static final String MODID = "easysubtitles";

    public EasySubtitlesMod() {
        MinecraftForge.EVENT_BUS.register(this);
        MinecraftForge.EVENT_BUS.register(new SubtitleRenderer());
    }

    @SubscribeEvent
    public void onCommandRegister(RegisterCommandsEvent event) {
        CommandHandler.register(event.getDispatcher());
    }
}