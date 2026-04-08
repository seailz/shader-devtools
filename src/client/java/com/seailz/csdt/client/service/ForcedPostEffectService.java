package com.seailz.csdt.client.service;

import com.seailz.csdt.client.mixins.GameRendererAccess;
import net.minecraft.client.Minecraft;
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
            Minecraft.getInstance().gameRenderer.clearPostEffect();
            ClientToastService.showInfo("Post effect released", postEffectId.toString());
            return;
        }

        forcedPostEffectId = postEffectId;
        applyForcedPostEffect();
        ClientToastService.showInfo("Post effect forced", postEffectId.toString());
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

        ((GameRendererAccess) minecraft.gameRenderer).csdt$setPostEffect(forcedPostEffectId);
    }

    public static void clearForcedPostEffect() {
        forcedPostEffectId = null;
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.gameRenderer != null) {
            minecraft.gameRenderer.clearPostEffect();
        }
    }
}
