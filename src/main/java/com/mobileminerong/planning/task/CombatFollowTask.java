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

    @Override
    public void onStart(BotContext ctx) {
        ctx.setState(BotState.MOVING_TO_TARGET, "Engaging combat");
        lastSelectedSlot = ctx.getCombatToolSlot();
        ActionController.selectHotbarSlot(ctx, lastSelectedSlot);
        ctx.getRotationEngine().setActive(true);
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

            Entity target = ctx.getTargetEntity();
            
            // Target lost check
            if (target == null) {
                targetLostTicks++;
                if (targetLostTicks > 100) onFailure(ctx, "Target lost");
                return;
            }

            if (!target.isAlive()) {
                finished = true;
                ActionController.stopAttack();
                ctx.setState(BotState.IDLE, "Target defeated");
                return;
            }
            
            targetLostTicks = 0; 
            double distance = client.player.distanceTo(target);

            // Pathfinding/Movement logic
            if (distance > 2.5) {
                if (pathUpdateTicks++ >= 20 || currentPath == null || currentPath.isEmpty()) {
                    pathUpdateTicks = 0;
                    currentPath = com.mobileminerong.planning.pathfinding.AStarPathfinder.findPath(
                        ctx, client.player.blockPosition(), target.blockPosition(), 500
                    );
                }

                if (currentPath != null && !currentPath.isEmpty()) {
                    // Simple path following: look at next node
                    net.minecraft.core.BlockPos nextNode = currentPath.get(0);
                    if (client.player.blockPosition().distSqr(nextNode) < 1.0) {
                        currentPath.remove(0);
                    } else {
                        // Move toward node
                        Vec3 nodeVec = Vec3.atCenterOf(nextNode);
                        // For this implementation, we simplify movement back to key presses but 
                        // now directed toward the path node, not the entity itself.
                        // (Ideally, we would use a proper MovementTask here).
                        ActionController.setKey(client.options.keyUp, true);
                    }
                } else {
                    // Fallback to direct movement if pathing fails
                    ActionController.setKey(client.options.keyUp, true);
                }
            } else {
                ActionController.setKey(client.options.keyUp, false);
                ActionController.setKey(client.options.keyDown, false);
            }

            // Target update for RotationEngine
            int[] steps = ctx.getRotationEngine().computeNextFrameSteps(client.player.getYRot(), client.player.getXRot(), target.position());
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
