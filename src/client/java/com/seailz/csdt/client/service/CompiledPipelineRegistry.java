package com.seailz.csdt.client.service;

import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.WeakHashMap;

public final class CompiledPipelineRegistry {

    private static final Map<CompiledRenderPipeline, RenderPipeline> SOURCE_PIPELINES = new WeakHashMap<>();

    private CompiledPipelineRegistry() {
    }

    public static synchronized void remember(CompiledRenderPipeline compiledPipeline, RenderPipeline sourcePipeline) {
        if (compiledPipeline != null && sourcePipeline != null) {
            SOURCE_PIPELINES.put(compiledPipeline, sourcePipeline);
        }
    }

    @Nullable
    public static synchronized RenderPipeline sourceFor(CompiledRenderPipeline compiledPipeline) {
        return compiledPipeline == null ? null : SOURCE_PIPELINES.get(compiledPipeline);
    }
}
