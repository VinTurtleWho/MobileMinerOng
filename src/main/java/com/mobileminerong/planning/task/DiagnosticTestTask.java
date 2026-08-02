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

    @Override
    public void onStart(BotContext ctx) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        DiagnosticManager.event("Diagnostic run started");
        client.player.sendSystemMessage(Component.literal("§a[MobileMinerOng] --- RUNNING DIAGNOSTIC TEST ---"));

        // 1. Perception: Zone
        ScoreboardParser.updateZone(ctx);
        String zoneMsg = "Current Zone: " + ctx.getCurrentZone();
        DiagnosticManager.report("ENVIRONMENT", zoneMsg);
        client.player.sendSystemMessage(Component.literal("§b[1/5 Perception] §f" + zoneMsg));

        // 2. Perception: Ores
        List<BlockPos> ores = BlockScanner.findTargetOres(ctx, 10);
        String oreMsg = "Found " + ores.size() + " target ores within 10 blocks.";
        DiagnosticManager.report("PERCEPTION", oreMsg);
        client.player.sendSystemMessage(Component.literal("§b[2/5 Perception] §f" + oreMsg));

        if (!ores.isEmpty()) {
            BlockPos targetOre = ores.get(0);
            ctx.setCurrentTargetBlock(targetOre);

            // 3. Pathfinder
            BlockPos playerPos = client.player.blockPosition();
            List<BlockPos> path = AStarPathfinder.findPath(ctx, playerPos, targetOre, 500);
            String pathMsg = "Path to (" + targetOre.toShortString() + "): " + path.size() + " nodes generated.";
            DiagnosticManager.report("PATHFINDING", pathMsg);
            client.player.sendSystemMessage(Component.literal("§b[3/5 Pathfinder] §f" + pathMsg));

            // 4. Control
            Vec3 targetVec = targetOre.getCenter();
            RotationController.lookAt(ctx, targetVec);
            DiagnosticManager.debug("ROTATION", "Target vector: " + targetVec);
            client.player.sendSystemMessage(Component.literal("§b[4/5 Control] §fRotating toward target ore..."));

            // 5. Verification
            float currentYaw = client.player.getYRot();
            float currentPitch = client.player.getXRot();
            String rotMsg = "Rotations set -> Yaw: " + (int)currentYaw + " Pitch: " + (int)currentPitch;
            DiagnosticManager.report("ROTATION", rotMsg);
            client.player.sendSystemMessage(Component.literal("§b[5/5 Verification] §f" + rotMsg));
        } else {
            String warning = "No ores found nearby to target.";
            DiagnosticManager.warn(warning);
            client.player.sendSystemMessage(Component.literal("§c[Diagnostic] Place any Wool, Terracotta, Quartz, or Prismarine block nearby to test aim & pathfinding!"));
        }

        DiagnosticManager.event("Diagnostic run complete");
        finished = true;
    }

    @Override
    public void onTick(BotContext ctx) {}

    @Override
    public boolean isFinished(BotContext ctx) { return finished; }

    @Override
    public void onFailure(BotContext ctx, String reason) {
        DiagnosticManager.error("Diagnostic failed: " + reason);
    }

    @Override
    public int getPriority() { return 100; }

    @Override
    public String getName() { return "DiagnosticTestTask"; }
}
