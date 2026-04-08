package com.seailz.csdt.client.service;

import com.seailz.csdt.client.state.GlobalsOverrideState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.world.phys.Vec3;

/**
 * Resolves Globals uniform values against the current override state.
 */
public final class GlobalsUniformOverrideService {

    private GlobalsUniformOverrideService() {
    }

    public static ResolvedGlobalsUniform resolve(
            int width,
            int height,
            double glintAlpha,
            long gameTime,
            DeltaTracker deltaTracker,
            int menuBlurRadius,
            Vec3 cameraPos,
            boolean useRgss
    ) {
        GlobalsOverrideState state = GlobalsOverrideState.getInstance();
        return new ResolvedGlobalsUniform(
                pick(state.getScreenWidth(), width),
                pick(state.getScreenHeight(), height),
                pick(state.getGlintAlpha(), glintAlpha),
                pick(state.getGameTime(), gameTime),
                deltaTracker,
                pick(state.getMenuBlurRadius(), menuBlurRadius),
                pick(state.getCameraPos(), cameraPos),
                pick(state.getUseRgss(), useRgss)
        );
    }

    private static <T> T pick(GlobalsOverrideState.GlobalOverride<T> override, T fallback) {
        return override.isEnabled() ? override.override() : fallback;
    }

    public record ResolvedGlobalsUniform(
            int width,
            int height,
            double glintAlpha,
            long gameTime,
            DeltaTracker deltaTracker,
            int menuBlurRadius,
            Vec3 cameraPos,
            boolean useRgss
    ) {
    }
}
