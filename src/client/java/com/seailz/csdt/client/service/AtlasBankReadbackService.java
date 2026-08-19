package com.seailz.csdt.client.service;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import org.slf4j.Logger;

import java.util.Comparator;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

/** Debug-only GPU readback for the custom block-atlas state-bank region. */
public final class AtlasBankReadbackService {

    private static final Logger LOGGER = LogUtils.getLogger();

    private AtlasBankReadbackService() {
    }

    public static CompletableFuture<SamplerInspectionService.ReadbackResult> capture() {
        CompletableFuture<SamplerInspectionService.ReadbackResult> future = new CompletableFuture<>();
        Minecraft.getInstance().execute(() -> captureOnRenderThread(future));
        return future;
    }

    private static void captureOnRenderThread(
            CompletableFuture<SamplerInspectionService.ReadbackResult> future) {
        try {
            SamplerInspectionService.SamplerBindingSnapshot blockAtlas =
                    SamplerInspectionService.snapshotBindings().stream()
                            .filter(snapshot -> snapshot.copySrc()
                                    && !snapshot.textureClosed()
                                    && snapshot.baseMipLevel() == 0
                                    && snapshot.width() >= 1024
                                    && snapshot.height() >= 1024
                                    && snapshot.textureLabel().toLowerCase(Locale.ROOT)
                                    .contains("blocks"))
                            .max(Comparator.comparingLong(
                                    SamplerInspectionService.SamplerBindingSnapshot::sequence))
                            .orElseThrow(() -> new IllegalStateException(
                                    "No live copy-source block atlas sampler is available"));
            SamplerInspectionService.ReadbackResult result =
                    SamplerInspectionService.readRegion(
                            blockAtlas.id(), 0, 0, 144, 144);
            if (!result.success()) {
                throw new IllegalStateException(result.message());
            }
            future.complete(result);
        } catch (Exception exception) {
            LOGGER.error("Failed to capture atlas bank readback", exception);
            future.completeExceptionally(exception);
        }
    }
}
