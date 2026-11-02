package com.seailz.csdt.client.mixins;

import com.mojang.blaze3d.pipeline.PipelineCache;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.seailz.csdt.client.service.CompiledPipelineRegistry;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PipelineCache.class)
public abstract class PipelineCacheMixin {

    @Inject(method = "get(Lcom/mojang/blaze3d/pipeline/PipelineCache$Key;)Lcom/mojang/renderpearl/api/pipeline/CompiledRenderPipeline;", at = @At("RETURN"))
    private void csdt$rememberSourcePipeline(
            PipelineCache.Key key,
            CallbackInfoReturnable<CompiledRenderPipeline> cir
    ) {
        CompiledPipelineRegistry.remember(cir.getReturnValue(), key.pipeline());
    }
}
