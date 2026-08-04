package com.mobileminerong.planning.task;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.control.ActionController;
import com.mobileminerong.control.RotationController;
import com.mobileminerong.state.BotState;
import com.mobileminerong.MobileMinerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;

public class CombatFollowTask implements BotTask {

    private final RotationController rotationController = new RotationController();
    private boolean finished = false;

    @Override
    public void onStart(BotContext ctx) {
        ctx.setState(BotState.MOVING_TO_TARGET, "Engaging combat");
        ActionController.selectHotbarSlot(ctx, ctx.getCombatToolSlot());
    }

    @Override
    public void onTick(BotContext ctx) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        Entity target = ctx.getTargetEntity();
        if (target == null || !target.isAlive()) {
            onFailure(ctx, "Target lost or dead");
            return;
        }

        double distance = client.player.distanceTo(target);

        // Movement: Within 2 blocks
        if (distance > 2.0) {
            rotationController.setTarget(target.position(), client.player.getYRot(), client.player.getXRot());
            rotationController.tick(ctx);
            client.options.keyUp.setDown(true);
        } else {
            client.options.keyUp.setDown(false);
            // Face target and attack
            rotationController.setTarget(target.position(), client.player.getYRot(), client.player.getXRot());
            rotationController.tick(ctx);
            
            if (rotationController.isAligned()) {
                ActionController.startAttack();
            } else {
                ActionController.stopAttack();
            }
        }
    }

    @Override
    public boolean isFinished(BotContext ctx) {
        return finished;
    }

    @Override
    public void onFailure(BotContext ctx, String reason) {
        ActionController.stopAllInputs();
        finished = true;
        ctx.setState(BotState.RECOVERING, "Combat failed: " + reason);
        MobileMinerClient.TASK_ENGINE.reportTaskFailure(this, reason);
    }

    @Override
    public int getPriority() {
        return 50; // Combat Defense
    }

    @Override
    public String getName() {
        return "CombatFollowTask";
    }
}
