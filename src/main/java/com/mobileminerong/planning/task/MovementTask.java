package com.mobileminerong.planning.task;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.state.BotState;
import com.mobileminerong.planning.pathfinding.AStarPathfinder;
import com.mobileminerong.control.ActionController;
import com.mobileminerong.control.RotationController;
import com.mobileminerong.MobileMinerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class MovementTask implements BotTask {

    private final BlockPos targetPos;
    private List<BlockPos> path;
    private int currentIndex = 0;
    private boolean finished = false;
    private Vec3 lastPos;
    private int stuckTicks = 0;
    private int pathCooldown = 0; // Cooldown timer
    private boolean rotationInitialized = false; // Add this flag
    private final RotationController rotationController = new RotationController();

    public MovementTask(BlockPos targetPos) {
        this.targetPos = targetPos;
    }

    @Override
    public void onStart(BotContext ctx) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            onFailure(ctx, "No player context");
            return;
        }

        this.path = AStarPathfinder.findPath(ctx, client.player.blockPosition(), targetPos, 1000);
        if (path.isEmpty()) {
            onFailure(ctx, "No path found to " + targetPos);
            return;
        }

        this.rotationInitialized = false; // Reset flag
        ctx.setState(BotState.MOVING_TO_TARGET, "Moving to " + targetPos);
        this.lastPos = client.player.position();
    }

    @Override
    public void onTick(BotContext ctx) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        // Reduce cooldown
        if (pathCooldown > 0) pathCooldown--;

        // Stuck detection
        if (client.player.position().distanceToSqr(lastPos) < 0.01) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
            lastPos = client.player.position();
        }

        if (stuckTicks > 60 && pathCooldown == 0) {
            // Attempt to re-path only if cooldown allows
            this.path = AStarPathfinder.findPath(ctx, client.player.blockPosition(), targetPos, 1000);
            currentIndex = 0;
            pathCooldown = 100; // 5 seconds cooldown
            stuckTicks = 0;
            return;
        }

        if (stuckTicks > 120) {
            onFailure(ctx, "Bot stuck for too long");
            return;
        }

        if (currentIndex >= path.size()) {
            finished = true;
            ActionController.stopAllInputs();
            ctx.setState(BotState.AIMING, "Reached destination");
            return;
        }

        BlockPos currentWaypoint = path.get(currentIndex);
        Vec3 waypointVec = Vec3.atCenterOf(currentWaypoint);

        // Advance waypoint if close
        if (client.player.position().distanceToSqr(waypointVec) < 0.5) {
            currentIndex++;
            rotationInitialized = false; // Reset flag for next waypoint
            ActionController.stopAllInputs();
            return;
        }

        // Initialize rotation for this waypoint only once
        if (!rotationInitialized) {
            rotationController.setTarget(waypointVec, client.player.getYRot(), client.player.getXRot());
            rotationInitialized = true;
        }

        // Tick rotation every tick
        rotationController.tick(ctx);
        
        // Only move forward once facing the right direction
        if (rotationController.isAligned()) {
            client.options.keyUp.setDown(true);
        } else {
            client.options.keyUp.setDown(false);
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
        ctx.setState(BotState.RECOVERING, "Movement failed: " + reason);
        MobileMinerClient.TASK_ENGINE.reportTaskFailure(this, reason);
    }

    @Override
    public int getPriority() {
        return 15;
    }

    @Override
    public String getName() {
        return "MovementTask";
    }
}
