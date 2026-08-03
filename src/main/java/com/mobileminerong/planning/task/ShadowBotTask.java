package com.mobileminerong.planning.task;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.state.BotState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

public class ShadowBotTask implements BotTask {

    private MovementTask movementTask = null;
    private AimingTask aimingTask = null;

    @Override
    public void onStart(BotContext ctx) {
        ctx.setState(BotState.SEARCHING_TARGET, "Shadowing enabled");
    }

    @Override
    public void onTick(BotContext ctx) {
        Player targetPlayer = ctx.getTargetPlayer();
        if (targetPlayer == null) return;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        double dist = client.player.distanceTo(targetPlayer);
        Vec3 targetPos = targetPlayer.position();

        // If distance > 3, move to 2 blocks away
        if (dist > 3.0) {
            // Very basic: move towards player position
            BlockPos targetBlock = BlockPos.containing(targetPos.x, targetPos.y, targetPos.z);
            if (movementTask == null || movementTask.isFinished(ctx)) {
                movementTask = new MovementTask(targetBlock);
                movementTask.onStart(ctx);
            } else {
                movementTask.onTick(ctx);
            }
        } else {
            // If distance < 3, just aim at player
            BlockPos targetBlock = BlockPos.containing(targetPos.x, targetPos.y, targetPos.z);
            if (aimingTask == null || aimingTask.isFinished(ctx)) {
                aimingTask = new AimingTask(targetBlock);
                aimingTask.onStart(ctx);
            } else {
                aimingTask.onTick(ctx);
            }
        }
    }

    @Override
    public boolean isFinished(BotContext ctx) {
        return false; // Persistent
    }

    @Override
    public void onFailure(BotContext ctx, String reason) {
        ctx.setState(BotState.ERROR, "Shadowing failed: " + reason);
    }

    @Override
    public int getPriority() {
        return 100; // High priority for testing
    }

    @Override
    public String getName() {
        return "ShadowBotTask";
    }
}
