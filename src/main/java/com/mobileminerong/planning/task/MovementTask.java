package com.mobileminerong.planning.task;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.state.BotState;
import com.mobileminerong.MobileMinerClient;
import com.mobileminerong.planning.pathfinding.AStarPathfinder;
import com.mobileminerong.control.ActionController;
import com.mobileminerong.control.RotationController;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class MovementTask implements BotTask {

    private List<BlockPos> path;
    private int currentWaypointIndex = 0;
    private Vec3 lastPos;
    private int stuckTicks = 0;
    private final RotationController rotationController = new RotationController();

    @Override
    public void onStart(BotContext ctx) {
        ctx.setState(BotState.MOVING_TO_TARGET, "Calculating path...");
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) {
            onFailure(ctx, "Player null");
            return;
        }

        path = AStarPathfinder.findPath(ctx, client.player.blockPosition(), ctx.getCurrentTargetBlock(), 1000);
        if (path.isEmpty()) {
            onFailure(ctx, "No path found");
            return;
        }

        currentWaypointIndex = 0;
        lastPos = client.player.position();
        stuckTicks = 0;
    }

    @Override
    public void onTick(BotContext ctx) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        // Stuck detection
        Vec3 currentPos = client.player.position();
        if (currentPos.distanceToSqr(lastPos) < 0.01) {
            stuckTicks++;
        } else {
            stuckTicks = 0;
            lastPos = currentPos;
        }

        if (stuckTicks > 60) {
            onFailure(ctx, "Stuck detected");
            return;
        }

        if (currentWaypointIndex >= path.size()) {
            ActionController.stopAllInputs();
            ctx.setState(BotState.AIMING, "Reached destination");
            return; 
        }

        BlockPos target = path.get(currentWaypointIndex);
        Vec3 targetVec = Vec3.atCenterOf(target);
        double distSqr = currentPos.distanceToSqr(targetVec);

        // Improved waypoint check: check if passed or very close
        if (distSqr < 0.5) {
            currentWaypointIndex++;
            return;
        }

        // Rotate
        rotationController.setTarget(targetVec, client.player.getYRot(), client.player.getXRot());
        boolean aligned = rotationController.tick(ctx);

        // Only move forward if reasonably aligned
        if (aligned || distSqr > 1.0) {
            client.options.keyUp.setDown(true);
        } else {
            client.options.keyUp.setDown(false);
        }
    }

    @Override
    public boolean isFinished(BotContext ctx) {
        return ctx.getCurrentState() == BotState.AIMING || ctx.getCurrentState() == BotState.ERROR;
    }

    @Override
    public void onFailure(BotContext ctx, String reason) {
        ActionController.stopAllInputs();
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
