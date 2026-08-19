package com.seailz.csdt.client.mixins;

import com.seailz.csdt.client.service.ShaderDebugRuntimeService;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(targets = "com.mojang.renderpearl.backend.opengl.GlCommandEncoder")
public abstract class GlCommandEncoderMixin {

    @Inject(method = "setupDraw", at = @At("TAIL"))
    private void csdt$bindShaderDebugStorage(@Coerce Object pass, CallbackInfo ci) {
        ShaderDebugRuntimeService.bindStorageBuffer();
    }
}
