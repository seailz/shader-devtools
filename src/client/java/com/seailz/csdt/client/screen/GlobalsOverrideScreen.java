package com.seailz.csdt.client.screen;

import com.seailz.csdt.client.service.ClientToastService;
import com.seailz.csdt.client.state.GlobalsOverrideState;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

public final class GlobalsOverrideScreen extends Screen {

    private final Screen parent;

    private EditBox screenWidthBox;
    private EditBox screenHeightBox;
    private EditBox glintAlphaBox;
    private EditBox gameTimeBox;
    private EditBox menuBlurRadiusBox;
    private EditBox cameraXBox;
    private EditBox cameraYBox;
    private EditBox cameraZBox;

    private boolean screenWidthEnabled;
    private boolean screenHeightEnabled;
    private boolean glintAlphaEnabled;
    private boolean gameTimeEnabled;
    private boolean menuBlurRadiusEnabled;
    private boolean cameraPosEnabled;
    private boolean useRgssEnabled;
    private boolean useRgssValue;

    private Button screenWidthToggle;
    private Button screenHeightToggle;
    private Button glintAlphaToggle;
    private Button gameTimeToggle;
    private Button menuBlurRadiusToggle;
    private Button cameraPosToggle;
    private Button doneButton;

    public GlobalsOverrideScreen(Screen parent) {
        super(Component.literal("Globals Overrides"));
        this.parent = parent;

        GlobalsOverrideState state = GlobalsOverrideState.getInstance();
        this.screenWidthEnabled = state.getScreenWidth().isEnabled();
        this.screenHeightEnabled = state.getScreenHeight().isEnabled();
        this.glintAlphaEnabled = state.getGlintAlpha().isEnabled();
        this.gameTimeEnabled = state.getGameTime().isEnabled();
        this.menuBlurRadiusEnabled = state.getMenuBlurRadius().isEnabled();
        this.cameraPosEnabled = state.getCameraPos().isEnabled();
        this.useRgssEnabled = state.getUseRgss().isEnabled();
        this.useRgssValue = Boolean.TRUE.equals(state.getUseRgss().override());
    }

