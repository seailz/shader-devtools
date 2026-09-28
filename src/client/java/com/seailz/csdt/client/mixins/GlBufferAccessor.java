package com.seailz.csdt.client.mixins;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(targets = "com.mojang.renderpearl.backend.opengl.GlBuffer")
public interface GlBufferAccessor {

    @Accessor("handle")
    int csdt$getHandle();
}
