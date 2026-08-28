package com.seailz.csdt.client.mixins;

import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.api.pipeline.CompiledRenderPipeline;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.mojang.renderpearl.api.pipeline.ShaderSource;
import com.mojang.renderpearl.api.pipeline.UniformType;
import com.mojang.renderpearl.backend.api.BackendRenderPipeline;
import com.mojang.renderpearl.backend.api.GpuDeviceBackend;
import com.mojang.renderpearl.backend.api.SpvModule;
import com.mojang.renderpearl.frontend.shaders.PipelineBuilder;
import com.seailz.csdt.client.service.CompiledPipelineRegistry;
import com.seailz.csdt.client.service.ShaderDebugPipelineService;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ReferenceArrayList;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.concurrent.Executor;

@Mixin(PipelineBuilder.class)
public abstract class PipelineBuilderMixin {

    @Shadow
    @Final
    private GpuDeviceBackend backendDevice;

    @Inject(method = "generateBackendCreateInfo", at = @At("HEAD"))
    private void csdt$beginShaderDebugPipeline(
            RenderPipeline sourcePipeline,
            ShaderSource shaderSource,
            ReferenceArrayList<BackendRenderPipeline.CreateInfo.Shader> shaders,
            Object2IntOpenHashMap<String> uniformIndices,
            CallbackInfoReturnable<BackendRenderPipeline.CreateInfo> cir
    ) {
        ShaderDebugPipelineService.beginCompile(this.backendDevice, sourcePipeline, shaderSource);
    }

    @Redirect(
            method = "generateBackendCreateInfo",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/renderpearl/api/pipeline/RenderPipeline;getBindGroupLayouts()Ljava/util/List;"
            )
    )
    private List<BindGroupLayout> csdt$addVulkanDebugLayout(RenderPipeline pipeline) {
        return ShaderDebugPipelineService.withDebugLayout(pipeline.getBindGroupLayouts());
    }

    @Redirect(
            method = "generateBackendCreateInfo",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/renderpearl/backend/api/SpvModule$Reflection;descriptors()Ljava/util/List;"
            )
    )
    private List<SpvModule.Reflection.Descriptor> csdt$filterOpenGlDebugDescriptor(SpvModule.Reflection reflection) {
        return ShaderDebugPipelineService.filterDescriptors(reflection.descriptors());
    }

    @Redirect(
            method = "generateBackendCreateInfo",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/renderpearl/api/pipeline/BindGroupLayout$UniformDescription;type()Lcom/mojang/renderpearl/api/pipeline/UniformType;",
                    ordinal = 1
            )
    )
    private UniformType csdt$trackDebugUniformValidation(BindGroupLayout.UniformDescription uniform) {
        return ShaderDebugPipelineService.markUniformForValidation(uniform);
    }

    @Redirect(
            method = "generateBackendCreateInfo",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/renderpearl/frontend/shaders/SpvUtil;resourceType(Lcom/mojang/renderpearl/api/pipeline/UniformType;)I"
            )
    )
    private int csdt$allowStorageBufferReflection(UniformType uniformType) {
        return ShaderDebugPipelineService.resourceTypeForCurrentUniform(uniformType);
    }

    @Redirect(
            method = "generateBackendCreateInfo",
            at = @At(
                    value = "INVOKE",
                    target = "Lcom/mojang/renderpearl/backend/api/SpvModule$Reflection$Descriptor;binding(I)V"
            )
    )
    private void csdt$preserveDebugStorageBinding(SpvModule.Reflection.Descriptor descriptor, int binding) {
        descriptor.binding(ShaderDebugPipelineService.bindingFor(descriptor, binding));
    }

    @Inject(method = "generateBackendCreateInfo", at = @At("RETURN"))
    private void csdt$finishShaderDebugPipeline(
            RenderPipeline sourcePipeline,
            ShaderSource shaderSource,
            ReferenceArrayList<BackendRenderPipeline.CreateInfo.Shader> shaders,
            Object2IntOpenHashMap<String> uniformIndices,
            CallbackInfoReturnable<BackendRenderPipeline.CreateInfo> cir
    ) {
        ShaderDebugPipelineService.finishCompile();
    }

    @Inject(method = "lambda$compilePipeline$1", at = @At("RETURN"))
    private static void csdt$rememberSourcePipeline(
            RenderPipeline sourcePipeline,
            BackendRenderPipeline.Pending pendingPipeline,
            Executor executor,
            ReferenceArrayList<?> shaders,
            Object2IntOpenHashMap<String> uniformIndices,
            CallbackInfoReturnable<CompiledRenderPipeline> cir
    ) {
        CompiledPipelineRegistry.remember(cir.getReturnValue(), sourcePipeline);
    }
}
