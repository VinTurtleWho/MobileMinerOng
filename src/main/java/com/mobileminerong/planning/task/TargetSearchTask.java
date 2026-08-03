package com.mobileminerong.planning.task;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.state.BotState;
import com.mobileminerong.MobileMinerClient;

public class TargetSearchTask implements BotTask {

    private boolean finished = false;

    @Override
    public void onStart(BotContext ctx) {
        ctx.setState(BotState.SEARCHING_TARGET, "Initial search started");
        checkTarget(ctx);
    }

    @Override
    public void onTick(BotContext ctx) {
        if (!finished) {
            checkTarget(ctx);
        }
    }

    private void checkTarget(BotContext ctx) {
        try {
            if (ctx.getCurrentTargetBlock() != null) {
                ctx.setState(BotState.MOVING_TO_TARGET, "Target acquired, transitioning to move");
                finished = true;
                ctx.addTaskEvent(getName(), "COMPLETED", "Target found");
            }
        } catch (Exception e) {
            onFailure(ctx, "Error checking target: " + e.getMessage());
        }
    }

    @Override
    public boolean isFinished(BotContext ctx) {
        return finished;
    }

    @Override
    public void onFailure(BotContext ctx, String reason) {
        ctx.setState(BotState.ERROR, "Target search failed: " + reason);
        MobileMinerClient.TASK_ENGINE.reportTaskFailure(this, reason);
        ctx.addTaskEvent(getName(), "FAILED", reason);
        finished = true;
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
