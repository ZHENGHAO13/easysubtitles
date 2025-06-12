package com.zhenghao123.easysubtitles.config;

import net.minecraftforge.common.ForgeConfigSpec;

public class ConfigHandler {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // PNG背景配置
    public static final ForgeConfigSpec.BooleanValue USE_IMAGE_BG;
    public static final ForgeConfigSpec.ConfigValue<String> BG_IMAGE_PATH;
    public static final ForgeConfigSpec.DoubleValue BG_SCALE;

    static {
        BUILDER.push("字幕背景配置");
        USE_IMAGE_BG = BUILDER.comment("是否使用PNG图片作为字幕背景")
                .define("use_image_bg", false);
        BG_IMAGE_PATH = BUILDER.comment("背景图片资源路径 (格式: modid:path/to/image)")
                .define("bg_image_path", "easysubtitles:textures/gui/subtitle_bg.png");
        BG_SCALE = BUILDER.comment("背景图片缩放比例 (0.5-2.0)")
                .defineInRange("bg_scale", 1.0, 0.5, 2.0);
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}