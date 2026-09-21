package com.seailz.csdt.client.mixins;

import com.seailz.csdt.client.CoreShaderDevToolsClient;
import com.seailz.csdt.client.service.McpControlServerService;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {

    @Inject(method = "tick", at = @At("TAIL"))
    private void csdt$afterClientTick(CallbackInfo ci) {
        CoreShaderDevToolsClient.onEndClientTick((Minecraft) (Object) this);
    }

    @Inject(method = "close", at = @At("HEAD"))
    private void csdt$stopMcpControlServer(CallbackInfo ci) {
        McpControlServerService.stop();
    }
}
