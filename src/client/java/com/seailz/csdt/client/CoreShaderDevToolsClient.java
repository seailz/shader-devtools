package com.seailz.csdt.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.logging.LogUtils;
import com.seailz.csdt.client.screen.ShaderDevToolsScreen;
import com.seailz.csdt.client.service.ForcedPostEffectService;
import com.seailz.csdt.client.service.McpControlServerService;
import com.seailz.csdt.client.service.ShaderReloadService;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;

public class CoreShaderDevToolsClient implements ClientModInitializer {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final String GUI_SMOKE_PROPERTY = "csdt.smoke.openMenu";
    private static final int GUI_SMOKE_TIMEOUT_TICKS = 1_800;
    private static final int GUI_SMOKE_SETTLE_TICKS = 20;

    private static KeyMapping reloadCoreShadersKey;
    private static KeyMapping openShaderDevToolsMenuKey;
    private static boolean guiSmokeMenuOpened;
    private static boolean guiSmokeMenuRendered;
    private static int guiSmokeTicks;
    private static int guiSmokeRenderedTicks;

    @Override
    public void onInitializeClient() {
        McpControlServerService.start();

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.parse("coreshader-devtools:main"));
        reloadCoreShadersKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.coreshader-devtools.reload_core_shaders",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_R,
                category
        ));
        openShaderDevToolsMenuKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.coreshader-devtools.shader_dev_tools_menu",
                InputConstants.Type.KEYBOARD,
                InputConstants.KEY_G,
                category
        ));

        ScreenEvents.AFTER_INIT.register((client, screen, scaledWidth, scaledHeight) ->
                ScreenKeyboardEvents.allowKeyPress(screen).register((currentScreen, keyEvent) -> {
                    if (client.level == null
                            && !isShaderDevToolsScreen(currentScreen)
                            && !(currentScreen.getFocused() instanceof EditBox)
                            && openShaderDevToolsMenuKey.matches(keyEvent)) {
                        openShaderDevToolsMenu(client, currentScreen);
                        return false;
                    }
                    return true;
                })
        );
    }

    public static void onEndClientTick(Minecraft client) {
        if (Boolean.getBoolean(GUI_SMOKE_PROPERTY)) {
            runGuiSmokeTest(client);
        }

        while (reloadCoreShadersKey.consumeClick()) {
            ShaderReloadService.reloadCoreShadersOnly();
        }

        while (openShaderDevToolsMenuKey.consumeClick()) {
            openShaderDevToolsMenu(client, client.gui.screen());
        }

        ForcedPostEffectService.applyForcedPostEffect();
    }

    public static void onShaderDevToolsMenuRendered() {
        if (Boolean.getBoolean(GUI_SMOKE_PROPERTY)) {
            guiSmokeMenuRendered = true;
        }
    }

    private static void runGuiSmokeTest(Minecraft client) {
        guiSmokeTicks++;

        if (!guiSmokeMenuRendered
                && client.isGameLoadFinished()
                && (client.level != null || client.gui.screen() != null)
                && !(client.gui.screen() instanceof ShaderDevToolsScreen)) {
            if (!guiSmokeMenuOpened) {
                LOGGER.info("GUI smoke test opening the Shader DevTools menu.");
            }
            guiSmokeMenuOpened = true;
            openShaderDevToolsMenu(client, client.gui.screen());
        }

        if (guiSmokeMenuRendered) {
            guiSmokeRenderedTicks++;
            if (guiSmokeRenderedTicks >= GUI_SMOKE_SETTLE_TICKS) {
                LOGGER.info("GUI smoke test passed: Shader DevTools menu rendered for {} client ticks.", GUI_SMOKE_SETTLE_TICKS);
                client.stop();
            }
            return;
        }

        if (guiSmokeTicks >= GUI_SMOKE_TIMEOUT_TICKS) {
            LOGGER.error("GUI smoke test failed: Shader DevTools menu did not render within {} client ticks.", GUI_SMOKE_TIMEOUT_TICKS);
            client.stop();
        }
    }

    private static void openShaderDevToolsMenu(Minecraft client, Screen parent) {
        if (parent instanceof ShaderDevToolsScreen) {
            return;
        }
        client.gui.setScreen(new ShaderDevToolsScreen(parent));
    }

    private static boolean isShaderDevToolsScreen(Screen screen) {
        return screen != null && "com.seailz.csdt.client.screen".equals(screen.getClass().getPackageName());
    }
}
