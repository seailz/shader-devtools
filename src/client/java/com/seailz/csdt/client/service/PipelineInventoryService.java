package com.seailz.csdt.client.service;

import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderType;
import com.mojang.renderpearl.api.pipeline.UniformType;
import net.minecraft.client.renderer.RenderPipelines;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

public final class PipelineInventoryService {

    private PipelineInventoryService() {
    }

    public static List<PipelineEntry> listPipelines() {
        return Stream.concat(RenderPipelines.requiredPipelines().stream(), RenderPipelines.optionalPipelines().stream())
                .sorted(Comparator.comparing(pipeline -> pipeline.getLocation().toString()))
                .map(PipelineEntry::new)
                .toList();
    }

    public static String describe(PipelineEntry entry) {
        RenderPipeline pipeline = entry.pipeline();
        return """
                [Shaders]
                Vertex: %s
                Fragment: %s

                [Geometry]
                Vertex Formats: %s
                Primitive Topology: %s
                Polygon Mode: %s

                [Resources]
                Samplers: %s
                Uniforms: %s
                Defines: %s

                [State]
                Cull: %s
                Wants Depth Texture: %s
                Color Targets: %s
                Depth/Stencil: %s
                """.formatted(
                shortId(pipeline.getShaders().get(ShaderType.VERTEX)),
                shortId(pipeline.getShaders().get(ShaderType.FRAGMENT)),
                pipeline.getVertexFormatBindings(),
                pipeline.getPrimitiveTopology(),
                pipeline.getPolygonMode(),
                formatList(samplers(pipeline)),
                formatList(uniforms(pipeline)),
                pipeline.getShaderDefines(),
                pipeline.isCull() ? "Enabled" : "Disabled",
                pipeline.wantsDepthTexture() ? "Yes" : "No",
                pipeline.getColorTargetStates(),
                pipeline.getDepthStencilState() == null ? "<none>" : pipeline.getDepthStencilState().toString()
        ).trim();
    }

    private static List<BindGroupLayout.UniformDescription> samplers(RenderPipeline pipeline) {
        return BindGroupLayout.flattenUniforms(pipeline.getBindGroupLayouts()).stream()
                .filter(uniform -> uniform.type() == UniformType.COMBINED_IMAGE_SAMPLER)
                .toList();
    }

    private static List<BindGroupLayout.UniformDescription> uniforms(RenderPipeline pipeline) {
        return BindGroupLayout.flattenUniforms(pipeline.getBindGroupLayouts()).stream()
                .filter(uniform -> uniform.type() != UniformType.COMBINED_IMAGE_SAMPLER)
                .toList();
    }

    private static String formatList(List<?> values) {
        return values.isEmpty() ? "<none>" : values.toString();
    }

    private static String shortId(Object value) {
        String text = String.valueOf(value);
        return text.startsWith("minecraft:") ? text.substring("minecraft:".length()) : text;
    }

    public record PipelineEntry(RenderPipeline pipeline) {
        public String location() {
            return this.pipeline.getLocation().toString();
        }
    }
}