    @Override
    protected void init() {
        int left = this.width / 2 - 155;
        int valueLeft = left + 150;
        int y = 32;
        int rowHeight = 24;

        this.screenWidthToggle = this.addRenderableWidget(toggleButton(left, y, 145, () -> this.screenWidthEnabled, "Screen Width", value -> this.screenWidthEnabled = value));
        this.screenWidthBox = this.addRenderableWidget(numberBox(valueLeft, y, 160, currentInt(GlobalsOverrideState.getInstance().getScreenWidth().override()), "window width"));
        y += rowHeight;

        this.screenHeightToggle = this.addRenderableWidget(toggleButton(left, y, 145, () -> this.screenHeightEnabled, "Screen Height", value -> this.screenHeightEnabled = value));
        this.screenHeightBox = this.addRenderableWidget(numberBox(valueLeft, y, 160, currentInt(GlobalsOverrideState.getInstance().getScreenHeight().override()), "window height"));
        y += rowHeight;

        this.glintAlphaToggle = this.addRenderableWidget(toggleButton(left, y, 145, () -> this.glintAlphaEnabled, "Glint Alpha", value -> this.glintAlphaEnabled = value));
        this.glintAlphaBox = this.addRenderableWidget(numberBox(valueLeft, y, 160, currentDouble(GlobalsOverrideState.getInstance().getGlintAlpha().override()), "double"));
        y += rowHeight;

        this.gameTimeToggle = this.addRenderableWidget(toggleButton(left, y, 145, () -> this.gameTimeEnabled, "Game Time", value -> this.gameTimeEnabled = value));
        this.gameTimeBox = this.addRenderableWidget(numberBox(valueLeft, y, 160, currentLong(GlobalsOverrideState.getInstance().getGameTime().override()), "0-23999"));
        y += rowHeight;

        this.menuBlurRadiusToggle = this.addRenderableWidget(toggleButton(left, y, 145, () -> this.menuBlurRadiusEnabled, "Menu Blur", value -> this.menuBlurRadiusEnabled = value));
        this.menuBlurRadiusBox = this.addRenderableWidget(numberBox(valueLeft, y, 160, currentInt(GlobalsOverrideState.getInstance().getMenuBlurRadius().override()), "radius"));
        y += rowHeight;

        this.cameraPosToggle = this.addRenderableWidget(toggleButton(left, y, 145, () -> this.cameraPosEnabled, "Camera Pos", value -> this.cameraPosEnabled = value));
        Vec3 cameraPos = GlobalsOverrideState.getInstance().getCameraPos().override();
        this.cameraXBox = this.addRenderableWidget(numberBox(valueLeft, y, 50, cameraPos == null ? "" : Double.toString(cameraPos.x), "x"));
        this.cameraYBox = this.addRenderableWidget(numberBox(valueLeft + 55, y, 50, cameraPos == null ? "" : Double.toString(cameraPos.y), "y"));
        this.cameraZBox = this.addRenderableWidget(numberBox(valueLeft + 110, y, 50, cameraPos == null ? "" : Double.toString(cameraPos.z), "z"));
        y += rowHeight;

        this.addRenderableWidget(toggleButton(left, y, 145, () -> this.useRgssEnabled, "Use RGSS", value -> this.useRgssEnabled = value));
        this.addRenderableWidget(CycleButton.onOffBuilder(this.useRgssValue).create(
                valueLeft,
                y,
                160,
                20,
                Component.literal("RGSS Value"),
                (button, value) -> this.useRgssValue = value
        ));

        int bottomY = this.height - 28;
        this.addRenderableWidget(Button.builder(Component.literal("Apply"), button -> {
            this.applyOverrides();
            ClientToastService.showOverridesApplied(GlobalsOverrideState.getInstance().describeActiveOverrides().size());
            this.refreshDoneButton();
        }).bounds(left, bottomY, 95, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Reset All"), button -> {
            GlobalsOverrideState.getInstance().clearAll();
            ClientToastService.showOverridesCleared();
            this.minecraft.gui.setScreen(new GlobalsOverrideScreen(this.parent));
        }).bounds(left + 105, bottomY, 95, 20).build());
        this.doneButton = this.addRenderableWidget(Button.builder(Component.literal("Done"), button -> {
            this.applyOverrides();
            ClientToastService.showOverridesApplied(GlobalsOverrideState.getInstance().describeActiveOverrides().size());
            this.onClose();
        }).bounds(left + 210, bottomY, 100, 20).build());

        this.refreshDoneButton();
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(this.parent);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private void applyOverrides() {
        GlobalsOverrideState state = GlobalsOverrideState.getInstance();

        state.setScreenWidth(this.screenWidthEnabled ? override(parseInteger(this.screenWidthBox.getValue())) : GlobalsOverrideState.GlobalOverride.disabled());
        state.setScreenHeight(this.screenHeightEnabled ? override(parseInteger(this.screenHeightBox.getValue())) : GlobalsOverrideState.GlobalOverride.disabled());
        state.setGlintAlpha(this.glintAlphaEnabled ? override(parseDouble(this.glintAlphaBox.getValue())) : GlobalsOverrideState.GlobalOverride.disabled());

        Long gameTime = parseGameTime(this.gameTimeBox.getValue());
        state.setGameTime(this.gameTimeEnabled && gameTime != null
                ? override(gameTime)
                : GlobalsOverrideState.GlobalOverride.disabled());

        state.setMenuBlurRadius(this.menuBlurRadiusEnabled ? override(parseInteger(this.menuBlurRadiusBox.getValue())) : GlobalsOverrideState.GlobalOverride.disabled());

        Vec3 cameraPos = parseVec3(this.cameraXBox.getValue(), this.cameraYBox.getValue(), this.cameraZBox.getValue());
        state.setCameraPos(this.cameraPosEnabled && cameraPos != null ? override(cameraPos) : GlobalsOverrideState.GlobalOverride.disabled());

        state.setUseRgss(this.useRgssEnabled ? override(this.useRgssValue) : GlobalsOverrideState.GlobalOverride.disabled());
    }

