package com.seailz.csdt.client.service;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.Util;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

/**
 * Util to help with reloading shaders without triggering a full resource pack reload.
 */
public final class ShaderReloadService {

    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Map<ReloadScope, ReloadStat> STATS = new EnumMap<>(ReloadScope.class);

    static {
        for (ReloadScope scope : ReloadScope.values()) {
            STATS.put(scope, ReloadStat.empty());
        }
    }

    private ShaderReloadService() {
    }

    public static void reloadCoreShadersOnly() {
        enqueueReload(ReloadScope.CORE_ONLY);
    }

    public static void reloadPostShadersOnly() {
        enqueueReload(ReloadScope.POST_ONLY);
    }

    public static void reloadAllShaders() {
        enqueueReload(ReloadScope.ALL);
    }

    public static void reloadAllShadersFromHub() {
        ShaderResourceOverrideService.clearVisualizations();
        ForcedPostEffectService.clearForcedPostEffect();
        enqueueReload(ReloadScope.ALL);
    }

    public static ReloadStat getStat(ReloadScope scope) {
        return STATS.get(scope);
    }

    private static void enqueueReload(ReloadScope scope) {
        Minecraft minecraft = Minecraft.getInstance();
        minecraft.execute(() -> reloadNow(minecraft, scope));
    }

    private static void reloadNow(Minecraft minecraft, ReloadScope scope) {
        long startedAt = System.nanoTime();
        try {
            ShaderManager shaderManager = minecraft.getShaderManager();
            PreparableReloadListener.SharedState reloadState = new PreparableReloadListener.SharedState(minecraft.getResourceManager());
            PreparableReloadListener.PreparationBarrier barrier = CompletableFuture::completedFuture;
            shaderManager.reload(reloadState, Util.backgroundExecutor(), barrier, minecraft::execute)
                    .whenCompleteAsync((unused, throwable) -> completeReload(scope, startedAt, throwable), minecraft::execute);
        } catch (Exception exception) {
            completeReload(scope, startedAt, exception);
        }
    }

    private static long nanosToMillis(long startedAt, long endedAt) {
        return Duration.ofNanos(endedAt - startedAt).toMillis();
    }

    private static void completeReload(ReloadScope scope, long startedAt, Throwable throwable) {
        if (throwable == null) {
            ReloadStat stat = ReloadStat.success(System.currentTimeMillis(), nanosToMillis(startedAt, System.nanoTime()));
            STATS.put(scope, stat);
            ClientToastService.showReloadResult(scope, stat);
            return;
        }

        Throwable cause = throwable instanceof CompletionException && throwable.getCause() != null
                ? throwable.getCause()
                : throwable;
        ReloadStat stat = ReloadStat.failure(
                System.currentTimeMillis(),
                nanosToMillis(startedAt, System.nanoTime()),
                cause.getClass().getSimpleName() + ": " + cause.getMessage()
        );
        STATS.put(scope, stat);
        ClientToastService.showReloadResult(scope, stat);
        LOGGER.error("Failed to reload {} shaders", scope.logName, cause);
    }

    public enum ReloadScope {
        CORE_ONLY("core", "Core"),
        POST_ONLY("post", "Post"),
        ALL("all", "All");

        private final String logName;
        private final String label;

        ReloadScope(String logName, String label) {
            this.logName = logName;
            this.label = label;
        }

        public String label() {
            return this.label;
        }
    }

    public record ReloadStat(boolean success, long finishedAtMillis, long durationMillis, String message) {

        private static ReloadStat empty() {
            return new ReloadStat(true, 0L, -1L, "No reload yet");
        }

        private static ReloadStat success(long finishedAtMillis, long durationMillis) {
            return new ReloadStat(true, finishedAtMillis, durationMillis, "OK");
        }

        private static ReloadStat failure(long finishedAtMillis, long durationMillis, String message) {
            return new ReloadStat(false, finishedAtMillis, durationMillis, message == null ? "Unknown error" : message);
        }
    }
}
