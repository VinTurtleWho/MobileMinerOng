package com.mobileminerong.planning.task;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.control.RotationController;
import com.mobileminerong.diagnostic.DiagnosticManager;
import com.mobileminerong.perception.BlockScanner;
import com.mobileminerong.perception.ScoreboardParser;
import com.mobileminerong.planning.pathfinding.AStarPathfinder;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.List;

public class DiagnosticTestTask implements BotTask {

    private boolean finished = false;
    private static long lastRun = 0;
    private static final long COOLDOWN = 5000;

    @Override
    public void onStart(BotContext ctx) {

        long now = System.currentTimeMillis();

        if (now - lastRun < COOLDOWN) {
            finished = true;
            return;
        }

        lastRun = now;

        Minecraft client = Minecraft.getInstance();

        if (client.player == null) {
            DiagnosticManager.error("No player instance");
            finished = true;
            return;
        }

        long start = System.currentTimeMillis();

        DiagnosticManager.event("=== Diagnostic Started ===");

        client.player.sendSystemMessage(
                Component.literal("§a[MobileMinerOng] Diagnostic started")
        );


        // ENVIRONMENT
        ScoreboardParser.updateZone(ctx);

        DiagnosticManager.report(
                "ENVIRONMENT",
                "Zone: " + ctx.getCurrentZone()
        );


        // PERCEPTION
        List<BlockPos> targets =
                BlockScanner.findTargetOres(ctx, 10);

        DiagnosticManager.report(
                "PERCEPTION",
                "Targets found: " + targets.size()
        );


        if (!targets.isEmpty()) {

            BlockPos target = targets.get(0);

            ctx.setCurrentTargetBlock(target);


            // PATHFINDING
            BlockPos player =
                    client.player.blockPosition();

            List<BlockPos> path =
                    AStarPathfinder.findPath(
                            ctx,
                            player,
                            target,
                            500
                    );


            DiagnosticManager.report(
                    "PATHFINDING",
                    "Path nodes: " + path.size()
            );


            // ROTATION
            Vec3 targetVec =
                    target.getCenter();

            RotationController.lookAt(
                    ctx,
                    targetVec
            );


            DiagnosticManager.debug(
                    "ROTATION",
                    "Target: " + targetVec
            );


            DiagnosticManager.report(
                    "PLAYER",
                    "Yaw: " +
                    (int)client.player.getYRot()
                    +
                    " Pitch: "
                    +
                    (int)client.player.getXRot()
            );


        } else {

            DiagnosticManager.warn(
                    "No targets detected"
            );
        }


        long time =
                System.currentTimeMillis() - start;


        DiagnosticManager.event(
                "=== Diagnostic Complete (" + time + "ms) ==="
        );


        finished = true;
    }


    @Override
    public void onTick(BotContext ctx) {}


    @Override
    public boolean isFinished(BotContext ctx) {
        return finished;
    }


    @Override
    public void onFailure(
            BotContext ctx,
            String reason
    ) {
        DiagnosticManager.error(reason);
    }


    @Override
    public int getPriority() {
        return 100;
    }


    @Override
    public String getName() {
        return "DiagnosticTestTask";
    }
}
