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
    private long lastClickTime = 0;
    private boolean hasAttackedOnce = false;
    private java.util.Random random = new java.util.Random();

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
        this.lastClickTime = 0;
        this.hasAttackedOnce = false;
    }

    @Override
    public void onTick(BotContext ctx) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.player == null) return;

            // ... Weapon check...
            if (lastSelectedSlot != ctx.getCombatToolSlot()) {
                lastSelectedSlot = ctx.getCombatToolSlot();
                ActionController.selectHotbarSlot(ctx, lastSelectedSlot);
            }

            if (lockedTarget == null || !lockedTarget.isAlive()) {
                Entity newTarget = ctx.getTargetEntity();
                if (newTarget != null && newTarget.isAlive()) { lockedTarget = newTarget; hasAttackedOnce = false; }
                else { targetLostTicks++; if (targetLostTicks > 100) onFailure(ctx, "Target lost"); return; }
            }

            // ... Aiming (AimPoint logic same as previous step)...
            Vec3 currentTargetPos = lockedTarget.position();
            if (lastPos != null) {
                Vec3 currentVelocity = currentTargetPos.subtract(lastPos);
                velocityHistory.addLast(currentVelocity);
                if (velocityHistory.size() > 3) velocityHistory.removeFirst();
            }
            lastPos = currentTargetPos;

            Vec3 delayedVelocity = velocityHistory.size() >= 3 ? velocityHistory.peekFirst() : Vec3.ZERO;
            drift.update();
            Vec3 aimPoint = lockedTarget.getEyePosition()
                .add(delayedVelocity.scale(5.0))
                .add(drift.getX(), drift.getY(), 0);

            double distance = client.player.distanceTo(lockedTarget);
            
            // ... Movement logic same ...
            if (distance > 2.5) {
                if (!ctx.getRotationEngine().isActive()) {
                    ctx.getRotationEngine().startRotation(client.player.getYRot(), client.player.getXRot(), aimPoint, 5);
                }
                ActionController.setKey(client.options.keyUp, true);
            } else {
                ActionController.setKey(client.options.keyUp, false);
                ActionController.setKey(client.options.keyDown, false);
                if (!ctx.getRotationEngine().isActive()) {
                    ctx.getRotationEngine().startRotation(client.player.getYRot(), client.player.getXRot(), aimPoint, 3);
                }
            }
            int[] steps = ctx.getRotationEngine().computeNextFrameSteps(aimPoint);
            ctx.setPendingMouseDelta(steps[0], steps[1]);

            // Attack logic (BAS-Synced)
            if (distance <= 3.0) {
                int bas = 100; // placeholder for actual BAS
                boolean isShortbow = false;
                
                // One-Shot Mode (if mobHP < playerDmg)
                float mobHp = 100; // placeholder
                float playerDmg = 200; 

                if (mobHp <= playerDmg) {
                    if (!hasAttackedOnce) {
                        com.mobileminerong.control.ClickGenerator.performClick();
                        hasAttackedOnce = true;
                    }
                } else {
                    // Boss Mode (BAS-Synced)
                    if (System.currentTimeMillis() > lastClickTime + com.mobileminerong.control.ClickGenerator.calculateInterval(bas, isShortbow)) {
                        com.mobileminerong.control.ClickGenerator.performClick();
                        lastClickTime = System.currentTimeMillis();
                    }
                }
            } else {
                hasAttackedOnce = false;
            }

        } catch (Exception e) {
            com.mobileminerong.debug.DebugLogger.error("COMBAT", "Error: " + e.getMessage());
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
