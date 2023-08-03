package org.embeddedt.modernfix.fabric.mixin.core;

import net.minecraft.client.multiplayer.ClientCommonPacketListenerImpl;
import org.embeddedt.modernfix.ModernFixClientFabric;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientCommonPacketListenerImpl.class)
public class ClientCommonPacketListenerImplMixin {
    @Inject(method = "handleUpdateTags", at = @At("RETURN"))
    private void signalTags(CallbackInfo ci) {
        ModernFixClientFabric.commonMod.onTagsUpdated();
    }
}
