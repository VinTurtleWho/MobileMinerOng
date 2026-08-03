package com.mobileminerong.planning.task;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.state.BotState;
import com.mobileminerong.MobileMinerClient;
import com.mobileminerong.control.RotationController;
import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

public class AimingTask implements BotTask {

    private final RotationController rotationController = new RotationController();
    private int timeoutTicks = 0;

    @Override
    public void onStart(BotContext ctx) {
        ctx.setState(BotState.AIMING, "Aligning aim to target...");
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            onFailure(ctx, "Player null");
            return;
        }

        Vec3 targetPos = Vec3.atCenterOf(ctx.getCurrentTargetBlock());
        rotationController.setTarget(targetPos, client.player.getYRot(), client.player.getXRot());
        
        // Dynamically set timeout: rotation time + buffer
        // RotationController defines totalTicks based on delta
        // We need access to that value. Since it's an instance, we can calculate it again or expose it.
        // For now, let's use a safe upper bound based on the rotation logic.
        this.timeoutTicks = 100; // Sufficient buffer
    }

    @Override
    public void onTick(BotContext ctx) {
        if (rotationController.tick(ctx)) {
            ctx.setState(BotState.MINING, "Aligned to target");
        }
        
        timeoutTicks--;
        if (timeoutTicks <= 0) {
            onFailure(ctx, "Aiming timed out");
        }
    }

    @Override
    public boolean isFinished(BotContext ctx) {
        return ctx.getState() == BotState.MINING || ctx.getState() == BotState.ERROR;
    }

    @Override
    public void onFailure(BotContext ctx, String reason) {
        ctx.setState(BotState.RECOVERING, "Aiming failed: " + reason);
        MobileMinerClient.TASK_ENGINE.reportTaskFailure(this, reason);
    }

    @Override
    public int getPriority() {
        return 12;
    }

    @Override
    public String getName() {
        return "AimingTask";
    }
}
