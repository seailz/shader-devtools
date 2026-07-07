package com.seailz.csdt.client.mixins;

import com.mojang.renderpearl.backend.vulkan.VulkanGpuSampler;
import com.mojang.renderpearl.backend.vulkan.VulkanGpuTextureView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "com.mojang.renderpearl.backend.vulkan.VulkanRenderPass$TextureViewAndSampler")
public interface VulkanRenderPassTextureBindingAccessor {

    @Accessor("view")
    VulkanGpuTextureView csdt$getView();

    @Accessor("sampler")
    VulkanGpuSampler csdt$getSampler();
}
