package com.mobileminerong.mixin;

import com.mobileminerong.MobileMinerClient;
import com.mobileminerong.planning.task.CombatFollowTask;
import net.minecraft.client.MouseHandler;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(value = MouseHandler.class, priority = 2000)
public class MouseHandlerMixin {
    @Shadow private double accumulatedDX;
    @Shadow private double accumulatedDY;

    @Inject(method = "handleAccumulatedMovement", at = @At("HEAD"), order = 10000)
    private void onHandleAccumulatedMovement(CallbackInfo ci) {
        if (!MobileMinerClient.BOT_CONTEXT.isActive()) return;

        // Human Input Gate: If physical mouse moved significantly, yield control.
        if (Math.abs(accumulatedDX) > 1.0 || Math.abs(accumulatedDY) > 1.0) {
            MobileMinerClient.BOT_CONTEXT.getRotationEngine().abort();
            return;
        }

        // Bot Control
        int[] steps = MobileMinerClient.BOT_CONTEXT.getAndClearPendingMouseDelta();
        if (steps[0] != 0 || steps[1] != 0) {
            this.accumulatedDX = steps[0];
            this.accumulatedDY = steps[1];
        }
    }
}
