package com.seailz.csdt.client.mixins;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.renderer.pipeline.RenderPipelines;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import java.util.Map;

@Mixin(RenderPipelines.class)
public interface RenderPipelinesAccessor {

    @Accessor("DYNAMIC_PIPELINES_CACHE")
    static Map<?, RenderPipeline> csdt$getDynamicPipelines() {
        throw new AssertionError("RenderPipelines accessor was not transformed");
    }
}
