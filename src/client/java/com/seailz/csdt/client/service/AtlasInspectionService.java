package com.seailz.csdt.client.service;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;

import java.util.concurrent.CompletableFuture;

/**
 * Debug-only read access to atlas placement. Resource-pack shaders cannot use
 * this service; it exists solely to measure deterministic atlas coordinates
 * while developing vanilla shader probes.
 */
public final class AtlasInspectionService {

    private AtlasInspectionService() {
    }

    public static CompletableFuture<Result> inspectBlockSprite(String spriteId) {
        CompletableFuture<Result> result = new CompletableFuture<>();
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> {
            try {
                AbstractTexture texture = minecraft.getTextureManager()
                        .getTexture(TextureAtlas.LOCATION_BLOCKS);
                if (!(texture instanceof TextureAtlas atlas)) {
                    throw new IllegalStateException("Block atlas texture has unexpected type: "
                            + texture.getClass().getName());
                }

                Identifier id = Identifier.parse(spriteId);
                TextureAtlasSprite sprite = atlas.getSprite(id);
                int atlasWidth = atlas.getTexture().getWidth(0);
                int atlasHeight = atlas.getTexture().getHeight(0);
                result.complete(new Result(
                        id.toString(),
                        atlasWidth,
                        atlasHeight,
                        sprite.getX(),
                        sprite.getY(),
                        Math.round((sprite.getU1() - sprite.getU0()) * atlasWidth),
                        Math.round((sprite.getV1() - sprite.getV0()) * atlasHeight),
                        sprite.getU0(),
                        sprite.getV0(),
                        sprite.getU1(),
                        sprite.getV1(),
                        sprite.isAnimated()));
            } catch (Exception exception) {
                exception.printStackTrace();
                result.completeExceptionally(exception);
            }
        });
        return result;
    }

    public record Result(
            String id,
            int atlasWidth,
            int atlasHeight,
            int x,
            int y,
            int width,
            int height,
            float u0,
            float v0,
            float u1,
            float v1,
            boolean animated) {
    }
}
