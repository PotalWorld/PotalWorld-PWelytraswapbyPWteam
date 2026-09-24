package com.pwteam.pwelytraswap.mixins;

import com.pwteam.pwelytraswap.PWElytraSwapClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public class FlightSwapMixin {
    @Inject(
            method = "aiStep",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;tryToStartFallFlying()Z")
    )
    private void swapToElytraOnFlightAttempt(CallbackInfo callbackInfo) {
        if (PWElytraSwapClient.enabled && PWElytraSwapClient.isSecondJumpReady()) {
            PWElytraSwapClient.tryWearElytra(Minecraft.getInstance());
        }
    }
}
