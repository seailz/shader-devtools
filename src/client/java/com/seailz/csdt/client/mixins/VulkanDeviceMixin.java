package com.seailz.csdt.client.mixins;

import com.mojang.blaze3d.vulkan.VulkanDevice;
import com.mojang.blaze3d.vulkan.glsl.IntermediaryShaderModule;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Map;

/**
 * Fixes a bug in 26.2-snapshot-1 where if Vulkan is enabled, core shaders will only reload when the entire game is restarted
 */
@Mixin(VulkanDevice.class)
public abstract class VulkanDeviceMixin {
    @Shadow
    @Final
    private Map<?, IntermediaryShaderModule> shaderCache;

    @Inject(method = "clearPipelineCache", at = @At("TAIL"))
    private void csdt$clearShaderCache(CallbackInfo ci) {
        this.shaderCache.clear();
    }
}
