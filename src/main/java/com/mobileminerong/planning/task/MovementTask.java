package com.mobileminerong.planning.task;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.state.BotState;
import com.mobileminerong.planning.pathfinding.LazyThetaPathfinder;
import com.mobileminerong.planning.pathfinding.PathTrajectory;
import com.mobileminerong.control.ActionController;
import com.mobileminerong.MobileMinerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MovementTask implements BotTask {

    private static final int MAX_ITERATIONS = 5000;
    private final BlockPos targetPos;
    private PathTrajectory trajectory = null;
    private boolean finished = false;

    // Async pathfinding
    private CompletableFuture<List<BlockPos>> pathFuture = null;
    private boolean waitingForPath = false;

    // Stuck detection
    private Vec3 lastPos = Vec3.ZERO;
    private int stuckTicks = 0;
    private int pathCooldown = 0;

    public MovementTask(BlockPos targetPos) {
        this.targetPos = targetPos;
    }

    @Override
    public void onStart(BotContext ctx) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) {
            onFailure(ctx, "No player or world context");
            return;
        }

        this.finished = false;
        this.stuckTicks = 0;
        this.pathCooldown = 0;
        this.lastPos = client.player.position();
        this.trajectory = null;

        ctx.setState(BotState.MOVING_TO_TARGET, "Computing path to " + targetPos);
        requestPath(client.player.blockPosition(), client.level);
    }

    @Override
    public void onTick(BotContext ctx) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        // Process async path results
        if (waitingForPath) {
            if (pathFuture != null && pathFuture.isDone()) {
                try {
                    List<BlockPos> result = pathFuture.get();
                    waitingForPath = false;
                    pathFuture = null;

                    if (result.isEmpty()) {
                        onFailure(ctx, "No path found to " + targetPos);
                        return;
                    }

                    this.trajectory = new PathTrajectory(result);
                    ctx.setState(BotState.MOVING_TO_TARGET, "Spline mapped, engaging momentum");
                } catch (Exception e) {
                    onFailure(ctx, "Spline computation failed: " + e.getMessage());
                    return;
                }
            } else {
                return; // Wait for path
            }
        }

        if (trajectory == null) return;

        // Stuck detection
        Vec3 currentPlayerPos = client.player.position();
        if (currentPlayerPos.distanceToSqr(lastPos) < 0.005) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
            lastPos = currentPlayerPos;
        }

        if (stuckTicks > 80 && pathCooldown == 0) {
            ctx.getRotationEngine().abort();
            ActionController.stopAllInputs();
            stuckTicks = 0;
            pathCooldown = 60;
            requestPath(client.player.blockPosition(), client.level);
            return;
        }

        if (stuckTicks > 160) {
            onFailure(ctx, "Stuck - Alternative pathfinding exhausted");
            return;
        }

        if (pathCooldown > 0) pathCooldown--;

        // Destination reached check
        if (trajectory.isDestinationReached(currentPlayerPos)) {
            ActionController.stopAllInputs();
            ctx.getRotationEngine().abort();
            ctx.setState(BotState.AIMING, "Destination achieved smoothly");
            finished = true;
            return;
        }

        // Fetch dynamic lookahead point
        Vec3 aimPoint = trajectory.getLookahead(currentPlayerPos);

        // Feed rotation steps directly into Mouse Pipeline
        if (!ctx.getRotationEngine().isActive()) {
            ctx.getRotationEngine().startRotation(
                client.player.getYRot(),
                client.player.getXRot(),
                aimPoint
            );
        } else {
            ctx.getRotationEngine().updateTarget(aimPoint);
        }

        // Maintain forward sprint momentum
        client.options.keyUp.setDown(true);
        client.options.keySprint.setDown(true);
        client.player.setSprinting(true);
    }

    private void requestPath(BlockPos from, Level world) {
        waitingForPath = true;
        pathFuture = CompletableFuture.supplyAsync(() ->
            LazyThetaPathfinder.findPath(from, targetPos, MAX_ITERATIONS, world)
        );
    }

    @Override
    public boolean isFinished(BotContext ctx) {
        return finished;
    }

    @Override
    public void onFailure(BotContext ctx, String reason) {
        ctx.getRotationEngine().abort();
        ActionController.stopAllInputs();
        if (pathFuture != null) {
            pathFuture.cancel(true);
            pathFuture = null;
        }
        waitingForPath = false;
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
