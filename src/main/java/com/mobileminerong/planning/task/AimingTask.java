package com.mobileminerong.planning.task;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.state.BotState;
import com.mobileminerong.MobileMinerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class AimingTask implements BotTask {

    private final Vec3 targetVec;
    private int timeoutTicks = 200;
    private boolean finished = false;
    private boolean rotationStarted = false;

    public AimingTask(BlockPos targetPos) {
        this.targetVec = Vec3.atCenterOf(targetPos);
    }

    @Override
    public void onStart(BotContext ctx) {
        ctx.setState(BotState.AIMING, "Aiming at " + targetVec);
        rotationStarted = false;
        finished = false;
        timeoutTicks = 200;
    }

    @Override
    public void onTick(BotContext ctx) {
        timeoutTicks--;
        if (timeoutTicks <= 0) {
            onFailure(ctx, "Aiming timed out");
            return;
        }

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        // Start rotation once
        if (!rotationStarted) {
            ctx.getRotationEngine().startRotation(
                client.player.getYRot(), client.player.getXRot(), targetVec
            );
            rotationStarted = true;
        }

        // Feed mouse steps every tick
        int[] steps = ctx.getRotationEngine().computeNextFrameSteps();
        ctx.setPendingMouseDelta(steps[0], steps[1]);

        // Rotation complete when engine goes inactive
        if (!ctx.getRotationEngine().isActive()) {
            ctx.setState(BotState.MINING, "Aiming complete");
            finished = true;
        }
    }

    @Override
    public boolean isFinished(BotContext ctx) { return finished; }

    @Override
    public void onFailure(BotContext ctx, String reason) {
        ctx.getRotationEngine().abort();
        finished = true;
        ctx.setState(BotState.RECOVERING, "Aiming failed: " + reason);
        MobileMinerClient.TASK_ENGINE.reportTaskFailure(this, reason);
    }

    @Override
    public int getPriority() { return 12; }

    @Override
    public String getName() { return "AimingTask"; }
}
