package com.seailz.csdt.client.service;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.FileToIdConverter;
import net.minecraft.resources.Identifier;

public final class ForcedPostEffectService {

    private static final FileToIdConverter POST_EFFECT_ID_CONVERTER = FileToIdConverter.json("post_effect");
    private static Identifier forcedPostEffectId;

    private ForcedPostEffectService() {
    }

    public static void toggleForcedPostEffect(String resourcePath) {
        Identifier resourceId = Identifier.parse(resourcePath);
        Identifier postEffectId = POST_EFFECT_ID_CONVERTER.fileToId(resourceId);
        if (postEffectId.equals(forcedPostEffectId)) {
            forcedPostEffectId = null;
            removeRequestedPostEffect(Minecraft.getInstance().gameRenderer, postEffectId);
            ClientToastService.showInfo("Post effect released", postEffectId.toString());
            return;
        }

        forcedPostEffectId = postEffectId;
        applyForcedPostEffect();
        ClientToastService.showInfo("Post effect forced", postEffectId.toString());
    }

    public static void setForcedPostEffect(String resourcePath, boolean enabled) {
        if (!enabled) {
            clearForcedPostEffect();
            return;
        }

        Identifier resourceId = Identifier.parse(resourcePath);
        forcedPostEffectId = POST_EFFECT_ID_CONVERTER.fileToId(resourceId);
        applyForcedPostEffect();
        ClientToastService.showInfo("Post effect forced", forcedPostEffectId.toString());
    }

    public static boolean isForced(String resourcePath) {
        Identifier resourceId = Identifier.parse(resourcePath);
        return POST_EFFECT_ID_CONVERTER.fileToId(resourceId).equals(forcedPostEffectId);
    }

    public static void applyForcedPostEffect() {
        if (forcedPostEffectId == null) {
            return;
        }

        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null) {
            return;
        }

        appendForcedPostEffect(minecraft.gameRenderer);
    }

    public static void appendForcedPostEffect(GameRenderer gameRenderer) {
        if (forcedPostEffectId == null || gameRenderer == null) {
            return;
        }
        if (!gameRenderer.getRequestedPostEffects().contains(forcedPostEffectId)) {
            gameRenderer.getRequestedPostEffects().add(forcedPostEffectId);
        }
    }

    public static void clearForcedPostEffect() {
        Identifier postEffectId = forcedPostEffectId;
        forcedPostEffectId = null;
        Minecraft minecraft = Minecraft.getInstance();
        if (postEffectId != null && minecraft.gameRenderer != null) {
            removeRequestedPostEffect(minecraft.gameRenderer, postEffectId);
        }
    }

    private static void removeRequestedPostEffect(GameRenderer gameRenderer, Identifier postEffectId) {
        if (gameRenderer != null && postEffectId != null) {
            gameRenderer.getRequestedPostEffects().removeIf(postEffectId::equals);
        }
    }
}
