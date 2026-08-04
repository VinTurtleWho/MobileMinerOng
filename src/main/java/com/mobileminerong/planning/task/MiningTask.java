package com.mobileminerong.planning.task;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.control.InventoryManager;
import com.mobileminerong.control.ActionController;
import com.mobileminerong.state.BotState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;

public class MiningTask implements BotTask {

    private boolean finished = false;

    @Override
    public void onStart(BotContext ctx) {
        InventoryManager.selectHotbarSlot(ctx.getMiningToolSlot());
        ctx.setState(BotState.MINING, "Mining started");
    }

    @Override
    public void onTick(BotContext ctx) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        BlockPos targetPos = ctx.getCurrentTargetBlock();
        if (targetPos == null) {
            // No target found, keep searching
            return;
        }

        // Check if we are still aiming at the target
        // (Assuming AimingTask handled the rotation)
        
        // Start breaking
        ActionController.startMining(targetPos);
    }

    @Override
    public boolean isFinished(BotContext ctx) {
        return finished;
    }

    @Override
    public void onFailure(BotContext ctx, String reason) {
        ActionController.stopMining();
        ctx.setState(BotState.RECOVERING, "Mining failed: " + reason);
    }

    @Override
    public int getPriority() {
        return 10; // Standard Mining
    }

    @Override
    public String getName() {
        return "MiningTask";
    }
}
