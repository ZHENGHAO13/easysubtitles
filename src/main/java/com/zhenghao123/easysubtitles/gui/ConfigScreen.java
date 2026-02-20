package com.zhenghao123.easysubtitles.gui;

import com.zhenghao123.easysubtitles.config.ConfigHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraftforge.common.ForgeConfigSpec;

public class ConfigScreen extends Screen {
    private final Screen previousScreen;
    private final ForgeConfigSpec configSpec;

    // 控件
    private CycleButton<Boolean> autoSubtitleToggle;
    private CycleButton<Boolean> useImageBgToggle;
    private EditBox bgImagePathField;
    private EditBox bgScaleField;
    private CycleButton<Boolean> closeOnEscToggle;

    private CycleButton<ConfigHandler.PositionPreset> positionPresetButton;
    private EditBox xOffsetField;
    private EditBox yOffsetField;
    private EditBox maxWidthField;
    private EditBox fontSizeField;
    private EditBox opacityField;
    private CycleButton<Boolean> backgroundToggle;
    private EditBox scaleField;
    private CycleButton<Boolean> shadowToggle;
    private EditBox textColorField;
    private EditBox updateRateField;

    // 重置按钮
    private Button resetButton;

    public ConfigScreen(Screen previousScreen, ForgeConfigSpec configSpec) {
        super(Component.translatable("easysubtitles.config.title"));
        this.previousScreen = previousScreen;
        this.configSpec = configSpec;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = 40; // 从顶部开始，留出标题空间
        int rowHeight = 22;
        int sectionSpacing = 15;
        int columnWidth = 150;
        int columnSpacing = 10;

        // === 基本设置部分 ===
        addRenderableWidget(Button.builder(Component.translatable("easysubtitles.config.category.basic"), button -> {})
                .pos(centerX - 100, startY - 25)
                .size(200, 20)
                .build());

        int currentY = startY;

        // 自动字幕
        autoSubtitleToggle = CycleButton.onOffBuilder(ConfigHandler.AUTO_SUBTITLE.get())
                .create(centerX - columnWidth - columnSpacing/2, currentY, columnWidth, 20,
                        Component.translatable("easysubtitles.config.auto_subtitles"),
                        (button, value) -> {});
        addRenderableWidget(autoSubtitleToggle);

        // ESC关闭播放
        closeOnEscToggle = CycleButton.onOffBuilder(ConfigHandler.CLOSE_ON_ESC.get())
                .create(centerX + columnSpacing/2, currentY, columnWidth, 20,
                        Component.translatable("easysubtitles.config.close_on_esc"),
                        (button, value) -> {});
        addRenderableWidget(closeOnEscToggle);

        currentY += rowHeight;

        // 使用图片背景
        useImageBgToggle = CycleButton.onOffBuilder(ConfigHandler.USE_IMAGE_BG.get())
                .create(centerX - columnWidth - columnSpacing/2, currentY, columnWidth, 20,
                        Component.translatable("easysubtitles.config.use_image_bg"),
                        (button, value) -> {});
        addRenderableWidget(useImageBgToggle);

        // 显示背景
        backgroundToggle = CycleButton.onOffBuilder(ConfigHandler.SHOW_BACKGROUND.get())
                .create(centerX + columnSpacing/2, currentY, columnWidth, 20,
                        Component.translatable("easysubtitles.config.show_bg"),
                        (button, value) -> {});
        addRenderableWidget(backgroundToggle);

        currentY += rowHeight;

        // 背景图片路径（全宽度）
        bgImagePathField = new EditBox(this.font, centerX - columnWidth*2 - columnSpacing/2, currentY,
                columnWidth*2 + columnSpacing, 20, Component.translatable("easysubtitles.config.bg_image_path"));
        bgImagePathField.setValue(ConfigHandler.BG_IMAGE_PATH.get());
        addRenderableWidget(bgImagePathField);

        currentY += rowHeight;

        // 背景缩放
        bgScaleField = new EditBox(this.font, centerX - columnWidth - columnSpacing/2, currentY, columnWidth, 20,
                Component.translatable("easysubtitles.config.bg_scale"));
        bgScaleField.setValue(String.valueOf(ConfigHandler.BG_SCALE.get()));
        addRenderableWidget(bgScaleField);

        // 文字阴影
        shadowToggle = CycleButton.onOffBuilder(ConfigHandler.ENABLE_TEXT_SHADOW.get())
                .create(centerX + columnSpacing/2, currentY, columnWidth, 20,
                        Component.translatable("easysubtitles.config.text_shadow"),
                        (button, value) -> {});
        addRenderableWidget(shadowToggle);

        currentY += rowHeight + sectionSpacing;

        // === 位置和显示设置 ===
        addRenderableWidget(Button.builder(Component.translatable("easysubtitles.config.category.display"), button -> {})
                .pos(centerX - 100, currentY - 25)
                .size(200, 20)
                .build());

        currentY += 5; // 小间距

        // 位置预设（全宽度）
        positionPresetButton = CycleButton.<ConfigHandler.PositionPreset>builder(value ->
                        Component.translatable("easysubtitles.config.position_preset." + value.name().toLowerCase()))
                .withValues(ConfigHandler.PositionPreset.values())
                .create(centerX - columnWidth*2 - columnSpacing/2, currentY, columnWidth*2 + columnSpacing, 20,
                        Component.translatable("easysubtitles.config.position_preset"),
                        (button, value) -> {});
        positionPresetButton.setValue(ConfigHandler.POSITION_PRESET.get());
        addRenderableWidget(positionPresetButton);

        currentY += rowHeight;

        // 自定义位置设置（仅当选择CUSTOM时显示提示）
        if (positionPresetButton.getValue() == ConfigHandler.PositionPreset.CUSTOM) {
            // X偏移
            xOffsetField = new EditBox(this.font, centerX - columnWidth - columnSpacing/2, currentY, columnWidth, 20,
                    Component.translatable("easysubtitles.config.x_offset"));
            xOffsetField.setValue(String.valueOf(ConfigHandler.X_OFFSET.get()));
            addRenderableWidget(xOffsetField);

            // Y偏移
            yOffsetField = new EditBox(this.font, centerX + columnSpacing/2, currentY, columnWidth, 20,
                    Component.translatable("easysubtitles.config.y_offset"));
            yOffsetField.setValue(String.valueOf(ConfigHandler.Y_OFFSET.get()));
            addRenderableWidget(yOffsetField);

            currentY += rowHeight;
        }

        // 最大宽度
        maxWidthField = new EditBox(this.font, centerX - columnWidth - columnSpacing/2, currentY, columnWidth, 20,
                Component.translatable("easysubtitles.config.max_width"));
        maxWidthField.setValue(String.valueOf(ConfigHandler.MAX_WIDTH.get()));
        addRenderableWidget(maxWidthField);

        // 字体高度
        fontSizeField = new EditBox(this.font, centerX + columnSpacing/2, currentY, columnWidth, 20,
                Component.translatable("easysubtitles.config.font_height"));
        fontSizeField.setValue(String.valueOf(ConfigHandler.FONT_HEIGHT.get()));
        addRenderableWidget(fontSizeField);

        currentY += rowHeight;

        // 背景不透明度
        opacityField = new EditBox(this.font, centerX - columnWidth - columnSpacing/2, currentY, columnWidth, 20,
                Component.translatable("easysubtitles.config.bg_opacity"));
        opacityField.setValue(String.valueOf(ConfigHandler.BACKGROUND_OPACITY.get()));
        addRenderableWidget(opacityField);

        // 整体缩放
        scaleField = new EditBox(this.font, centerX + columnSpacing/2, currentY, columnWidth, 20,
                Component.translatable("easysubtitles.config.scale"));
        scaleField.setValue(String.valueOf(ConfigHandler.SCALE.get()));
        addRenderableWidget(scaleField);

        currentY += rowHeight;

        // 文字颜色
        textColorField = new EditBox(this.font, centerX - columnWidth - columnSpacing/2, currentY, columnWidth, 20,
                Component.translatable("easysubtitles.config.text_color"));
        textColorField.setValue(Integer.toHexString(ConfigHandler.TEXT_COLOR.get()).toUpperCase());
        addRenderableWidget(textColorField);

        // 更新频率
        updateRateField = new EditBox(this.font, centerX + columnSpacing/2, currentY, columnWidth, 20,
                Component.translatable("easysubtitles.config.update_rate"));
        updateRateField.setValue(String.valueOf(ConfigHandler.UPDATE_RATE.get()));
        addRenderableWidget(updateRateField);

        currentY += rowHeight + sectionSpacing;

        // === 操作按钮 ===
        int buttonWidth = 100;
        int buttonSpacing = 10;

        // 重置按钮
        resetButton = Button.builder(Component.translatable("easysubtitles.config.reset"),
                        button -> resetToDefaults())
                .pos(centerX - buttonWidth - buttonSpacing/2, currentY)
                .size(buttonWidth, 20)
                .build();
        addRenderableWidget(resetButton);

        // 保存按钮
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE,
                        button -> saveAndClose())
                .pos(centerX + buttonSpacing/2, currentY)
                .size(buttonWidth, 20)
                .build());

        // 取消按钮
        addRenderableWidget(Button.builder(CommonComponents.GUI_CANCEL,
                        button -> onClose())
                .pos(centerX - buttonWidth/2, currentY + rowHeight + 5)
                .size(buttonWidth, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        renderBackground(gui);
        gui.drawCenteredString(this.font, this.title, this.width / 2, 10, 0xFFFFFF);

        int centerX = this.width / 2;
        int infoY = this.height - 30;

        // 显示配置提示信息
        gui.drawCenteredString(this.font,
                Component.translatable("easysubtitles.config.tips"),
                centerX, infoY, 0xCCCCCC);

        // 如果选择自定义位置，显示提示
        if (positionPresetButton != null && positionPresetButton.getValue() == ConfigHandler.PositionPreset.CUSTOM) {
            gui.drawCenteredString(this.font,
                    Component.translatable("easysubtitles.config.custom_position_tip"),
                    centerX,
                    yOffsetField.getY() - 15,
                    0xDDDDDD);
        }

        // 如果使用图片背景，显示提示
        if (useImageBgToggle != null && useImageBgToggle.getValue()) {
            gui.drawCenteredString(this.font,
                    Component.translatable("easysubtitles.config.bg_image_tip"),
                    centerX,
                    bgImagePathField.getY() - 15,
                    0xDDDDDD);
        }

        super.render(gui, mouseX, mouseY, partialTicks);
    }

    private void saveAndClose() {
        try {
            // 保存原有配置
            ConfigHandler.AUTO_SUBTITLE.set(autoSubtitleToggle.getValue());
            ConfigHandler.USE_IMAGE_BG.set(useImageBgToggle.getValue());
            ConfigHandler.BG_IMAGE_PATH.set(bgImagePathField.getValue());
            ConfigHandler.BG_SCALE.set(Double.parseDouble(bgScaleField.getValue()));
            ConfigHandler.CLOSE_ON_ESC.set(closeOnEscToggle.getValue());

            // 保存显示配置
            ConfigHandler.POSITION_PRESET.set(positionPresetButton.getValue());

            // 只有在自定义位置时才保存偏移值
            if (positionPresetButton.getValue() == ConfigHandler.PositionPreset.CUSTOM) {
                ConfigHandler.X_OFFSET.set(Double.parseDouble(xOffsetField.getValue()));
                ConfigHandler.Y_OFFSET.set(Double.parseDouble(yOffsetField.getValue()));
            }

            ConfigHandler.MAX_WIDTH.set(Integer.parseInt(maxWidthField.getValue()));
            ConfigHandler.FONT_HEIGHT.set(Integer.parseInt(fontSizeField.getValue()));
            ConfigHandler.BACKGROUND_OPACITY.set(Double.parseDouble(opacityField.getValue()));
            ConfigHandler.SHOW_BACKGROUND.set(backgroundToggle.getValue());
            ConfigHandler.SCALE.set(Double.parseDouble(scaleField.getValue()));
            ConfigHandler.ENABLE_TEXT_SHADOW.set(shadowToggle.getValue());
            ConfigHandler.TEXT_COLOR.set(Integer.parseInt(textColorField.getValue(), 16));
            ConfigHandler.UPDATE_RATE.set(Double.parseDouble(updateRateField.getValue()));

            // 保存到配置文件
            configSpec.save();

            // 应用配置到渲染器
            Minecraft.getInstance().execute(() -> {
                // 重新加载背景纹理
                if (ConfigHandler.USE_IMAGE_BG.get()) {
                    com.zhenghao123.easysubtitles.SubtitleRenderer.loadBackgroundTexture();
                }
            });

            // 显示保存成功消息
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.translatable("easysubtitles.config.saved"),
                        true
                );
            }

            // 返回之前的屏幕
            this.minecraft.setScreen(previousScreen);
        } catch (NumberFormatException e) {
            // 显示错误提示
            if (this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                        Component.translatable("easysubtitles.config.error.number_format"),
                        true
                );
            }
        } catch (IllegalArgumentException e) {
            // 显示错误提示
            if (this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                        Component.translatable("easysubtitles.config.error", e.getMessage()),
                        true
                );
            }
        }
    }

    private void resetToDefaults() {
        // 重置所有配置为默认值
        ConfigHandler.AUTO_SUBTITLE.set(ConfigHandler.AUTO_SUBTITLE.getDefault());
        ConfigHandler.USE_IMAGE_BG.set(ConfigHandler.USE_IMAGE_BG.getDefault());
        ConfigHandler.BG_IMAGE_PATH.set(ConfigHandler.BG_IMAGE_PATH.getDefault());
        ConfigHandler.BG_SCALE.set(ConfigHandler.BG_SCALE.getDefault());
        ConfigHandler.CLOSE_ON_ESC.set(ConfigHandler.CLOSE_ON_ESC.getDefault());
        ConfigHandler.POSITION_PRESET.set(ConfigHandler.POSITION_PRESET.getDefault());
        ConfigHandler.X_OFFSET.set(ConfigHandler.X_OFFSET.getDefault());
        ConfigHandler.Y_OFFSET.set(ConfigHandler.Y_OFFSET.getDefault());
        ConfigHandler.MAX_WIDTH.set(ConfigHandler.MAX_WIDTH.getDefault());
        ConfigHandler.FONT_HEIGHT.set(ConfigHandler.FONT_HEIGHT.getDefault());
        ConfigHandler.BACKGROUND_OPACITY.set(ConfigHandler.BACKGROUND_OPACITY.getDefault());
        ConfigHandler.SHOW_BACKGROUND.set(ConfigHandler.SHOW_BACKGROUND.getDefault());
        ConfigHandler.SCALE.set(ConfigHandler.SCALE.getDefault());
        ConfigHandler.ENABLE_TEXT_SHADOW.set(ConfigHandler.ENABLE_TEXT_SHADOW.getDefault());
        ConfigHandler.TEXT_COLOR.set(ConfigHandler.TEXT_COLOR.getDefault());
        ConfigHandler.UPDATE_RATE.set(ConfigHandler.UPDATE_RATE.getDefault());

        // 重新初始化界面以更新控件值
        this.minecraft.setScreen(new ConfigScreen(previousScreen, configSpec));

        // 显示重置成功消息
        if (this.minecraft.player != null) {
            this.minecraft.player.displayClientMessage(
                    Component.translatable("easysubtitles.config.reset_success"),
                    true
            );
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(previousScreen);
    }
}