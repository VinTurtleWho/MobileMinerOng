package com.mobileminerong.planning.task;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.control.ActionController;
import com.mobileminerong.control.RotationController;
import com.mobileminerong.state.BotState;
import com.mobileminerong.MobileMinerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.util.Mth;

public class CombatFollowTask implements BotTask {

    private boolean finished = false;
    private int targetLostTicks = 0;
    private int stallTicks = 0;
    private int lastSelectedSlot = -1;
    private boolean isAttacking = false;
    private Vec3 lastTargetPosition = null;

    @Override
    public void onStart(BotContext ctx) {
        ctx.setState(BotState.MOVING_TO_TARGET, "Engaging combat");
        lastSelectedSlot = ctx.getCombatToolSlot();
        ActionController.selectHotbarSlot(ctx, lastSelectedSlot);
    }

    @Override
    public void onTick(BotContext ctx) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        // 1. Weapon check
        if (lastSelectedSlot != ctx.getCombatToolSlot()) {
            lastSelectedSlot = ctx.getCombatToolSlot();
            ActionController.selectHotbarSlot(ctx, lastSelectedSlot);
        }

        Entity target = ctx.getTargetEntity();
        com.mobileminerong.debug.DebugLogger.debug("COMBAT", "CombatFollowTask ticking, target: " + (target != null ? target.getName().getString() : "null"));
        
        // Target lost check
        if (target == null) {
            targetLostTicks++;
            if (targetLostTicks > 100) { 
                onFailure(ctx, "Target lost");
            }
            return;
        }

        // Target alive check
        if (!target.isAlive()) {
            finished = true;
            ActionController.stopAttack();
            ctx.setState(BotState.IDLE, "Target defeated");
            return;
        }
        
        targetLostTicks = 0; 
        double distance = client.player.distanceTo(target);

        // Movement logic
        if (distance > 2.2) {
            client.options.keyUp.setDown(true);
            client.options.keyDown.setDown(false);
        } else if (distance < 1.8) {
            client.options.keyUp.setDown(false);
            client.options.keyDown.setDown(true);
        } else {
            client.options.keyUp.setDown(false);
            client.options.keyDown.setDown(false);
        }

        // Target update for RotationEngine
        int[] steps = ctx.getRotationEngine().computeNextFrameSteps(client.player.getYRot(), client.player.getXRot(), target.position());
        ctx.setPendingMouseDelta(steps[0], steps[1]);
        
        // Attack logic (using distance check)
        if (distance <= 2.5) {
            if (!isAttacking) {
                ActionController.startAttack();
                isAttacking = true;
            }
        } else {
            if (isAttacking) {
                ActionController.stopAttack();
                isAttacking = false;
            }
        }
    }

    @Override
    public boolean isFinished(BotContext ctx) {
        if (finished) ctx.getRotationEngine().abort();
        return finished;
    }

    @Override
    public void onFailure(BotContext ctx, String reason) {
        ctx.getRotationEngine().abort();
        ActionController.stopAllInputs();
        finished = true;
        ctx.setState(BotState.RECOVERING, "Combat failed: " + reason);
        MobileMinerClient.TASK_ENGINE.reportTaskFailure(this, reason);
    }

    @Override
    public int getPriority() {
        return 50;
    }

    @Override
    public String getName() {
        return "CombatFollowTask";
    }
}
