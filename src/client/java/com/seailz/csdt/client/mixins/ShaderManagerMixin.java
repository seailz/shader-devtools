package com.seailz.csdt.client.mixins;

import net.minecraft.client.renderer.ShaderManager;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ShaderManager.class)
public interface ShaderManagerMixin {

    @Invoker("prepare")
    ShaderManager.Configs csdt$prepare(ResourceManager manager, ProfilerFiller profiler);

    @Invoker("apply")
    void csdt$apply(ShaderManager.Configs preparations, ResourceManager manager, ProfilerFiller profiler);
}
