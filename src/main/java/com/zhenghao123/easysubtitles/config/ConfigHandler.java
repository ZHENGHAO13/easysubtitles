package com.zhenghao123.easysubtitles.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConfigHandler {
    public static final String SUBTITLE_NAMESPACE = "easysubtitles";
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // 原有配置选项
    public static final ForgeConfigSpec.BooleanValue USE_IMAGE_BG;
    public static final ForgeConfigSpec.ConfigValue<String> BG_IMAGE_PATH;
    public static final ForgeConfigSpec.DoubleValue BG_SCALE;
    public static final ForgeConfigSpec.BooleanValue AUTO_SUBTITLE;

    // 新增显示配置选项
    public static final ForgeConfigSpec.EnumValue<PositionPreset> POSITION_PRESET;
    public static final ForgeConfigSpec.DoubleValue X_OFFSET;
    public static final ForgeConfigSpec.DoubleValue Y_OFFSET;
    public static final ForgeConfigSpec.IntValue MAX_WIDTH;
    public static final ForgeConfigSpec.IntValue FONT_HEIGHT;
    public static final ForgeConfigSpec.DoubleValue BACKGROUND_OPACITY;
    public static final ForgeConfigSpec.BooleanValue SHOW_BACKGROUND;
    public static final ForgeConfigSpec.IntValue TEXT_COLOR;
    public static final ForgeConfigSpec.DoubleValue SCALE;
    public static final ForgeConfigSpec.BooleanValue ENABLE_TEXT_SHADOW;
    public static final ForgeConfigSpec.DoubleValue UPDATE_RATE;

    // 字幕位置预设枚举
    public enum PositionPreset {
        TOP_LEFT, TOP_CENTER, TOP_RIGHT,
        CENTER_LEFT, CENTER, CENTER_RIGHT,
        BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT,
        CUSTOM
    }

    private static final Logger LOGGER = LogManager.getLogger();

    static {
        // 原有背景配置
        BUILDER.comment("Background Settings").push("background");
        USE_IMAGE_BG = BUILDER.comment("Use PNG image as subtitle background")
                .define("use_image_bg", false);
        BG_IMAGE_PATH = BUILDER.comment("Background image resource path (format: modid:path/to/image)")
                .define("bg_image_path", "easysubtitles:textures/gui/subtitle_bg.png");
        BG_SCALE = BUILDER.comment("Background image scale (0.5-2.0)")
                .defineInRange("bg_scale", 1.0, 0.5, 2.0);
        BUILDER.pop();

        // 原有自动播放配置
        BUILDER.comment("Auto Play Settings").push("auto_play");
        AUTO_SUBTITLE = BUILDER.comment("Automatically play subtitles for custom sound commands")
                .define("auto_subtitle", true);
        BUILDER.pop();

        // 新增显示配置
        BUILDER.comment("Display Settings").push("display");

        POSITION_PRESET = BUILDER
                .comment("Subtitle position preset",
                        "Options: TOP_LEFT, TOP_CENTER, TOP_RIGHT, CENTER_LEFT,",
                        "CENTER, CENTER_RIGHT, BOTTOM_LEFT, BOTTOM_CENTER, BOTTOM_RIGHT, CUSTOM")
                .defineEnum("position_preset", PositionPreset.BOTTOM_CENTER);

        X_OFFSET = BUILDER
                .comment("Custom X position offset (0.0 = left, 1.0 = right)",
                        "Only used when position_preset is set to CUSTOM")
                .defineInRange("x_offset", 0.5, 0.0, 1.0);

        Y_OFFSET = BUILDER
                .comment("Custom Y position offset (0.0 = top, 1.0 = bottom)",
                        "Only used when position_preset is set to CUSTOM")
                .defineInRange("y_offset", 0.8, 0.0, 1.0);

        MAX_WIDTH = BUILDER
                .comment("Maximum subtitle width in pixels (0 = no limit)")
                .defineInRange("max_width", 400, 0, 2000);

        FONT_HEIGHT = BUILDER
                .comment("Font height in pixels (affects text size)")
                .defineInRange("font_height", 12, 8, 64);

        BACKGROUND_OPACITY = BUILDER
                .comment("Background opacity (0.0 = transparent, 1.0 = opaque)",
                        "Only applies when using solid color background")
                .defineInRange("background_opacity", 0.65, 0.0, 1.0);

        SHOW_BACKGROUND = BUILDER
                .comment("Whether to show subtitle background")
                .define("show_background", true);

        TEXT_COLOR = BUILDER
                .comment("Text color in RGB hexadecimal (0xFFFFFF = white)")
                .defineInRange("text_color", 0xFFFFFF, 0x000000, 0xFFFFFF);

        SCALE = BUILDER
                .comment("Overall scale factor for subtitles")
                .defineInRange("scale", 1.0, 0.5, 3.0);

        ENABLE_TEXT_SHADOW = BUILDER
                .comment("Enable text shadow for better readability")
                .define("enable_text_shadow", true);

        UPDATE_RATE = BUILDER
                .comment("Subtitle update rate (in seconds). Lower values make subtitles smoother",
                        "Note: Very low values may impact performance")
                .defineInRange("update_rate", 0.05, 0.01, 0.5);

        BUILDER.pop();

        SPEC = BUILDER.build();

        LOGGER.info("Configuration handler initialized");
    }
}