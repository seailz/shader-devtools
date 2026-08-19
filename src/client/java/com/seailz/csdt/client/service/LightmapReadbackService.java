package com.seailz.csdt.client.service;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.concurrent.CompletableFuture;

public final class LightmapReadbackService {

    private static final Logger LOGGER = LogUtils.getLogger();

    private LightmapReadbackService() {
    }

    public static CompletableFuture<SamplerInspectionService.ReadbackResult> capture() {
        CompletableFuture<SamplerInspectionService.ReadbackResult> future = new CompletableFuture<>();
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> captureOnRenderThread(future));
        return future;
    }

    private static void captureOnRenderThread(
            CompletableFuture<SamplerInspectionService.ReadbackResult> future) {
        try {
            SamplerInspectionService.SamplerBindingSnapshot lightmap =
                    SamplerInspectionService.snapshotBindings().stream()
                            .filter(snapshot -> snapshot.lightmapLike()
                                    && snapshot.width() == 16
                                    && snapshot.height() == 16
                                    && snapshot.copySrc()
                                    && !snapshot.textureClosed())
                            .max(Comparator.comparingLong(
                                    SamplerInspectionService.SamplerBindingSnapshot::sequence))
                            .orElseThrow(() -> new IllegalStateException(
                                    "No live copy-source 16x16 lightmap sampler is available"));
            SamplerInspectionService.ReadbackResult result =
                    SamplerInspectionService.readRegion(
                            lightmap.id(), 0, 0, 16, 16);
            if (!result.success()) {
                throw new IllegalStateException(result.message());
            }
            future.complete(result);
        } catch (Exception exception) {
            LOGGER.error("Failed to capture lightmap readback", exception);
            future.completeExceptionally(exception);
        }
    }
}