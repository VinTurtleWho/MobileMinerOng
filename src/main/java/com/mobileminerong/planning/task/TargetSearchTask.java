package com.mobileminerong.planning.task;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.state.BotState;
import com.mobileminerong.MobileMinerClient;

public class TargetSearchTask implements BotTask {

    @Override
    public void onStart(BotContext ctx) {
        ctx.setState(BotState.SEARCHING_TARGET, "Initial search started");
        checkTarget(ctx);
    }

    @Override
    public void onTick(BotContext ctx) {
        checkTarget(ctx);
    }

    private void checkTarget(BotContext ctx) {
        try {
            if (ctx.getCurrentTargetBlock() != null) {
                // If a target is already acquired, we don't need to do anything.
                // The MovementTask/AimingTask/MiningTask chain will handle the rest.
                return;
            }
            
            // Logic to find a target would go here, 
            // but for now, we just ensure the task keeps running.
            
        } catch (Exception e) {
            onFailure(ctx, "Error checking target: " + e.getMessage());
        }
    }

    @Override
    public boolean isFinished(BotContext ctx) {
        // Never finish; this is a persistent background task.
        return false;
    }

    @Override
    public void onFailure(BotContext ctx, String reason) {
        ctx.setState(BotState.ERROR, "Target search failed: " + reason);
        MobileMinerClient.TASK_ENGINE.reportTaskFailure(this, reason);
        ctx.addTaskEvent(getName(), "FAILED", reason);
    }

    @Override
    public int getPriority() {
        return 20;
    }

    @Override
    public String getName() {
        return "TargetSearchTask";
    }
}
