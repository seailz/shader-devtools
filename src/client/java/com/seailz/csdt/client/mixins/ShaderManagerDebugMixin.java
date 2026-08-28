package com.seailz.csdt.client.mixins;

import com.google.common.collect.ImmutableMap;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.logging.LogUtils;
import com.seailz.csdt.client.service.ShaderDebugSourceService;
import com.seailz.csdt.client.service.ShaderResourceOverrideService;
import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.BufferedReader;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;

@Mixin(ShaderManager.class)
public abstract class ShaderManagerDebugMixin {

    private static final Logger CSDT_LOGGER = LogUtils.getLogger();

    @Inject(method = "loadConfigs", at = @At("HEAD"))
    private void csdt$beginShaderDebugReload(ResourceManager manager, CallbackInfoReturnable<ShaderManager.Configs> cir) {
        ShaderDebugSourceService.beginReload();
    }

    @Inject(method = "loadConfigs", at = @At("RETURN"), cancellable = true)
    private void csdt$applyShaderOverrides(ResourceManager manager, CallbackInfoReturnable<ShaderManager.Configs> cir) {
        cir.setReturnValue(ShaderResourceOverrideService.applyOverrides(cir.getReturnValue(), manager));
    }

    @Inject(
            method = "loadShader",
            at = @At("HEAD"),
            cancellable = true
    )
    private static void csdt$transformShaderSource(
            Identifier location,
            Resource resource,
            ShaderType type,
            Map<Identifier, Resource> files,
            ImmutableMap.Builder<?, ?> output,
            CallbackInfo ci
    ) {
        Identifier id = type == null ? location : type.idConverter().fileToId(location);
        try (BufferedReader reader = resource.openAsReader()) {
            String source = IOUtils.toString(reader);
            ((ImmutableMap.Builder) output).put(
                    createShaderSourceKey(id, type),
                    type == null ? source : ShaderDebugSourceService.transformShaderSource(location, type, source)
            );
        } catch (IOException exception) {
            CSDT_LOGGER.error("Failed to load shader source at {}", location, exception);
        }
        ci.cancel();
    }

    private static Object createShaderSourceKey(Identifier id, ShaderType type) {
        try {
            Class<?> keyClass = Class.forName("net.minecraft.client.renderer.ShaderManager$ShaderSourceKey");
            Constructor<?> constructor = keyClass.getDeclaredConstructor(Identifier.class, ShaderType.class);
            constructor.setAccessible(true);
            return constructor.newInstance(id, type);
        } catch (ClassNotFoundException | NoSuchMethodException | InstantiationException | IllegalAccessException |
                 InvocationTargetException exception) {
            throw new IllegalStateException("Failed to create ShaderSourceKey", exception);
        }
    }
}
