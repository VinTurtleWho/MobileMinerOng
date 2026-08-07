package com.mobileminerong.planning.task;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.state.BotState;
import com.mobileminerong.planning.pathfinding.AStarPathfinder;
import com.mobileminerong.control.ActionController;
import com.mobileminerong.MobileMinerClient;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;

public class MovementTask implements BotTask {

    // Configuration
    private static final int MAX_ITERATIONS = 5000;
    private static final int LOOKAHEAD = 3;            // Rotate toward N waypoints ahead
    private static final float MOVE_ANGLE_THRESHOLD = 20.0f; // Start walking within 20°
    private static final double WAYPOINT_REACH_DIST = 0.6;   // Advance when within 0.6 blocks
    private static final int STUCK_THRESHOLD = 80;            // Ticks before repath
    private static final int STUCK_FAIL_THRESHOLD = 160;      // Ticks before full failure
    private static final int REPATH_COOLDOWN = 60;            // Ticks between repathing

    // State
    private final BlockPos targetPos;
    private List<BlockPos> path = Collections.emptyList();
    private int currentIndex = 0;
    private boolean finished = false;

    // Async pathfinding
    private CompletableFuture<List<BlockPos>> pathFuture = null;
    private boolean waitingForPath = false;

    // Stuck detection
    private Vec3 lastPos = Vec3.ZERO;
    private int stuckTicks = 0;
    private int pathCooldown = 0;

    // Rotation state
    private boolean rotationInitialized = false;

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

        this.currentIndex = 0;
        this.finished = false;
        this.stuckTicks = 0;
        this.pathCooldown = 0;
        this.rotationInitialized = false;
        this.lastPos = client.player.position();

        ctx.setState(BotState.MOVING_TO_TARGET, "Computing path to " + targetPos);
        requestPath(client.player.blockPosition(), client.level);
    }

    @Override
    public void onTick(BotContext ctx) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null) return;

        // Waiting for async path result
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

                    this.path = result;
                    this.currentIndex = 0;
                    this.rotationInitialized = false;
                    ctx.setState(BotState.MOVING_TO_TARGET, "Path found, moving to " + targetPos);
                } catch (Exception e) {
                    onFailure(ctx, "Path computation failed: " + e.getMessage());
                    return;
                }
            } else {
                // Still waiting — do nothing this tick
                return;
            }
        }

        if (path.isEmpty()) return;

        // Cooldown tick
        if (pathCooldown > 0) pathCooldown--;

        // Stuck detection
        Vec3 currentPlayerPos = client.player.position();
        if (currentPlayerPos.distanceToSqr(lastPos) < 0.005) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
            lastPos = currentPlayerPos;
        }

        if (stuckTicks > STUCK_THRESHOLD && pathCooldown == 0) {
            // Repath from current position
            ctx.getRotationEngine().abort();
            ActionController.stopAllInputs();
            stuckTicks = 0;
            pathCooldown = REPATH_COOLDOWN;
            rotationInitialized = false;
            requestPath(client.player.blockPosition(), client.level);
            return;
        }

        if (stuckTicks > STUCK_FAIL_THRESHOLD) {
            onFailure(ctx, "Stuck — could not find alternative route");
            return;
        }

        // Destination reached
        if (currentIndex >= path.size()) {
            ActionController.stopAllInputs();
            ctx.getRotationEngine().abort();
            ctx.setState(BotState.AIMING, "Reached destination");
            finished = true;
            return;
        }

        BlockPos currentWaypoint = path.get(currentIndex);
        Vec3 waypointVec = Vec3.atCenterOf(currentWaypoint);

        // Advance waypoint if close enough
        if (currentPlayerPos.distanceToSqr(waypointVec) < WAYPOINT_REACH_DIST * WAYPOINT_REACH_DIST) {
            currentIndex++;
            rotationInitialized = false;
            // Don't abort rotation or stop — keep momentum
            return;
        }

        // Determine lookahead target for rotation
        // Rotate toward the waypoint LOOKAHEAD steps ahead for smoother arc movement
        int lookTarget = Math.min(currentIndex + LOOKAHEAD, path.size() - 1);
        Vec3 lookVec = Vec3.atCenterOf(path.get(lookTarget));

        // Start rotation if not initialized for this waypoint group
        if (!rotationInitialized || !ctx.getRotationEngine().isActive()) {
            ctx.getRotationEngine().startRotation(
                client.player.getYRot(),
                client.player.getXRot(),
                lookVec,
                4 // Fast rotation for movement — 4 ticks
            );
            rotationInitialized = true;
        }

        // Feed rotation steps to mouse pipeline every tick
        int[] steps = ctx.getRotationEngine().computeNextFrameSteps(lookVec);
        ctx.setPendingMouseDelta(steps[0], steps[1]);

        // Check angular error to lookahead target
        float angleError = getAngleError(client.player.getYRot(), lookVec, currentPlayerPos);

        // Move forward when within threshold — no full stop required
        if (angleError < MOVE_ANGLE_THRESHOLD) {
            client.options.keyUp.setDown(true);
            // Sprint if not too close to next waypoint (avoid overshooting)
            double distToNext = currentPlayerPos.distanceToSqr(waypointVec);
            client.options.keySprint.setDown(distToNext > 4.0);
        } else {
            // Facing wrong direction — stop and rotate
            client.options.keyUp.setDown(false);
            client.options.keySprint.setDown(false);
        }
    }

    /**
     * Calculate the horizontal angular error between player's current yaw
     * and the direction to the target position.
     */
    private float getAngleError(float playerYaw, Vec3 target, Vec3 playerPos) {
        double dx = target.x - playerPos.x;
        double dz = target.z - playerPos.z;
        float targetYaw = (float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f;
        return Math.abs(Mth.wrapDegrees(targetYaw - playerYaw));
    }

    /**
     * Request an async A* path computation on a background thread.
     * Sets waitingForPath = true until the result arrives.
     */
    private void requestPath(BlockPos from, Level world) {
        waitingForPath = true;
        // Capture world reference — Level is not thread-safe for writes but
        // getBlockState() reads are safe from background threads in Fabric
        pathFuture = CompletableFuture.supplyAsync(() ->
            AStarPathfinder.findPath(from, targetPos, MAX_ITERATIONS, world)
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
