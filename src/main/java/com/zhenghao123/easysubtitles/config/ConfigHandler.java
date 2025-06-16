package com.zhenghao123.easysubtitles.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class ConfigHandler {
    public static final String SUBTITLE_NAMESPACE = "easysubtitles";
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // 字幕位置预设枚举
    public enum PositionPreset {
        TOP_LEFT,       // 左上角
        TOP_CENTER,     // 顶部居中
        TOP_RIGHT,      // 右上角
        CENTER_LEFT,    // 左中
        CENTER,         // 正中心
        CENTER_RIGHT,   // 右中
        BOTTOM_LEFT,    // 左下角
        BOTTOM_CENTER,  // 底部居中
        BOTTOM_RIGHT,   // 右下角
        CUSTOM          // 自定义位置
    }

    // 位置调整模式枚举
    public enum PositionMode {
        RATIO,      // 使用比例调整
        PIXEL       // 使用像素调整
    }

    // 配置选项定义
    // ------------------- 背景设置 -------------------
    public static final ForgeConfigSpec.BooleanValue USE_IMAGE_BG;           // 是否使用图片背景
    public static final ForgeConfigSpec.ConfigValue<String> BG_IMAGE_PATH;   // 背景图片路径
    public static final ForgeConfigSpec.DoubleValue BG_SCALE;               // 背景缩放比例

    // ------------------- 自动播放设置 -------------------
    public static final ForgeConfigSpec.BooleanValue AUTO_SUBTITLE;         // 是否自动显示字幕

    // ------------------- 显示设置 -------------------
    public static final ForgeConfigSpec.EnumValue<PositionPreset> POSITION_PRESET; // 字幕位置预设
    public static final ForgeConfigSpec.EnumValue<PositionMode> POSITION_MODE;    // 位置调整模式
    public static final ForgeConfigSpec.DoubleValue X_OFFSET;               // 自定义X偏移（比例）
    public static final ForgeConfigSpec.DoubleValue Y_OFFSET;               // 自定义Y偏移（比例）
    public static final ForgeConfigSpec.IntValue PIXEL_X_OFFSET;            // 自定义X偏移（像素）
    public static final ForgeConfigSpec.IntValue PIXEL_Y_OFFSET;            // 自定义Y偏移（像素）
    public static final ForgeConfigSpec.BooleanValue CENTER_ALIGNED;        // 像素偏移是否基于中心
    public static final ForgeConfigSpec.IntValue MAX_WIDTH;                 // 最大宽度
    public static final ForgeConfigSpec.IntValue FONT_HEIGHT;               // 字体高度
    public static final ForgeConfigSpec.DoubleValue BACKGROUND_OPACITY;     // 背景不透明度
    public static final ForgeConfigSpec.BooleanValue SHOW_BACKGROUND;        // 是否显示背景
    public static final ForgeConfigSpec.IntValue TEXT_COLOR;                // 文字颜色
    public static final ForgeConfigSpec.DoubleValue SCALE;                  // 整体缩放
    public static final ForgeConfigSpec.BooleanValue ENABLE_TEXT_SHADOW;    // 启用文字阴影（修复拼写错误）
    public static final ForgeConfigSpec.DoubleValue UPDATE_RATE;            // 更新频率

    private static final Logger LOGGER = LogManager.getLogger();

    static {
        // =================== 背景设置 ===================
        BUILDER.comment("背景设置 - 控制字幕背景的外观和类型").push("background");

        USE_IMAGE_BG = BUILDER.comment(
                "是否使用PNG图片作为字幕背景",
                "如果为 true，将使用指定的背景图片",
                "如果为 false，使用简单的半透明纯色背景"
        ).define("use_image_bg", false);

        BG_IMAGE_PATH = BUILDER.comment(
                "背景图片资源路径 (格式: 命名空间:路径/图片.png)",
                "例如: 'easysubtitles:textures/gui/subtitle_bg.png'",
                "请确保该资源存在于您的资源包中"
        ).define("bg_image_path", "easysubtitles:textures/gui/subtitle_bg.png");

        BG_SCALE = BUILDER.comment(
                "背景图片的缩放比例 (0.5-2.0)",
                "值小于1.0会缩小图片，大于1.0会放大图片"
        ).defineInRange("bg_scale", 1.0, 0.5, 2.0);

        BUILDER.pop();

        // =================== 自动播放设置 ===================
        BUILDER.comment("自动播放设置 - 控制字幕自动显示的规则").push("auto_play");

        AUTO_SUBTITLE = BUILDER.comment(
                "是否自动为专用命令声音事件显示字幕",
                "如果为 true，当命名空间为 'easysubtitles' 的声音播放时，自动显示对应的字幕",
                "如果为 false，只能通过命令手动显示字幕"
        ).define("auto_subtitle", true);

        BUILDER.pop();

        // =================== 显示设置 ===================
        BUILDER.comment("显示设置 - 控制字幕在屏幕上的显示方式").push("display");

        POSITION_PRESET = BUILDER.comment(
                "字幕位置预设",
                "选项: ",
                "TOP_LEFT - 左上角",
                "TOP_CENTER - 顶部居中",
                "TOP_RIGHT - 右上角",
                "CENTER_LEFT - 左中",
                "CENTER - 正中心",
                "CENTER_RIGHT - 右中",
                "BOTTOM_LEFT - 左下角",
                "BOTTOM_CENTER - 底部居中",
                "BOTTOM_RIGHT - 右下角",
                "CUSTOM - 自定义位置"
        ).defineEnum("position_preset", PositionPreset.CUSTOM);

        POSITION_MODE = BUILDER.comment(
                "位置调整模式",
                "RATIO: 使用比例定位 (0.0-1.0) - 适用于不同分辨率",
                "PIXEL: 使用像素定位 - 提供精确控制"
        ).defineEnum("position_mode", PositionMode.RATIO);

        X_OFFSET = BUILDER.comment(
                "自定义X位置偏移 (0.0 = 左侧, 1.0 = 右侧)",
                "仅在位置预设设置为 CUSTOM 并且位置模式为 RATIO 时生效",
                "表示相对于屏幕宽度的偏移比例"
        ).defineInRange("x_offset", 0.5, 0.0, 1.0);

        Y_OFFSET = BUILDER.comment(
                "自定义Y位置偏移 (0.0 = 顶部, 1.0 = 底部)",
                "仅在位置预设设置为 CUSTOM 并且位置模式为 RATIO 时生效",
                "表示相对于屏幕高度的偏移比例",
                "设置值: 0.8 - 底部上方20%位置",
                "设置值: 0.48 - 比原版物品名称高50%的位置"
        ).defineInRange("y_offset", 0.795, 0.0, 1.0);

        PIXEL_X_OFFSET = BUILDER.comment(
                "X方向像素偏移",
                "仅在位置预设设置为 CUSTOM 并且位置模式为 PIXEL 时生效",
                "正值向右偏移，负值向左偏移",
                "如果启用中心对齐，原点是屏幕中心",
                "如果禁用中心对齐，原点是左上角"
        ).defineInRange("pixel_x_offset", 0, -1000, 1000);

        PIXEL_Y_OFFSET = BUILDER.comment(
                "Y方向像素偏移",
                "仅在位置预设设置为 CUSTOM 并且位置模式为 PIXEL 时生效",
                "正值向下偏移，负值向上偏移",
                "如果启用中心对齐，原点是屏幕中心",
                "如果禁用中心对齐，原点是左上角",
                "示例: -30 - 向上移动30像素"
        ).defineInRange("pixel_y_offset", -50, -1000, 1000);

        CENTER_ALIGNED = BUILDER.comment(
                "是否使用中心对齐",
                "仅在位置预设设置为 CUSTOM 并且位置模式为 PIXEL 时生效",
                "true: 以屏幕中心为原点 (0,0)",
                "false: 以屏幕左上角为原点 (默认)",
                "建议在需要精确居中定位时启用"
        ).define("center_aligned", false);

        MAX_WIDTH = BUILDER.comment(
                "字幕最大宽度（像素）(0 = 不换行)",
                "当字幕文本超过此宽度时会自动换行",
                "建议值: 300-600 像素"
        ).defineInRange("max_width", 400, 0, 2000);

        FONT_HEIGHT = BUILDER.comment(
                "字体高度（像素）(影响文字大小)",
                "默认值为12像素，这是游戏原生字幕大小",
                "值在8到64像素之间"
        ).defineInRange("font_height", 9, 8, 64);

        BACKGROUND_OPACITY = BUILDER.comment(
                "背景不透明度 (0.0 = 完全透明, 1.0 = 完全不透明)",
                "仅在未使用图片背景时生效",
                "设置半透明黑色背景的不透明度"
        ).defineInRange("background_opacity", 0.65, 0.0, 1.0);

        SHOW_BACKGROUND = BUILDER.comment(
                "是否显示字幕背景",
                "如果为 true，显示背景（图片或纯色）",
                "如果为 false，文本将没有背景直接显示在屏幕上"
        ).define("show_background", true);

        TEXT_COLOR = BUILDER.comment(
                "文字颜色（十六进制RGB格式）",
                "例如: 0xFFFFFF = 白色, 0xFF0000 = 红色",
                "颜色范围: 0x000000 (黑色) 到 0xFFFFFF (白色)",
                "使用十六进制颜色值"
        ).defineInRange("text_color", 0xFFFFFF, 0x000000, 0xFFFFFF);

        SCALE = BUILDER.comment(
                "整体缩放因子",
                "影响字幕位置和大小的整体比例",
                "值小于1.0会缩小整个字幕，大于1.0会放大",
                "默认值1.0（不缩放）",
                "注意: 此选项不影响像素位置模式"
        ).defineInRange("scale", 1.0, 0.5, 3.0);

        ENABLE_TEXT_SHADOW = BUILDER.comment(
                "是否启用文字阴影",
                "如果为 true，文字会添加阴影效果（提高可读性）",
                "如果为 false，文字显示无阴影效果",
                "深色背景上建议启用，浅色背景上建议禁用"
        ).define("enable_text_shadow", true);

        UPDATE_RATE = BUILDER.comment(
                "字幕更新速率（秒）",
                "控制字幕在屏幕上更新的频率",
                "较低的值使字幕更流畅（但消耗更多性能）",
                "较高的值使字幕更新较慢（节省性能）",
                "默认值0.05秒（每秒20帧）",
                "注意: 过低的值可能影响性能"
        ).defineInRange("update_rate", 0.05, 0.01, 0.5);

        BUILDER.pop(); // 结束显示设置

        // 构建最终配置规范
        SPEC = BUILDER.build();

        LOGGER.info("配置处理器已初始化");
    }
}