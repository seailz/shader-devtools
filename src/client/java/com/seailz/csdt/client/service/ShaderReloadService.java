package com.seailz.csdt.client.service;

import com.mojang.logging.LogUtils;
import com.seailz.csdt.client.mixins.ShaderManagerMixin;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.Profiler;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.EnumMap;
import java.util.Map;

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
            ResourceManager resourceManager = minecraft.getResourceManager();
            ProfilerFiller profiler = Profiler.get();
            ShaderManager.Configs prepared = ((ShaderManagerMixin) shaderManager).csdt$prepare(resourceManager, profiler);
            prepared = ShaderResourceOverrideService.applyOverrides(prepared, resourceManager);
            ShaderManager.Configs current = currentConfigs(shaderManager);
            ShaderManager.Configs merged = switch (scope) {
                case CORE_ONLY -> new ShaderManager.Configs(prepared.shaderSources(), current.postChains());
                case POST_ONLY -> new ShaderManager.Configs(current.shaderSources(), prepared.postChains());
                case ALL -> prepared;
            };

            if (scope == ReloadScope.POST_ONLY) {
                replaceCompilationCache(shaderManager, merged);
            } else {
                ((ShaderManagerMixin) shaderManager).csdt$apply(merged, resourceManager, profiler);
            }

            ReloadStat stat = ReloadStat.success(System.currentTimeMillis(), nanosToMillis(startedAt, System.nanoTime()));
            STATS.put(scope, stat);
            ClientToastService.showReloadResult(scope, stat);
        } catch (Exception exception) {
            ReloadStat stat = ReloadStat.failure(System.currentTimeMillis(), nanosToMillis(startedAt, System.nanoTime()), exception.getClass().getSimpleName() + ": " + exception.getMessage());
            STATS.put(scope, stat);
            ClientToastService.showReloadResult(scope, stat);
            LOGGER.error("Failed to reload {} shaders", scope.logName, exception);
        }
    }

    private static long nanosToMillis(long startedAt, long endedAt) {
        return Duration.ofNanos(endedAt - startedAt).toMillis();
    }

    private static ShaderManager.Configs currentConfigs(ShaderManager shaderManager) throws ReflectiveOperationException {
        Object compilationCache = compilationCacheField().get(shaderManager);
        return (ShaderManager.Configs) compilationConfigsField().get(compilationCache);
    }

    private static void replaceCompilationCache(ShaderManager shaderManager, ShaderManager.Configs configs) throws ReflectiveOperationException {
        Field compilationCacheField = compilationCacheField();
        Object oldCache = compilationCacheField.get(shaderManager);
        Method closeMethod = oldCache.getClass().getDeclaredMethod("close");
        closeMethod.setAccessible(true);
        closeMethod.invoke(oldCache);
        compilationCacheField.set(shaderManager, newCompilationCache(shaderManager, configs));
    }

    private static Object newCompilationCache(ShaderManager shaderManager, ShaderManager.Configs configs) throws ReflectiveOperationException {
        Constructor<?> constructor = Class.forName("net.minecraft.client.renderer.ShaderManager$CompilationCache").getDeclaredConstructors()[0];
        constructor.setAccessible(true);
        Class<?>[] parameterTypes = constructor.getParameterTypes();
        Object[] arguments = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            Class<?> parameterType = parameterTypes[i];
            if (ShaderManager.class.isAssignableFrom(parameterType)) {
                arguments[i] = shaderManager;
            } else if (ShaderManager.Configs.class.isAssignableFrom(parameterType)) {
                arguments[i] = configs;
            } else {
                throw new IllegalStateException("Unsupported ShaderManager$CompilationCache constructor parameter: " + parameterType.getName());
            }
        }
        return constructor.newInstance(arguments);
    }

    private static Field compilationCacheField() throws NoSuchFieldException {
        Field field = ShaderManager.class.getDeclaredField("compilationCache");
        field.setAccessible(true);
        return field;
    }

    private static Field compilationConfigsField() throws ClassNotFoundException, NoSuchFieldException {
        Field field = Class.forName("net.minecraft.client.renderer.ShaderManager$CompilationCache").getDeclaredField("configs");
        field.setAccessible(true);
        return field;
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
