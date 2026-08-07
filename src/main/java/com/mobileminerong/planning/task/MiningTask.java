package com.mobileminerong.planning.task;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.control.ActionController;
import com.mobileminerong.state.BotState;
import com.mobileminerong.MobileMinerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

public class MiningTask implements BotTask {

    private boolean finished = false;
    private BlockPos targetPos = null;
    private int timeoutTicks = 0;
    private static final int MINING_TIMEOUT = 200;

    @Override
    public void onStart(BotContext ctx) {
        this.targetPos = ctx.getCurrentTargetBlock();
        if (targetPos == null) {
            onFailure(ctx, "No target block set");
            return;
        }
        this.timeoutTicks = 0;
        this.finished = false;
        ActionController.selectHotbarSlot(ctx, ctx.getMiningToolSlot());
        ActionController.startMining(targetPos);
        ctx.setState(BotState.MINING, "Mining " + targetPos);
    }

    @Override
    public void onTick(BotContext ctx) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        if (targetPos == null) {
            onFailure(ctx, "Target lost");
            return;
        }

        // Check if block is gone (mined)
        Level level = client.level;
        if (level.getBlockState(targetPos).getCollisionShape(level, targetPos).isEmpty()) {
            ActionController.stopMining();
            ctx.setCurrentTargetBlock(null);
            ctx.setState(BotState.SEARCHING_TARGET, "Block mined successfully");
            finished = true;
            return;
        }

        // Timeout check
        timeoutTicks++;
        if (timeoutTicks > MINING_TIMEOUT) {
            onFailure(ctx, "Mining timeout — block not broken in " + MINING_TIMEOUT + " ticks");
            return;
        }

        // Keep holding attack
        ActionController.startMining(targetPos);
    }

    @Override
    public boolean isFinished(BotContext ctx) {
        return finished;
    }

    @Override
    public void onFailure(BotContext ctx, String reason) {
        ActionController.stopMining();
        finished = true;
        ctx.setState(BotState.RECOVERING, "Mining failed: " + reason);
        MobileMinerClient.TASK_ENGINE.reportTaskFailure(this, reason);
        ctx.addTaskEvent(getName(), "FAILED", reason);
    }

    @Override
    public int getPriority() {
        return 10;
    }

    @Override
    public String getName() {
        return "MiningTask";
    }
}
