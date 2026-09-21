package com.seailz.csdt.client.screen;

import com.seailz.csdt.client.CoreShaderDevToolsClient;
import com.seailz.csdt.client.service.ShaderDebugInfoService;
import com.seailz.csdt.client.service.ShaderReloadService;
import com.seailz.csdt.client.state.GlobalsOverrideState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

public final class ShaderDevToolsScreen extends Screen {

    private final Screen parent;

    public ShaderDevToolsScreen(Screen parent) {
        super(Component.translatable("screen.coreshader-devtools.shader_dev_tools"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        if (this.width <= 0 || this.height <= 0) {
            return;
        }

        int left = Math.max(0, this.width / 2 - 155);
        int buttonWidth = Math.min(150, this.width - left);
        int doneHeight = Math.min(20, this.height);
        int doneTop = Math.max(0, this.height - doneHeight - 8);
        int contentTop = Math.min(34, Math.max(0, this.height / 8));
        int contentBottom = Math.max(contentTop, doneTop - 4);
        int availableHeight = contentBottom - contentTop;
        int menuButtonCount = 9;
        int groupGap = availableHeight >= 260 ? 24 : 0;
        int compactAvailableHeight = availableHeight - groupGap * 2;

        if (compactAvailableHeight < menuButtonCount) {
            this.addDoneButton(doneTop, doneHeight);
            return;
        }

        int buttonHeight = Math.min(20, Math.max(1, (compactAvailableHeight - (menuButtonCount - 1)) / menuButtonCount));
        int gap = Math.min(4, (compactAvailableHeight - buttonHeight * menuButtonCount) / (menuButtonCount - 1));
        int top = contentTop;

        this.addRenderableWidget(Button.builder(Component.translatable("button.coreshader-devtools.shader_inventory"), button ->
                this.minecraft.gui.setScreen(new ShaderInventoryScreen(this))
        ).bounds(left, top, buttonWidth, buttonHeight).build());
        top += buttonHeight + gap;
        this.addRenderableWidget(Button.builder(Component.translatable("button.coreshader-devtools.uniform_inspector"), button ->
                this.minecraft.gui.setScreen(new UniformInspectorScreen(this))
        ).bounds(left, top, buttonWidth, buttonHeight).build());
        top += buttonHeight + gap;
        this.addRenderableWidget(Button.builder(Component.translatable("button.coreshader-devtools.sampler_inspector"), button ->
                this.minecraft.gui.setScreen(new SamplerInspectorScreen(this))
        ).bounds(left, top, buttonWidth, buttonHeight).build());
        top += buttonHeight + gap;
        this.addRenderableWidget(Button.builder(Component.translatable("button.coreshader-devtools.globals_overrides"), button ->
                this.minecraft.gui.setScreen(new GlobalsOverrideScreen(this))
        ).bounds(left, top, buttonWidth, buttonHeight).build());
        top += buttonHeight + gap;
        this.addRenderableWidget(Button.builder(Component.translatable("button.coreshader-devtools.log_viewer"), button ->
                this.minecraft.gui.setScreen(new LogViewerScreen(this))
        ).bounds(left, top, buttonWidth, buttonHeight).build());
        top += buttonHeight + gap;

        top += groupGap;

        this.addRenderableWidget(Button.builder(Component.translatable("button.coreshader-devtools.reload_core"), button ->
                ShaderReloadService.reloadCoreShadersOnly()
        ).bounds(left, top, buttonWidth, buttonHeight).build());
        top += buttonHeight + gap;

        this.addRenderableWidget(Button.builder(Component.translatable("button.coreshader-devtools.reload_post"), button ->
                ShaderReloadService.reloadPostShadersOnly()
        ).bounds(left, top, buttonWidth, buttonHeight).build());
        top += buttonHeight + gap;

        this.addRenderableWidget(Button.builder(Component.translatable("button.coreshader-devtools.reload_all"), button ->
                ShaderReloadService.reloadAllShadersFromHub()
        ).bounds(left, top, buttonWidth, buttonHeight).build());
        top += buttonHeight + gap;

        top += groupGap;

        this.addRenderableWidget(Button.builder(Component.translatable("button.coreshader-devtools.reset_overrides"), button ->
                GlobalsOverrideState.getInstance().clearAll()
        ).bounds(left, top, buttonWidth, buttonHeight).build());

        this.addDoneButton(doneTop, doneHeight);
    }

    private void addDoneButton(int top, int height) {
        int width = Math.min(100, this.width);
        this.addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> this.onClose())
                .bounds((this.width - width) / 2, top, width, height)
                .build());
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor guiGraphics, int mouseX, int mouseY, float partialTick) {
        guiGraphics.fill(0, 0, this.width, this.height, 0xB010141A);
        super.extractRenderState(guiGraphics, mouseX, mouseY, partialTick);

        guiGraphics.centeredText(this.font, this.title, this.width / 2, 12, 0xFFFFFFFF);

        int debugLeft = this.width / 2 + 10;
        int y = 36;
        List<String> lines = ShaderDebugInfoService.buildDebugLines(Minecraft.getInstance());
        for (String line : lines) {
            if (y + this.font.lineHeight > this.height) {
                break;
            }
            int color = line.startsWith("[") ? 0xFFFFD166 : 0xFFE0E0E0;
            guiGraphics.text(this.font, line, debugLeft, y, color, false);
            y += 10;
        }

        CoreShaderDevToolsClient.onShaderDevToolsMenuRendered();
    }
}
