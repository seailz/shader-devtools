package com.seailz.csdt.client.service;

import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.backend.api.GpuDeviceBackend;
import com.mojang.renderpearl.backend.api.SpvModule;
import com.mojang.renderpearl.frontend.shaders.SpvUtil;
import org.lwjgl.util.spvc.Spvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ShaderDebugPipelineService {

    private static final ThreadLocal<CompileState> COMPILE_STATE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> VALIDATING_DEBUG_UNIFORM = new ThreadLocal<>();

    private ShaderDebugPipelineService() {
    }

    public static void beginCompile(GpuDeviceBackend device, RenderPipeline pipeline, ShaderSource shaderSource) {
        COMPILE_STATE.set(new CompileState(backendFor(device), hasDebugBuffer(pipeline, shaderSource)));
        VALIDATING_DEBUG_UNIFORM.remove();
    }

    public static void finishCompile() {
        COMPILE_STATE.remove();
        VALIDATING_DEBUG_UNIFORM.remove();
    }

    public static List<BindGroupLayout> withDebugLayout(List<BindGroupLayout> layouts) {
        if (!isVulkanDebugPipeline() || containsDebugUniform(layouts)) {
            return layouts;
        }

        List<BindGroupLayout> augmentedLayouts = new ArrayList<>(layouts);
        augmentedLayouts.add(new BindGroupLayout(List.of(new BindGroupLayout.UniformDescription(
                ShaderDebugSourceService.DEBUG_BUFFER_NAME,
                UniformType.UNIFORM_BUFFER
        ))));
        return List.copyOf(augmentedLayouts);
    }

    public static List<SpvModule.Reflection.Descriptor> filterDescriptors(List<SpvModule.Reflection.Descriptor> descriptors) {
        if (!isOpenGlDebugPipeline()) {
            return descriptors;
        }
        return descriptors.stream()
                .filter(descriptor -> !ShaderDebugSourceService.DEBUG_BUFFER_NAME.equals(descriptor.name()))
                .toList();
    }

    public static UniformType markUniformForValidation(BindGroupLayout.UniformDescription uniform) {
        VALIDATING_DEBUG_UNIFORM.set(isVulkanDebugPipeline()
                && ShaderDebugSourceService.DEBUG_BUFFER_NAME.equals(uniform.name()));
        return uniform.type();
    }

    public static int resourceTypeForCurrentUniform(UniformType uniformType) {
        try {
            return Boolean.TRUE.equals(VALIDATING_DEBUG_UNIFORM.get())
                    ? Spvc.SPVC_RESOURCE_TYPE_STORAGE_BUFFER
                    : SpvUtil.resourceType(uniformType);
        } finally {
            VALIDATING_DEBUG_UNIFORM.remove();
        }
    }

    public static int bindingFor(SpvModule.Reflection.Descriptor descriptor, int binding) {
        return isVulkanDebugPipeline() && ShaderDebugSourceService.DEBUG_BUFFER_NAME.equals(descriptor.name())
                ? ShaderDebugSourceService.STORAGE_BINDING
                : binding;
    }

    private static boolean containsDebugUniform(List<BindGroupLayout> layouts) {
        return BindGroupLayout.flattenUniforms(layouts).stream()
                .anyMatch(uniform -> ShaderDebugSourceService.DEBUG_BUFFER_NAME.equals(uniform.name()));
    }

    private static boolean hasDebugBuffer(RenderPipeline pipeline, ShaderSource shaderSource) {
        for (Map.Entry<com.mojang.renderpearl.api.pipeline.ShaderType, net.minecraft.resources.Identifier> entry : pipeline.getShaders().entrySet()) {
            String source = shaderSource.get(entry.getValue(), entry.getKey());
            if (source != null && source.contains(ShaderDebugSourceService.DEBUG_BUFFER_NAME)) {
                return true;
            }
        }
        return false;
    }

    private static Backend backendFor(GpuDeviceBackend device) {
        String backendName = device.getDeviceInfo().backendName().toLowerCase(Locale.ROOT);
        if (backendName.contains("vulkan")) {
            return Backend.VULKAN;
        }
        if (backendName.contains("opengl")) {
            return Backend.OPENGL;
        }
        return Backend.OTHER;
    }

    private static boolean isVulkanDebugPipeline() {
        CompileState state = COMPILE_STATE.get();
        return state != null && state.backend() == Backend.VULKAN && state.hasDebugBuffer();
    }

    private static boolean isOpenGlDebugPipeline() {
        CompileState state = COMPILE_STATE.get();
        return state != null && state.backend() == Backend.OPENGL && state.hasDebugBuffer();
    }

    private enum Backend {
        OPENGL,
        VULKAN,
        OTHER
    }

    private record CompileState(Backend backend, boolean hasDebugBuffer) {
    }
}
