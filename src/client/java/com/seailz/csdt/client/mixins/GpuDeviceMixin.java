package com.seailz.csdt.client.mixins;

import com.mojang.renderpearl.api.buffers.GpuBuffer;
import com.mojang.renderpearl.api.textures.GpuTexture;
import com.seailz.csdt.client.service.UniformInspectorService;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.nio.ByteBuffer;
import java.util.function.Supplier;

@Mixin(targets = {"com.mojang.renderpearl.backend.opengl.GlDevice", "com.mojang.renderpearl.backend.vulkan.VulkanDevice"})
public abstract class GpuDeviceMixin {

    @Inject(method = "createBuffer(Ljava/util/function/Supplier;ILjava/nio/ByteBuffer;)Lcom/mojang/renderpearl/api/buffers/GpuBuffer;", at = @At("RETURN"))
    private void csdt$recordUniformInspectorInitialBuffer(
            Supplier<String> label,
            int usage,
            ByteBuffer source,
            CallbackInfoReturnable<GpuBuffer> cir
    ) {
        UniformInspectorService.recordCreatedBuffer(cir.getReturnValue(), source);
    }

    // Sampler inspection needs transfer-source access for the lightmap. Applying this flag to every
    // texture causes Snapshot 10's backend to create invalid atlas resources.
    @ModifyVariable(
            method = "createTexture(Ljava/lang/String;ILcom/mojang/renderpearl/api/GpuFormat;IIII)Lcom/mojang/renderpearl/api/textures/GpuTexture;",
            at = @At("HEAD"),
            argsOnly = true,
            ordinal = 0
    )
    private int csdt$allowNamedTextureReadback(int usage, String label) {
        return "Lightmap".equals(label) ? usage | GpuTexture.USAGE_COPY_SRC : usage;
    }

}
