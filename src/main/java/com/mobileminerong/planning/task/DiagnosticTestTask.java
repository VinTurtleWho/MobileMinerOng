package com.mobileminerong.planning.task;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.control.RotationController;
import com.mobileminerong.debug.DebugLogger;
import com.mobileminerong.perception.BlockScanner;
import com.mobileminerong.perception.ScoreboardParser;
import com.mobileminerong.planning.pathfinding.AStarPathfinder;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.List;

public class DiagnosticTestTask implements BotTask {
    private boolean finished = false;

    @Override
    public void onStart(BotContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        DebugLogger.info("DIAGNOSTIC", "--- STARTING DIAGNOSTIC RUN ---");
        client.player.sendMessage(Text.of("§a[MobileMinerOng] --- RUNNING DIAGNOSTIC TEST ---"), false);

        // 1. Perception: Zone
        ScoreboardParser.updateZone(ctx);
        String zoneMsg = "Current Zone: " + ctx.getCurrentZone();
        DebugLogger.info("DIAGNOSTIC", zoneMsg);
        client.player.sendMessage(Text.of("§b[1/5 Perception] §f" + zoneMsg), false);

        // 2. Perception: Ores
        List<BlockPos> ores = BlockScanner.findTargetOres(ctx, 10);
        String oreMsg = "Found " + ores.size() + " target ores within 10 blocks.";
        DebugLogger.info("DIAGNOSTIC", oreMsg);
        client.player.sendMessage(Text.of("§b[2/5 Perception] §f" + oreMsg), false);

        if (!ores.isEmpty()) {
            BlockPos targetOre = ores.get(0);
            ctx.setCurrentTargetBlock(targetOre);

            // 3. Pathfinder
            BlockPos playerPos = client.player.getBlockPos();
            List<BlockPos> path = AStarPathfinder.findPath(ctx, playerPos, targetOre, 500);
            String pathMsg = "Path to (" + targetOre.toShortString() + "): " + path.size() + " nodes generated.";
            DebugLogger.info("DIAGNOSTIC", pathMsg);
            client.player.sendMessage(Text.of("§b[3/5 Pathfinder] §f" + pathMsg), false);

            // 4. Control
            Vec3d targetVec = targetOre.toCenterPos();
            RotationController.lookAt(ctx, targetVec);
            DebugLogger.info("DIAGNOSTIC", "Rotating toward vector: " + targetVec);
            client.player.sendMessage(Text.of("§b[4/5 Control] §fRotating toward target ore..."), false);

            // 5. Verification
            float currentYaw = client.player.getYaw();
            float currentPitch = client.player.getPitch();
            String rotMsg = "Rotations set -> Yaw: " + (int)currentYaw + " Pitch: " + (int)currentPitch;
            DebugLogger.info("DIAGNOSTIC", rotMsg);
            client.player.sendMessage(Text.of("§b[5/5 Verification] §f" + rotMsg), false);
        } else {
            String warning = "No ores found nearby to target.";
            DebugLogger.warn("DIAGNOSTIC", warning);
            client.player.sendMessage(Text.of("§c[Diagnostic] Place any Wool, Terracotta, Quartz, or Prismarine block nearby to test aim & pathfinding!"), false);
        }

        DebugLogger.info("DIAGNOSTIC", "--- DIAGNOSTIC RUN COMPLETE ---");
        finished = true;
    }

    @Override
    public void onTick(BotContext ctx) {}

    @Override
    public boolean isFinished(BotContext ctx) { return finished; }

    @Override
    public void onFailure(BotContext ctx, String reason) {
        DebugLogger.error("DIAGNOSTIC", "Diagnostic failed: " + reason);
    }

    @Override
    public int getPriority() { return 100; }

    @Override
    public String getName() { return "DiagnosticTestTask"; }
}
