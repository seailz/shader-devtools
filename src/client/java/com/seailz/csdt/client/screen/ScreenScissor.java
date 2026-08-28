package com.seailz.csdt.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;

final class ScreenScissor {

    private ScreenScissor() {
    }

    static boolean enableIfNonEmpty(GuiGraphicsExtractor guiGraphics, int left, int top, int right, int bottom) {
        int screenWidth = Minecraft.getInstance().getWindow().getGuiScaledWidth();
        int screenHeight = Minecraft.getInstance().getWindow().getGuiScaledHeight();
        int clippedLeft = Math.clamp(left, 0, screenWidth);
        int clippedTop = Math.clamp(top, 0, screenHeight);
        int clippedRight = Math.clamp(right, 0, screenWidth);
        int clippedBottom = Math.clamp(bottom, 0, screenHeight);
        if (clippedRight <= clippedLeft || clippedBottom <= clippedTop) {
            return false;
        }

        guiGraphics.enableScissor(clippedLeft, clippedTop, clippedRight, clippedBottom);
        return true;
    }
}
