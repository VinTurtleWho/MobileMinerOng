package com.mobileminerong.planning.task;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.state.BotState;
import com.mobileminerong.control.RotationController;
import com.mobileminerong.MobileMinerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

public class AimingTask implements BotTask {

    private final Vec3 targetVec;
    private final RotationController rotationController = new RotationController();
    private int timeoutTicks = 200;
    private boolean finished = false;

    public AimingTask(BlockPos targetPos) {
        this.targetVec = Vec3.atCenterOf(targetPos);
    }

    @Override
    public void onStart(BotContext ctx) {
        ctx.setState(BotState.AIMING, "Aiming at " + targetVec);
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            rotationController.setTarget(targetVec, client.player.getYRot(), client.player.getXRot());
        }
    }

    @Override
    public void onTick(BotContext ctx) {
        timeoutTicks--;
        if (timeoutTicks <= 0) {
            onFailure(ctx, "Aiming timed out");
            return;
        }

        rotationController.tick(ctx);

        if (rotationController.isAligned()) {
            ctx.setState(BotState.MINING, "Aiming complete");
            finished = true;
        }
    }

    @Override
    public boolean isFinished(BotContext ctx) {
        return finished;
    }

    @Override
    public void onFailure(BotContext ctx, String reason) {
        finished = true;
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
