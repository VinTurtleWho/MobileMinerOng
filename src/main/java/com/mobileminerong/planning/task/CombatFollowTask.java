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

    @Override
    public void onStart(BotContext ctx) {
        ctx.setState(BotState.MOVING_TO_TARGET, "Engaging combat");
        lastSelectedSlot = ctx.getCombatToolSlot();
        ActionController.selectHotbarSlot(ctx, lastSelectedSlot);
        ctx.getRotationEngine().setActive(true);
        this.lockedTarget = ctx.getTargetEntity();
    }

    @Override
    public void onTick(BotContext ctx) {
        try {
            Minecraft client = Minecraft.getInstance();
            if (client == null || client.player == null) return;

            // 1. Weapon check
            if (lastSelectedSlot != ctx.getCombatToolSlot()) {
                lastSelectedSlot = ctx.getCombatToolSlot();
                ActionController.selectHotbarSlot(ctx, lastSelectedSlot);
            }

            // Ensure we have a valid target
            if (lockedTarget == null || !lockedTarget.isAlive()) {
                Entity newTarget = ctx.getTargetEntity();
                if (newTarget != null && newTarget.isAlive()) {
                    lockedTarget = newTarget;
                } else {
                    targetLostTicks++;
                    if (targetLostTicks > 100) onFailure(ctx, "Target lost");
                    return;
                }
            }

            targetLostTicks = 0; 
            double distance = client.player.distanceTo(lockedTarget);

            // Pathfinding/Movement logic
            if (distance > 2.5) {
                if (pathUpdateTicks++ >= 20 || currentPath == null || currentPath.isEmpty()) {
                    pathUpdateTicks = 0;
                    currentPath = com.mobileminerong.planning.pathfinding.AStarPathfinder.findPath(
                        ctx, client.player.blockPosition(), lockedTarget.blockPosition(), 500
                    );
                }

                if (currentPath != null && !currentPath.isEmpty()) {
                    net.minecraft.core.BlockPos nextNode = currentPath.get(0);
                    if (client.player.blockPosition().distSqr(nextNode) < 1.0) {
                        currentPath.remove(0);
                    } else {
                        // Rotation: Start/Update rotation toward node
                            if (!ctx.getRotationEngine().isActive()) {
                                ctx.getRotationEngine().startRotation(client.player.getYRot(), client.player.getXRot(), Vec3.atCenterOf(nextNode), 10);
                            }

                            ActionController.setKey(client.options.keyUp, true);
                        }
                        } else {
                        // Fallback to direct target rotation
                        if (!ctx.getRotationEngine().isActive()) {
                            ctx.getRotationEngine().startRotation(client.player.getYRot(), client.player.getXRot(), lockedTarget.getEyePosition(), 10);
                        }
                        ActionController.setKey(client.options.keyUp, true);
                        }
                        } else {
                        ActionController.setKey(client.options.keyUp, false);
                        ActionController.setKey(client.options.keyDown, false);

                        // Keep rotating to target
                        if (!ctx.getRotationEngine().isActive()) {
                        ctx.getRotationEngine().startRotation(client.player.getYRot(), client.player.getXRot(), lockedTarget.getEyePosition(), 5);
                        }
                        }

                        // Apply computed steps with live target position
                        int[] steps = ctx.getRotationEngine().computeNextFrameSteps(lockedTarget.getEyePosition());
                        ctx.setPendingMouseDelta(steps[0], steps[1]);

            // Attack logic
            if (distance <= 3.0) {
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