    private void refreshDoneButton() {
        this.doneButton.active = this.isValidInput();
        this.screenWidthToggle.setMessage(toggleLabel("Screen Width", this.screenWidthEnabled));
        this.screenHeightToggle.setMessage(toggleLabel("Screen Height", this.screenHeightEnabled));
        this.glintAlphaToggle.setMessage(toggleLabel("Glint Alpha", this.glintAlphaEnabled));
        this.gameTimeToggle.setMessage(toggleLabel("Game Time", this.gameTimeEnabled));
        this.menuBlurRadiusToggle.setMessage(toggleLabel("Menu Blur", this.menuBlurRadiusEnabled));
        this.cameraPosToggle.setMessage(toggleLabel("Camera Pos", this.cameraPosEnabled));
    }

    private boolean isValidInput() {
        return (!this.screenWidthEnabled || parseInteger(this.screenWidthBox.getValue()) != null)
                && (!this.screenHeightEnabled || parseInteger(this.screenHeightBox.getValue()) != null)
                && (!this.glintAlphaEnabled || parseDouble(this.glintAlphaBox.getValue()) != null)
                && (!this.gameTimeEnabled || parseGameTime(this.gameTimeBox.getValue()) != null)
                && (!this.menuBlurRadiusEnabled || parseInteger(this.menuBlurRadiusBox.getValue()) != null)
                && (!this.cameraPosEnabled || parseVec3(this.cameraXBox.getValue(), this.cameraYBox.getValue(), this.cameraZBox.getValue()) != null);
    }

    private Button toggleButton(int x, int y, int width, java.util.function.BooleanSupplier currentValue, String label, java.util.function.Consumer<Boolean> setter) {
        return Button.builder(toggleLabel(label, currentValue.getAsBoolean()), button -> {
            setter.accept(!currentValue.getAsBoolean());
            this.refreshDoneButton();
        }).bounds(x, y, width, 20).build();
    }

    private EditBox numberBox(int x, int y, int width, String value, String hint) {
        EditBox box = new EditBox(this.font, x, y, width, 20, Component.empty());
        box.setHint(Component.literal(hint));
        box.setValue(value);
        box.setResponder(text -> this.refreshDoneButton());
        return box;
    }

    private static <T> GlobalsOverrideState.GlobalOverride<T> override(T value) {
        return new GlobalsOverrideState.GlobalOverride<>(value);
    }

    private static Component toggleLabel(String name, boolean enabled) {
        return Component.literal(name + ": " + (enabled ? "On" : "Off"));
    }

    private static String currentInt(Integer value) {
        return value == null ? "" : Integer.toString(value);
    }

    private static String currentDouble(Double value) {
        return value == null ? "" : Double.toString(value);
    }

    private static String currentLong(Long value) {
        return value == null ? "" : Long.toString(value);
    }

    private static Integer parseInteger(String value) {
        try {
            return value.isBlank() ? null : Integer.parseInt(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Long parseGameTime(String value) {
        try {
            if (value.isBlank()) {
                return null;
            }
            long parsed = Long.parseLong(value.trim());
            return parsed >= 0L && parsed <= 23999L ? parsed : null;
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Double parseDouble(String value) {
        try {
            return value.isBlank() ? null : Double.parseDouble(value.trim());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private static Vec3 parseVec3(String x, String y, String z) {
        Double xValue = parseDouble(x);
        Double yValue = parseDouble(y);
        Double zValue = parseDouble(z);
        if (xValue == null || yValue == null || zValue == null) {
            return null;
        }
        return new Vec3(xValue, yValue, zValue);
    }
}
