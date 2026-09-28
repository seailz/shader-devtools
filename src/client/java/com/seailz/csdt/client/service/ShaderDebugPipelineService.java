package com.seailz.csdt.client.service;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.ShaderSource;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.SpvModule;
import com.mojang.renderpearl.api.pipeline.UniformType;
import org.lwjgl.util.spvc.Spvc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class ShaderDebugPipelineService {

    private static final ThreadLocal<CompileState> COMPILE_STATE = new ThreadLocal<>();

    private ShaderDebugPipelineService() {
    }

    public static void beginCompile(GpuDevice device, RenderPipeline pipeline, ShaderSource shaderSource) {
        COMPILE_STATE.set(new CompileState(backendFor(device), hasDebugBuffer(pipeline, shaderSource)));
    }

    public static void finishCompile() {
        COMPILE_STATE.remove();
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

    public static int resourceTypeFor(SpvModule.Reflection.Descriptor descriptor) {
        return isVulkanDebugPipeline()
                && ShaderDebugSourceService.DEBUG_BUFFER_NAME.equals(descriptor.name())
                ? Spvc.SPVC_RESOURCE_TYPE_STORAGE_BUFFER
                : descriptor.resourceType();
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
            String source = shaderSource.getShader(entry.getValue(), entry.getKey());
            if (source != null && source.contains(ShaderDebugSourceService.DEBUG_BUFFER_NAME)) {
                return true;
            }
        }
        return false;
    }

    private static Backend backendFor(GpuDevice device) {
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
