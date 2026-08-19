package com.seailz.csdt.client.mixins;

import com.mojang.renderpearl.api.buffers.GpuBufferSlice;
import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.backend.api.BackendRenderPipeline;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPass;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPipeline;
import com.seailz.csdt.client.service.ShaderDebugRuntimeService;
import com.seailz.csdt.client.service.ShaderDebugSourceService;
import it.unimi.dsi.fastutil.objects.ReferenceList;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkWriteDescriptorSet;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Objects;

@Mixin(VulkanRenderPass.class)
public abstract class VulkanRenderPassMixin {

    @Shadow
    private boolean anyDescriptorDirty;

    @Shadow
    protected VulkanRenderPipeline pipeline;

    @Shadow
    @Final
    protected ReferenceList<Object> uniforms;

    @Unique
    private static final ThreadLocal<Boolean> CSDT_DEBUG_UNIFORM = new ThreadLocal<>();

    @Inject(method = "setPipeline", at = @At("TAIL"))
    private void csdt$bindDebugStorage(BackendRenderPipeline pipeline, CallbackInfo ci) {
        this.csdt$refreshDebugStorage();
    }

    @Inject(method = "pushDescriptors", at = @At("HEAD"))
    private void csdt$prepareDebugStorage(CallbackInfo ci) {
        CSDT_DEBUG_UNIFORM.remove();
        this.csdt$refreshDebugStorage();
    }

    @Redirect(
            method = "pushDescriptors",
            at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;")
    )
    private Object csdt$trackDebugUniform(List<?> uniforms, int index) {
        Object uniform = uniforms.get(index);
        CSDT_DEBUG_UNIFORM.set(uniform instanceof BindGroupLayout.UniformDescription description
                && ShaderDebugSourceService.DEBUG_BUFFER_NAME.equals(description.name()));
        return uniform;
    }

    @Redirect(
            method = "pushDescriptors",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/vulkan/VkWriteDescriptorSet;dstBinding(I)Lorg/lwjgl/vulkan/VkWriteDescriptorSet;"
            )
    )
    private VkWriteDescriptorSet csdt$useStorageBinding(VkWriteDescriptorSet descriptorSet, int binding) {
        return descriptorSet.dstBinding(Boolean.TRUE.equals(CSDT_DEBUG_UNIFORM.get())
                ? ShaderDebugSourceService.STORAGE_BINDING
                : binding);
    }

    @Redirect(
            method = "pushDescriptors",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/vulkan/VkWriteDescriptorSet;descriptorType(I)Lorg/lwjgl/vulkan/VkWriteDescriptorSet;"
            )
    )
    private VkWriteDescriptorSet csdt$useStorageDescriptorType(VkWriteDescriptorSet descriptorSet, int descriptorType) {
        return descriptorSet.descriptorType(Boolean.TRUE.equals(CSDT_DEBUG_UNIFORM.get())
                ? VK12.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER
                : descriptorType);
    }

    @Inject(method = "pushDescriptors", at = @At("RETURN"))
    private void csdt$clearDebugUniform(CallbackInfo ci) {
        CSDT_DEBUG_UNIFORM.remove();
    }

    @Unique
    private void csdt$refreshDebugStorage() {
        if (this.pipeline == null) {
            return;
        }

        List<BindGroupLayout.UniformDescription> pipelineUniforms = this.pipeline.uniforms();
        for (int index = 0; index < pipelineUniforms.size(); index++) {
            if (!ShaderDebugSourceService.DEBUG_BUFFER_NAME.equals(pipelineUniforms.get(index).name())) {
                continue;
            }

            GpuBufferSlice storageSlice = ShaderDebugRuntimeService.storageSlice();
            if (storageSlice != null && !Objects.equals(this.uniforms.get(index), storageSlice)) {
                this.uniforms.set(index, storageSlice);
                this.anyDescriptorDirty = true;
            }
            return;
        }
    }
}
