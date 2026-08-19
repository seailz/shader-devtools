package com.seailz.csdt.client.mixins;

import com.mojang.renderpearl.api.pipeline.BindGroupLayout;
import com.mojang.renderpearl.backend.api.BackendRenderPipeline;
import com.mojang.renderpearl.backend.vulkan.VulkanDevice;
import com.mojang.renderpearl.backend.vulkan.VulkanRenderPipeline;
import com.seailz.csdt.client.service.ShaderDebugSourceService;
import org.lwjgl.vulkan.VK12;
import org.lwjgl.vulkan.VkDescriptorSetLayoutBinding;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(VulkanRenderPipeline.class)
public abstract class VulkanRenderPipelineMixin {

    @Unique
    private static final ThreadLocal<Boolean> CSDT_DEBUG_UNIFORM = new ThreadLocal<>();

    @Redirect(
            method = "compile",
            at = @At(value = "INVOKE", target = "Ljava/util/List;get(I)Ljava/lang/Object;")
    )
    private static Object csdt$trackDebugUniform(List<?> uniforms, int index) {
        Object uniform = uniforms.get(index);
        CSDT_DEBUG_UNIFORM.set(uniform instanceof BindGroupLayout.UniformDescription description
                && ShaderDebugSourceService.DEBUG_BUFFER_NAME.equals(description.name()));
        return uniform;
    }

    @Redirect(
            method = "compile",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/vulkan/VkDescriptorSetLayoutBinding;descriptorType(I)Lorg/lwjgl/vulkan/VkDescriptorSetLayoutBinding;"
            )
    )
    private static VkDescriptorSetLayoutBinding csdt$useStorageDescriptorType(
            VkDescriptorSetLayoutBinding binding,
            int descriptorType
    ) {
        return binding.descriptorType(Boolean.TRUE.equals(CSDT_DEBUG_UNIFORM.get())
                ? VK12.VK_DESCRIPTOR_TYPE_STORAGE_BUFFER
                : descriptorType);
    }

    @Redirect(
            method = "compile",
            at = @At(
                    value = "INVOKE",
                    target = "Lorg/lwjgl/vulkan/VkDescriptorSetLayoutBinding;binding(I)Lorg/lwjgl/vulkan/VkDescriptorSetLayoutBinding;"
            )
    )
    private static VkDescriptorSetLayoutBinding csdt$useStorageBinding(
            VkDescriptorSetLayoutBinding binding,
            int descriptorBinding
    ) {
        return binding.binding(Boolean.TRUE.equals(CSDT_DEBUG_UNIFORM.get())
                ? ShaderDebugSourceService.STORAGE_BINDING
                : descriptorBinding);
    }

    @Inject(method = "compile", at = @At("RETURN"))
    private static void csdt$clearDebugUniform(
            VulkanDevice device,
            BackendRenderPipeline.CreateInfo createInfo,
            CallbackInfoReturnable<VulkanRenderPipeline> cir
    ) {
        CSDT_DEBUG_UNIFORM.remove();
    }
}
