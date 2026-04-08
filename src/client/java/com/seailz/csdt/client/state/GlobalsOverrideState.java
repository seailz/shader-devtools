package com.seailz.csdt.client.state;

import lombok.Getter;
import lombok.Setter;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Handles state for force-overriding values in the Globals uniform.
 */
@Getter
@Setter
public final class GlobalsOverrideState {

    private static final GlobalsOverrideState INSTANCE = new GlobalsOverrideState();

    private GlobalOverride<Integer> screenWidth = GlobalOverride.disabled();
    private GlobalOverride<Integer> screenHeight = GlobalOverride.disabled();
    private GlobalOverride<Double> glintAlpha = GlobalOverride.disabled();
    private GlobalOverride<Long> gameTime = GlobalOverride.disabled();
    private GlobalOverride<Integer> menuBlurRadius = GlobalOverride.disabled();
    private GlobalOverride<Vec3> cameraPos = GlobalOverride.disabled();
    private GlobalOverride<Boolean> useRgss = GlobalOverride.disabled();

    private GlobalsOverrideState() {
    }

    public static GlobalsOverrideState getInstance() {
        return INSTANCE;
    }

    public void clearAll() {
        this.screenWidth = GlobalOverride.disabled();
        this.screenHeight = GlobalOverride.disabled();
        this.glintAlpha = GlobalOverride.disabled();
        this.gameTime = GlobalOverride.disabled();
        this.menuBlurRadius = GlobalOverride.disabled();
        this.cameraPos = GlobalOverride.disabled();
        this.useRgss = GlobalOverride.disabled();
    }

    public List<String> describeActiveOverrides() {
        List<String> lines = new ArrayList<>();
        this.addIfEnabled(lines, "Screen Width", this.screenWidth.override());
        this.addIfEnabled(lines, "Screen Height", this.screenHeight.override());
        this.addIfEnabled(lines, "Glint Alpha", this.glintAlpha.override());
        this.addIfEnabled(lines, "Game Time", this.gameTime.override());
        this.addIfEnabled(lines, "Menu Blur", this.menuBlurRadius.override());
        if (this.cameraPos.isEnabled() && this.cameraPos.override() != null) {
            Vec3 pos = this.cameraPos.override();
            lines.add("Camera Pos: %.2f, %.2f, %.2f".formatted(pos.x, pos.y, pos.z));
        }
        this.addIfEnabled(lines, "Use RGSS", this.useRgss.override());
        return lines;
    }

    private void addIfEnabled(List<String> lines, String name, @Nullable Object value) {
        if (value != null) {
            lines.add(name + ": " + value);
        }
    }

    /**
     * Represents an override of a value in the Globals uniform.
     */
    public record GlobalOverride<T>(@Nullable T override) {

        public static <T> GlobalOverride<T> disabled() {
            return new GlobalOverride<>(null);
        }

        public boolean isEnabled() {
            return this.override != null;
        }
    }

}
