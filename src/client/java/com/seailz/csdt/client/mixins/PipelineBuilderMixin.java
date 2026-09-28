package com.seailz.csdt.client.mixins;

import com.mojang.blaze3d.pipeline.PipelineBuilder;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.pipeline.ShaderSource;
import com.mojang.renderpearl.api.device.GpuDevice;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.SpvModule;
import com.seailz.csdt.client.service.ShaderDebugPipelineService;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(PipelineBuilder.class)
public abstract class PipelineBuilderMixin {

    @Shadow
    @Final
    private GpuDevice device;

    @Inject(method = "generateBackendCreateInfo", at = @At("HEAD"))
    private void csdt$beginShaderDebugPipeline(
            RenderPipeline sourcePipeline,
            ShaderSource shaderSource,
            CallbackInfoReturnable<CompiledRenderPipeline.CreateInfo> cir
    ) {
        ShaderDebugPipelineService.beginCompile(this.device, sourcePipeline, shaderSource);
    }

    @Redirect(
            method = "generateBackendCreateInfo",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/blaze3d/pipeline/RenderPipeline;getBindGroupLayouts()Ljava/util/List;"
            )
    )
    private List<BindGroupLayout> csdt$addVulkanDebugLayout(RenderPipeline pipeline) {
        return ShaderDebugPipelineService.withDebugLayout(pipeline.getBindGroupLayouts());
    }

    @Redirect(
            method = "generateBackendCreateInfo",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/renderpearl/api/pipeline/SpvModule$Reflection;descriptors()Ljava/util/List;"
            )
    )
    private List<SpvModule.Reflection.Descriptor> csdt$filterOpenGlDebugDescriptor(SpvModule.Reflection reflection) {
        return ShaderDebugPipelineService.filterDescriptors(reflection.descriptors());
    }

    @Redirect(
            method = "generateBackendCreateInfo",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/renderpearl/api/pipeline/SpvModule$Reflection$Descriptor;resourceType()I"
            )
    )
    private int csdt$allowStorageBufferReflection(SpvModule.Reflection.Descriptor descriptor) {
        return ShaderDebugPipelineService.resourceTypeFor(descriptor);
    }

    @Redirect(
            method = "generateBackendCreateInfo",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/renderpearl/api/pipeline/SpvModule$Reflection$Descriptor;binding(I)V"
            )
    )
    private void csdt$preserveDebugStorageBinding(SpvModule.Reflection.Descriptor descriptor, int binding) {
        descriptor.binding(ShaderDebugPipelineService.bindingFor(descriptor, binding));
    }

    @Inject(method = "generateBackendCreateInfo", at = @At("RETURN"))
    private void csdt$finishShaderDebugPipeline(
            RenderPipeline sourcePipeline,
            ShaderSource shaderSource,
            CallbackInfoReturnable<CompiledRenderPipeline.CreateInfo> cir
    ) {
        ShaderDebugPipelineService.finishCompile();
    }
}
