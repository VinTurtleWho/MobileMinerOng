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
    private int pathUpdateTicks = 0;
    private java.util.List<net.minecraft.core.BlockPos> currentPath = null;
    private int lastSelectedSlot = -1;
    private boolean isAttacking = false;
    private Entity lockedTarget = null;

    // Cognitive Ring Buffer for velocity
    private java.util.Deque<Vec3> velocityHistory = new java.util.ArrayDeque<>();
    private Vec3 lastPos = null;
    private com.mobileminerong.util.OrnsteinUhlenbeckDrift drift = new com.mobileminerong.util.OrnsteinUhlenbeckDrift();

    @Override
    public void onStart(BotContext ctx) {
        ctx.setState(BotState.MOVING_TO_TARGET, "Engaging combat");
        lastSelectedSlot = ctx.getCombatToolSlot();
        ActionController.selectHotbarSlot(ctx, lastSelectedSlot);
        ctx.getRotationEngine().setActive(true);
        this.lockedTarget = ctx.getTargetEntity();
        this.finished = false;
        this.targetLostTicks = 0;
        this.pathUpdateTicks = 0;
        this.currentPath = null;
        this.isAttacking = false;
        this.lastPos = lockedTarget != null ? lockedTarget.position() : null;
        this.velocityHistory.clear();
    }

    @Override
    public void onTick(BotContext ctx) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.player == null) return;

            // ... (Weapon check and Target validation remain the same)
            if (lastSelectedSlot != ctx.getCombatToolSlot()) {
                lastSelectedSlot = ctx.getCombatToolSlot();
                ActionController.selectHotbarSlot(ctx, lastSelectedSlot);
            }

            if (lockedTarget == null || !lockedTarget.isAlive()) {
                Entity newTarget = ctx.getTargetEntity();
                if (newTarget != null && newTarget.isAlive()) lockedTarget = newTarget;
                else { targetLostTicks++; if (targetLostTicks > 100) onFailure(ctx, "Target lost"); return; }
            }

            // Update Cognitive Ring Buffer
            Vec3 currentTargetPos = lockedTarget.position();
            if (lastPos != null) {
                Vec3 currentVelocity = currentTargetPos.subtract(lastPos);
                velocityHistory.addLast(currentVelocity);
                if (velocityHistory.size() > 3) velocityHistory.removeFirst();
            }
            lastPos = currentTargetPos;

            // Aiming formula: AimPoint = currentPos + (delayedVelocity * PredictionHorizon) + OU_Offset
            Vec3 delayedVelocity = velocityHistory.size() >= 3 ? velocityHistory.peekFirst() : Vec3.ZERO;
            drift.update();
            Vec3 aimPoint = lockedTarget.getEyePosition()
                .add(delayedVelocity.scale(5.0)) // PredictionHorizon
                .add(drift.getX(), drift.getY(), 0);

            // ... (Pathfinding/Movement logic, simplified as requested)
            double distance = client.player.distanceTo(lockedTarget);
            if (distance > 2.5) {
                // Rotation: Start/Update rotation toward aimPoint
                if (!ctx.getRotationEngine().isActive()) {
                    ctx.getRotationEngine().startRotation(client.player.getYRot(), client.player.getXRot(), aimPoint, 5);
                }
                ActionController.setKey(client.options.keyUp, true);
            } else {
                ActionController.setKey(client.options.keyUp, false);
                ActionController.setKey(client.options.keyDown, false);
                // Aim at target
                if (!ctx.getRotationEngine().isActive()) {
                    ctx.getRotationEngine().startRotation(client.player.getYRot(), client.player.getXRot(), aimPoint, 3);
                }
            }

            // Apply computed steps
            int[] steps = ctx.getRotationEngine().computeNextFrameSteps(aimPoint);
            ctx.setPendingMouseDelta(steps[0], steps[1]);

            // Attack logic (Schmitt trigger: 2.0 - 2.2)
            if (distance <= 2.0) isAttacking = true;
            else if (distance > 2.2) isAttacking = false;

            if (isAttacking) ActionController.startAttack();
            else ActionController.stopAttack();

        } catch (Exception e) {
            com.mobileminerong.debug.DebugLogger.error("COMBAT", "Error in CombatFollowTask: " + e.getMessage());
            onFailure(ctx, "Exception: " + e.getMessage());
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
