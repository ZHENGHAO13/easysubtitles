package com.zhenghao123.easysubtitles.config;

import com.zhenghao123.easysubtitles.EasySubtitlesMod;
import com.zhenghao123.easysubtitles.gui.ConfigScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

public class ConfigMenuIntegration {
    public static void registerConfigMenu() {
        ModLoadingContext.get().registerExtensionPoint(
                ConfigScreenHandler.ConfigScreenFactory.class,
                () -> new ConfigScreenHandler.ConfigScreenFactory(
                        (minecraft, parent) -> new ConfigScreen(parent, ConfigHandler.SPEC)
                )
        );
    }

    public static void onConfigReload(ModConfigEvent event) {
        if (event.getConfig().getSpec() == ConfigHandler.SPEC) {
            EasySubtitlesMod.applyNewConfig();
        }
    }
}