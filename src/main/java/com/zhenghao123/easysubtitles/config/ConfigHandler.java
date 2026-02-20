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
    public static final ForgeConfigSpec.BooleanValue CLOSE_ON_ESC;          // ESC菜单是否关闭播放

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
    public static final ForgeConfigSpec.BooleanValue SHOW_BACKGROUND;     // 是否显示背景
    public static final ForgeConfigSpec.IntValue TEXT_COLOR;                // 文字颜色
    public static final ForgeConfigSpec.DoubleValue SCALE;                  // 整体缩放
    public static final ForgeConfigSpec.BooleanValue ENABLE_TEXT_SHADOW;    // 启用文字阴影
    public static final ForgeConfigSpec.DoubleValue UPDATE_RATE;            // 更新频率

    private static final Logger LOGGER = LogManager.getLogger();

    // 默认值常量
    private static final boolean DEFAULT_AUTO_SUBTITLE = true;
    private static final boolean DEFAULT_USE_IMAGE_BG = false;
    private static final String DEFAULT_BG_IMAGE_PATH = "easysubtitles:textures/gui/subtitle_bg.png";
    private static final double DEFAULT_BG_SCALE = 1.0;
    private static final boolean DEFAULT_CLOSE_ON_ESC = true;
    private static final PositionPreset DEFAULT_POSITION_PRESET = PositionPreset.BOTTOM_CENTER;
    private static final PositionMode DEFAULT_POSITION_MODE = PositionMode.RATIO;
    private static final double DEFAULT_X_OFFSET = 0.5;
    private static final double DEFAULT_Y_OFFSET = 0.8;
    private static final int DEFAULT_PIXEL_X_OFFSET = 0;
    private static final int DEFAULT_PIXEL_Y_OFFSET = -50;
    private static final boolean DEFAULT_CENTER_ALIGNED = false;
    private static final int DEFAULT_MAX_WIDTH = 400;
    private static final int DEFAULT_FONT_HEIGHT = 9;
    private static final double DEFAULT_BACKGROUND_OPACITY = 0.65;
    private static final boolean DEFAULT_SHOW_BACKGROUND = true;
    private static final int DEFAULT_TEXT_COLOR = 0xFFFFFF;
    private static final double DEFAULT_SCALE = 1.0;
    private static final boolean DEFAULT_ENABLE_TEXT_SHADOW = true;
    private static final double DEFAULT_UPDATE_RATE = 0.05;

    static {
        // =================== 基本设置 ===================
        BUILDER.comment("基本设置 - 控制字幕的基本行为").push("basic");

        AUTO_SUBTITLE = BUILDER.comment(
                "是否自动为专用命令声音事件显示字幕",
                "如果为 true，当命名空间为 'easysubtitles' 的声音播放时，自动显示对应的字幕",
                "如果为 false，只能通过命令手动显示字幕"
        ).define("auto_subtitle", DEFAULT_AUTO_SUBTITLE);

        CLOSE_ON_ESC = BUILDER.comment(
                "是否在ESC菜单打开时关闭配音和字幕",
                "如果为 true，打开ESC菜单时会停止所有音频和字幕播放",
                "如果为 false，ESC菜单不会影响正在播放的音频和字幕"
        ).define("close_on_esc", DEFAULT_CLOSE_ON_ESC);

        BUILDER.pop();

        // =================== 背景设置 ===================
        BUILDER.comment("背景设置 - 控制字幕背景的外观和类型").push("background");

        USE_IMAGE_BG = BUILDER.comment(
                "是否使用PNG图片作为字幕背景",
                "如果为 true，将使用指定的背景图片",
                "如果为 false，使用简单的半透明纯色背景"
        ).define("use_image_bg", DEFAULT_USE_IMAGE_BG);

        BG_IMAGE_PATH = BUILDER.comment(
                "背景图片资源路径 (格式: 命名空间:路径/图片.png)",
                "例如: 'easysubtitles:textures/gui/subtitle_bg.png'",
                "请确保该资源存在于您的资源包中"
        ).define("bg_image_path", DEFAULT_BG_IMAGE_PATH);

        BG_SCALE = BUILDER.comment(
                "背景图片的缩放比例 (0.5-2.0)",
                "值小于1.0会缩小图片，大于1.0会放大图片"
        ).defineInRange("bg_scale", DEFAULT_BG_SCALE, 0.5, 2.0);

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
        ).defineEnum("position_preset", DEFAULT_POSITION_PRESET);

        POSITION_MODE = BUILDER.comment(
                "位置调整模式",
                "RATIO: 使用比例定位 (0.0-1.0) - 适用于不同分辨率",
                "PIXEL: 使用像素定位 - 提供精确控制"
        ).defineEnum("position_mode", DEFAULT_POSITION_MODE);

        X_OFFSET = BUILDER.comment(
                "自定义X位置偏移 (0.0 = 左侧, 1.0 = 右侧)",
                "仅在位置预设设置为 CUSTOM 并且位置模式为 RATIO 时生效",
                "表示相对于屏幕宽度的偏移比例"
        ).defineInRange("x_offset", DEFAULT_X_OFFSET, 0.0, 1.0);

        Y_OFFSET = BUILDER.comment(
                "自定义Y位置偏移 (0.0 = 顶部, 1.0 = 底部)",
                "仅在位置预设设置为 CUSTOM 并且位置模式为 RATIO 时生效",
                "表示相对于屏幕高度的偏移比例"
        ).defineInRange("y_offset", DEFAULT_Y_OFFSET, 0.0, 1.0);

        PIXEL_X_OFFSET = BUILDER.comment(
                "X方向像素偏移",
                "仅在位置预设设置为 CUSTOM 并且位置模式为 PIXEL 时生效",
                "正值向右偏移，负值向左偏移",
                "如果启用中心对齐，原点是屏幕中心",
                "如果禁用中心对齐，原点是左上角"
        ).defineInRange("pixel_x_offset", DEFAULT_PIXEL_X_OFFSET, -1000, 1000);

        PIXEL_Y_OFFSET = BUILDER.comment(
                "Y方向像素偏移",
                "仅在位置预设设置为 CUSTOM 并且位置模式为 PIXEL 时生效",
                "正值向下偏移，负值向上偏移",
                "如果启用中心对齐，原点是屏幕中心",
                "如果禁用中心对齐，原点是左上角"
        ).defineInRange("pixel_y_offset", DEFAULT_PIXEL_Y_OFFSET, -1000, 1000);

        CENTER_ALIGNED = BUILDER.comment(
                "是否使用中心对齐",
                "仅在位置预设设置为 CUSTOM 并且位置模式为 PIXEL 时生效",
                "true: 以屏幕中心为原点 (0,0)",
                "false: 以屏幕左上角为原点 (默认)"
        ).define("center_aligned", DEFAULT_CENTER_ALIGNED);

        MAX_WIDTH = BUILDER.comment(
                "字幕最大宽度（像素）(0 = 不换行)",
                "当字幕文本超过此宽度时会自动换行",
                "建议值: 300-600 像素"
        ).defineInRange("max_width", DEFAULT_MAX_WIDTH, 0, 2000);

        FONT_HEIGHT = BUILDER.comment(
                "字体高度（像素）(影响文字大小)",
                "默认值为9像素",
                "值在8到64像素之间"
        ).defineInRange("font_height", DEFAULT_FONT_HEIGHT, 8, 64);

        BACKGROUND_OPACITY = BUILDER.comment(
                "背景不透明度 (0.0 = 完全透明, 1.0 = 完全不透明)",
                "仅在未使用图片背景时生效",
                "设置半透明黑色背景的不透明度"
        ).defineInRange("background_opacity", DEFAULT_BACKGROUND_OPACITY, 0.0, 1.0);

        SHOW_BACKGROUND = BUILDER.comment(
                "是否显示字幕背景",
                "如果为 true，显示背景（图片或纯色）",
                "如果为 false，文本将没有背景直接显示在屏幕上"
        ).define("show_background", DEFAULT_SHOW_BACKGROUND);

        TEXT_COLOR = BUILDER.comment(
                "文字颜色（十六进制RGB格式）",
                "例如: 0xFFFFFF = 白色, 0xFF0000 = 红色",
                "颜色范围: 0x000000 (黑色) 到 0xFFFFFF (白色)",
                "使用十六进制颜色值"
        ).defineInRange("text_color", DEFAULT_TEXT_COLOR, 0x000000, 0xFFFFFF);

        SCALE = BUILDER.comment(
                "整体缩放因子",
                "影响字幕位置和大小的整体比例",
                "值小于1.0会缩小整个字幕，大于1.0会放大",
                "默认值1.0（不缩放）"
        ).defineInRange("scale", DEFAULT_SCALE, 0.5, 3.0);

        ENABLE_TEXT_SHADOW = BUILDER.comment(
                "是否启用文字阴影",
                "如果为 true，文字会添加阴影效果（提高可读性）",
                "如果为 false，文字显示无阴影效果"
        ).define("enable_text_shadow", DEFAULT_ENABLE_TEXT_SHADOW);

        UPDATE_RATE = BUILDER.comment(
                "字幕更新速率（秒）",
                "控制字幕在屏幕上更新的频率",
                "较低的值使字幕更流畅（但消耗更多性能）",
                "较高的值使字幕更新较慢（节省性能）",
                "默认值0.05秒（每秒20帧）"
        ).defineInRange("update_rate", DEFAULT_UPDATE_RATE, 0.01, 0.5);

        BUILDER.pop(); // 结束显示设置

        // 构建最终配置规范
        SPEC = BUILDER.build();

        LOGGER.info("配置处理器已初始化");
    }

    // =================== 获取默认值的方法 ===================

    public static boolean getAUTO_SUBTITLE_Default() {
        return DEFAULT_AUTO_SUBTITLE;
    }

    public static boolean getUSE_IMAGE_BG_Default() {
        return DEFAULT_USE_IMAGE_BG;
    }

    public static String getBG_IMAGE_PATH_Default() {
        return DEFAULT_BG_IMAGE_PATH;
    }

    public static double getBG_SCALE_Default() {
        return DEFAULT_BG_SCALE;
    }

    public static boolean getCLOSE_ON_ESC_Default() {
        return DEFAULT_CLOSE_ON_ESC;
    }

    public static PositionPreset getPOSITION_PRESET_Default() {
        return DEFAULT_POSITION_PRESET;
    }

    public static PositionMode getPOSITION_MODE_Default() {
        return DEFAULT_POSITION_MODE;
    }

    public static double getX_OFFSET_Default() {
        return DEFAULT_X_OFFSET;
    }

    public static double getY_OFFSET_Default() {
        return DEFAULT_Y_OFFSET;
    }

    public static int getPIXEL_X_OFFSET_Default() {
        return DEFAULT_PIXEL_X_OFFSET;
    }

    public static int getPIXEL_Y_OFFSET_Default() {
        return DEFAULT_PIXEL_Y_OFFSET;
    }

    public static boolean getCENTER_ALIGNED_Default() {
        return DEFAULT_CENTER_ALIGNED;
    }

    public static int getMAX_WIDTH_Default() {
        return DEFAULT_MAX_WIDTH;
    }

    public static int getFONT_HEIGHT_Default() {
        return DEFAULT_FONT_HEIGHT;
    }

    public static double getBACKGROUND_OPACITY_Default() {
        return DEFAULT_BACKGROUND_OPACITY;
    }

    public static boolean getSHOW_BACKGROUND_Default() {
        return DEFAULT_SHOW_BACKGROUND;
    }

    public static int getTEXT_COLOR_Default() {
        return DEFAULT_TEXT_COLOR;
    }

    public static double getSCALE_Default() {
        return DEFAULT_SCALE;
    }

    public static boolean getENABLE_TEXT_SHADOW_Default() {
        return DEFAULT_ENABLE_TEXT_SHADOW;
    }

    public static double getUPDATE_RATE_Default() {
        return DEFAULT_UPDATE_RATE;
    }

    // =================== 便捷方法 ===================

    /**
     * 将所有配置重置为默认值
     */
    public static void resetToDefaults() {
        LOGGER.info("将配置重置为默认值");

        AUTO_SUBTITLE.set(getAUTO_SUBTITLE_Default());
        USE_IMAGE_BG.set(getUSE_IMAGE_BG_Default());
        BG_IMAGE_PATH.set(getBG_IMAGE_PATH_Default());
        BG_SCALE.set(getBG_SCALE_Default());
        CLOSE_ON_ESC.set(getCLOSE_ON_ESC_Default());
        POSITION_PRESET.set(getPOSITION_PRESET_Default());
        POSITION_MODE.set(getPOSITION_MODE_Default());
        X_OFFSET.set(getX_OFFSET_Default());
        Y_OFFSET.set(getY_OFFSET_Default());
        PIXEL_X_OFFSET.set(getPIXEL_X_OFFSET_Default());
        PIXEL_Y_OFFSET.set(getPIXEL_Y_OFFSET_Default());
        CENTER_ALIGNED.set(getCENTER_ALIGNED_Default());
        MAX_WIDTH.set(getMAX_WIDTH_Default());
        FONT_HEIGHT.set(getFONT_HEIGHT_Default());
        BACKGROUND_OPACITY.set(getBACKGROUND_OPACITY_Default());
        SHOW_BACKGROUND.set(getSHOW_BACKGROUND_Default());
        TEXT_COLOR.set(getTEXT_COLOR_Default());
        SCALE.set(getSCALE_Default());
        ENABLE_TEXT_SHADOW.set(getENABLE_TEXT_SHADOW_Default());
        UPDATE_RATE.set(getUPDATE_RATE_Default());

        // 保存到配置文件
        SPEC.save();

        LOGGER.info("配置重置完成");
    }

    /**
     * 应用配置到渲染器
     */
    public static void applyConfig() {
        LOGGER.info("应用配置到渲染器");

        // 这里可以添加配置应用逻辑
        // 例如：重新加载背景纹理、刷新字幕位置等

        LOGGER.info("配置应用完成");
    }
}