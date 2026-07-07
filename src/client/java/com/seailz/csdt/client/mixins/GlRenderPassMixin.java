package com.seailz.csdt.client.mixins;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.backend.opengl.GlRenderPipeline;
import com.seailz.csdt.client.service.UniformInspectorService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.mojang.renderpearl.backend.opengl.GlRenderPass")
public abstract class GlRenderPassMixin {

    @Shadow
    protected GlRenderPipeline pipeline;

    @Inject(method = "setUniform(Ljava/lang/String;Lcom/mojang/renderpearl/api/buffers/GpuBuffer;)V", at = @At("TAIL"))
    private void csdt$recordUniformInspectorBuffer(String name, GpuBuffer buffer, CallbackInfo ci) {
        UniformInspectorService.recordUniformBinding("OpenGL", this.pipeline == null ? null : this.pipeline.info(), name, buffer);
    }

    @Inject(method = "setUniform(Ljava/lang/String;Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;)V", at = @At("TAIL"))
    private void csdt$recordUniformInspectorSlice(String name, GpuBufferSlice slice, CallbackInfo ci) {
        UniformInspectorService.recordUniformBinding("OpenGL", this.pipeline == null ? null : this.pipeline.info(), name, slice);
    }
}
