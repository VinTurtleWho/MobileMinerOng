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
                    // Find nearest player or mob (simplified logic)
                    net.minecraft.client.Minecraft client = net.minecraft.client.Minecraft.getInstance();
                    if (client.level != null) {
                        for (net.minecraft.world.entity.Entity entity : client.level.entitiesForRendering()) {
                            if (entity instanceof net.minecraft.world.entity.player.Player && entity != client.player) {
                                ctx.setTargetEntity(entity);
                                break;
                            }
                        }
                    }
                }
            } else if (ctx.getCurrentTargetBlock() != null) {
                return;
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
