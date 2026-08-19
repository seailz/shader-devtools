package com.seailz.csdt.client.mixins;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.textures.GpuSampler;
import com.mojang.renderpearl.api.textures.GpuTextureView;
import com.mojang.renderpearl.backend.api.GpuDeviceBackend;
import com.mojang.renderpearl.frontend.FrontendRenderPass;
import com.mojang.renderpearl.frontend.FrontendRenderPipeline;
import com.seailz.csdt.client.service.CompiledPipelineRegistry;
import com.seailz.csdt.client.service.SamplerInspectionService;
import com.seailz.csdt.client.service.ShaderDebugRuntimeService;
import com.seailz.csdt.client.service.ShaderDebugSourceService;
import com.seailz.csdt.client.service.UniformInspectorService;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.HashMap;

@Mixin(FrontendRenderPass.class)
public abstract class RenderPassMixin {

    @Shadow
    @Final
    private GpuDeviceBackend device;

    @Shadow
    private FrontendRenderPipeline boundPipeline;

    @Shadow
    @Final
    protected HashMap<String, Object> uniforms;

    @Inject(method = "setPipeline", at = @At("HEAD"))
    private void csdt$rememberSamplerPipeline(CompiledRenderPipeline pipeline, CallbackInfo ci) {
        SamplerInspectionService.forgetRenderPass(this);
        RenderPipeline sourcePipeline = CompiledPipelineRegistry.sourceFor(pipeline);
        if (sourcePipeline != null) {
            SamplerInspectionService.rememberPipeline(this, sourcePipeline);
        }

        if (pipeline instanceof FrontendRenderPipeline frontendPipeline
                && frontendPipeline.uniforms().stream().anyMatch(this::csdt$isDebugUniform)) {
            GpuBufferSlice storageSlice = ShaderDebugRuntimeService.storageSlice();
            if (storageSlice != null) {
                this.uniforms.put(ShaderDebugSourceService.DEBUG_BUFFER_NAME, storageSlice);
            }
        } else {
            this.uniforms.remove(ShaderDebugSourceService.DEBUG_BUFFER_NAME);
        }
    }

    @Inject(
            method = "setUniform(Ljava/lang/String;Lcom/mojang/renderpearl/api/textures/GpuTextureView;Lcom/mojang/renderpearl/api/textures/GpuSampler;)V",
            at = @At("TAIL")
    )
    private void csdt$captureSamplerBinding(String name, GpuTextureView view, GpuSampler sampler, CallbackInfo ci) {
        SamplerInspectionService.captureBinding(this, name, view, sampler);
    }

    @Inject(
            method = "setUniform(Ljava/lang/String;Lcom/mojang/renderpearl/api/buffers/GpuBufferSlice;)V",
            at = @At("TAIL")
    )
    private void csdt$recordUniformInspectorSlice(String name, GpuBufferSlice slice, CallbackInfo ci) {
        UniformInspectorService.recordUniformBinding(
                this.device.getDeviceInfo().backendName(),
                this.csdt$sourcePipeline(),
                name,
                slice
        );
    }

    @Inject(method = "close", at = @At("TAIL"))
    private void csdt$forgetSamplerPipeline(CallbackInfo ci) {
        SamplerInspectionService.forgetRenderPass(this);
    }

    private RenderPipeline csdt$sourcePipeline() {
        return this.boundPipeline == null ? null : CompiledPipelineRegistry.sourceFor(this.boundPipeline);
    }

    private boolean csdt$isDebugUniform(com.mojang.renderpearl.api.pipeline.BindGroupLayout.UniformDescription uniform) {
        return ShaderDebugSourceService.DEBUG_BUFFER_NAME.equals(uniform.name());
    }
}
