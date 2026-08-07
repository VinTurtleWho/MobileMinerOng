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
            if (ctx.getMode() == com.mobileminerong.state.MacroMode.COMBAT) {
                if (ctx.getTargetEntity() == null) {
                    // Logic to find nearest mob...
                }
            } else if (ctx.getCurrentTargetBlock() != null) {
                // Don't spawn chain if one is already active
                boolean chainActive = MobileMinerClient.TASK_ENGINE.hasTaskOfType(MovementTask.class)
                    || MobileMinerClient.TASK_ENGINE.hasTaskOfType(AimingTask.class)
                    || MobileMinerClient.TASK_ENGINE.hasTaskOfType(MiningTask.class);
                
                if (!chainActive) {
                    MobileMinerClient.TASK_ENGINE.registerTask(
                        new MovementTask(ctx.getCurrentTargetBlock())
                    );
                }
            }
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
