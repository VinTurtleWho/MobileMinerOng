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

    @Inject(method = "handleAccumulatedMovement", at = @At("HEAD"))
    private void onHandleAccumulatedMovement(CallbackInfo ci) {
        if (!MobileMinerClient.BOT_CONTEXT.isActive()) return;

        // Bot Control
        int[] steps = MobileMinerClient.BOT_CONTEXT.getAndClearPendingMouseDelta();
        if (steps[0] != 0 || steps[1] != 0) {
            this.accumulatedDX = steps[0];
            this.accumulatedDY = steps[1];
            com.mobileminerong.debug.DebugLogger.debug("MOUSE", "Injected delta: " + steps[0] + ", " + steps[1]);
        }
    }
}
