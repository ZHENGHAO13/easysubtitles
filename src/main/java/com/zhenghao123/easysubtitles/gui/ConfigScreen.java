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

    private CycleButton<ConfigHandler.PositionPreset> positionPresetButton;
    private EditBox xOffsetField;
    private EditBox yOffsetField;
    private EditBox maxWidthField;
    private EditBox fontSizeField;
    private EditBox opacityField;
    private CycleButton<Boolean> backgroundToggle;
    private EditBox scaleField;
    private CycleButton<Boolean> shadowToggle;
    private EditBox updateRateField;
    private EditBox textColorField;

    public ConfigScreen(Screen previousScreen, ForgeConfigSpec configSpec) {
        super(Component.literal("EasySubtitles Configuration"));
        this.previousScreen = previousScreen;
        this.configSpec = configSpec;
    }

    @Override
    protected void init() {
        super.init();

        int centerX = this.width / 2;
        int startY = this.height / 4 - 15;
        int rowHeight = 22;
        int sectionSpacing = 10;

        // === 原有配置部分 ===
        addRenderableWidget(Button.builder(Component.literal("Original Settings"), button -> {})
                .pos(centerX - 100, startY - 25)
                .size(200, 20)
                .build());

        // 自动字幕
        autoSubtitleToggle = CycleButton.onOffBuilder(ConfigHandler.AUTO_SUBTITLE.get())
                .create(centerX - 150, startY, 145, 20,
                        Component.literal("Auto Subtitles"),
                        (button, value) -> {});
        addRenderableWidget(autoSubtitleToggle);

        // 使用图片背景
        useImageBgToggle = CycleButton.onOffBuilder(ConfigHandler.USE_IMAGE_BG.get())
                .create(centerX + 5, startY, 145, 20,
                        Component.literal("Use Image BG"),
                        (button, value) -> {});
        addRenderableWidget(useImageBgToggle);

        // 背景图片路径
        bgImagePathField = new EditBox(this.font, centerX - 150, startY + rowHeight, 300, 20,
                Component.literal("BG Image Path"));
        bgImagePathField.setValue(ConfigHandler.BG_IMAGE_PATH.get());
        addRenderableWidget(bgImagePathField);

        // 背景缩放
        bgScaleField = new EditBox(this.font, centerX - 150, startY + rowHeight * 2, 145, 20,
                Component.literal("BG Scale"));
        bgScaleField.setValue(String.valueOf(ConfigHandler.BG_SCALE.get()));
        addRenderableWidget(bgScaleField);

        startY += rowHeight * 3 + sectionSpacing;

        // === 新增显示配置部分 ===
        addRenderableWidget(Button.builder(Component.literal("Display Settings"), button -> {})
                .pos(centerX - 100, startY - 25)
                .size(200, 20)
                .build());

        // 位置预设
        positionPresetButton = CycleButton.<ConfigHandler.PositionPreset>builder(value ->
                        Component.literal(value.name()))
                .withValues(ConfigHandler.PositionPreset.values())
                .create(centerX - 150, startY, 300, 20,
                        Component.translatable("Position Preset"),
                        (button, value) -> {});
        positionPresetButton.setValue(ConfigHandler.POSITION_PRESET.get());
        addRenderableWidget(positionPresetButton);

        // 自定义位置X偏移
        xOffsetField = new EditBox(this.font, centerX - 150, startY + rowHeight, 145, 20,
                Component.literal("X Offset"));
        xOffsetField.setValue(String.valueOf(ConfigHandler.X_OFFSET.get()));
        addRenderableWidget(xOffsetField);

        // 自定义位置Y偏移
        yOffsetField = new EditBox(this.font, centerX + 5, startY + rowHeight, 145, 20,
                Component.literal("Y Offset"));
        yOffsetField.setValue(String.valueOf(ConfigHandler.Y_OFFSET.get()));
        addRenderableWidget(yOffsetField);

        // 最大宽度
        maxWidthField = new EditBox(this.font, centerX - 150, startY + rowHeight * 2, 145, 20,
                Component.literal("Max Width"));
        maxWidthField.setValue(String.valueOf(ConfigHandler.MAX_WIDTH.get()));
        addRenderableWidget(maxWidthField);

        // 字体高度
        fontSizeField = new EditBox(this.font, centerX + 5, startY + rowHeight * 2, 145, 20,
                Component.literal("Font Height"));
        fontSizeField.setValue(String.valueOf(ConfigHandler.FONT_HEIGHT.get()));
        addRenderableWidget(fontSizeField);

        // 背景不透明度
        opacityField = new EditBox(this.font, centerX - 150, startY + rowHeight * 3, 145, 20,
                Component.literal("BG Opacity"));
        opacityField.setValue(String.valueOf(ConfigHandler.BACKGROUND_OPACITY.get()));
        addRenderableWidget(opacityField);

        // 显示背景
        backgroundToggle = CycleButton.onOffBuilder(ConfigHandler.SHOW_BACKGROUND.get())
                .create(centerX + 5, startY + rowHeight * 3, 145, 20,
                        Component.literal("Show BG"),
                        (button, value) -> {});
        addRenderableWidget(backgroundToggle);

        // 缩放
        scaleField = new EditBox(this.font, centerX - 150, startY + rowHeight * 4, 145, 20,
                Component.literal("Scale"));
        scaleField.setValue(String.valueOf(ConfigHandler.SCALE.get()));
        addRenderableWidget(scaleField);

        // 文字阴影
        shadowToggle = CycleButton.onOffBuilder(ConfigHandler.ENABLE_TEXT_SHADOW.get())
                .create(centerX + 5, startY + rowHeight * 4, 145, 20,
                        Component.literal("Text Shadow"),
                        (button, value) -> {});
        addRenderableWidget(shadowToggle);

        // 文字颜色
        textColorField = new EditBox(this.font, centerX - 150, startY + rowHeight * 5, 145, 20,
                Component.literal("Text Color"));
        textColorField.setValue(Integer.toHexString(ConfigHandler.TEXT_COLOR.get()).toUpperCase());
        addRenderableWidget(textColorField);

        // 更新频率
        updateRateField = new EditBox(this.font, centerX + 5, startY + rowHeight * 5, 145, 20,
                Component.literal("Update Rate"));
        updateRateField.setValue(String.valueOf(ConfigHandler.UPDATE_RATE.get()));
        addRenderableWidget(updateRateField);

        // 保存按钮
        addRenderableWidget(Button.builder(CommonComponents.GUI_DONE, (button) -> saveAndClose())
                .pos(centerX - 75, startY + rowHeight * 7)
                .size(150, 20)
                .build());
    }

    @Override
    public void render(GuiGraphics gui, int mouseX, int mouseY, float partialTicks) {
        renderBackground(gui);
        gui.drawCenteredString(this.font, this.title, this.width / 2, 20, 0xFFFFFF);

        // 如果选择自定义位置，显示X/Y偏移字段的背景提示
        if (positionPresetButton.getValue() == ConfigHandler.PositionPreset.CUSTOM) {
            gui.drawCenteredString(this.font,
                    Component.literal("Custom Position Settings:"),
                    this.width / 2,
                    yOffsetField.getY() - 25,
                    0xDDDDDD);
        }

        // 如果使用图片背景，显示相关字段的背景提示
        if (useImageBgToggle.getValue()) {
            gui.drawCenteredString(this.font,
                    Component.literal("Background Image Settings:"),
                    this.width / 2,
                    bgImagePathField.getY() - 25,
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

            // 保存显示配置
            ConfigHandler.POSITION_PRESET.set(positionPresetButton.getValue());
            ConfigHandler.X_OFFSET.set(Double.parseDouble(xOffsetField.getValue()));
            ConfigHandler.Y_OFFSET.set(Double.parseDouble(yOffsetField.getValue()));
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
                // 重新加载背景纹理（现在可以直接调用）
                if (ConfigHandler.USE_IMAGE_BG.get()) {
                    com.zhenghao123.easysubtitles.SubtitleRenderer.loadBackgroundTexture();
                }
            });

            // 显示保存成功消息
            if (Minecraft.getInstance().player != null) {
                Minecraft.getInstance().player.displayClientMessage(
                        Component.literal("EasySubtitles configuration saved!"),
                        true
                );
            }

            // 返回之前的屏幕
            this.minecraft.setScreen(previousScreen);
        } catch (NumberFormatException e) {
            // 显示错误提示
            if (this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                        Component.literal("Error: Invalid number format in one of the fields!"),
                        true
                );
            }
        } catch (IllegalArgumentException e) {
            // 显示错误提示
            if (this.minecraft.player != null) {
                this.minecraft.player.displayClientMessage(
                        Component.literal("Error: " + e.getMessage()),
                        true
                );
            }
        }
    }

    @Override
    public void onClose() {
        this.minecraft.setScreen(previousScreen);
    }
}