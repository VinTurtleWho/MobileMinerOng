package com.mobileminerong;

import com.mobileminerong.command.MacroCommandHandler;
import com.mobileminerong.context.BotContext;
import com.mobileminerong.debug.DebugLogger;
import com.mobileminerong.perception.BlockScanner;
import com.mobileminerong.perception.ScoreboardParser;
import com.mobileminerong.planning.task.DiagnosticTestTask;
import com.mobileminerong.planning.task.ShadowBotTask;
import com.mobileminerong.planning.task.TargetSearchTask;
import com.mobileminerong.state.PriorityTaskEngine;
import com.mobileminerong.util.ChatLogger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.stream.Collectors;

public class MobileMinerClient implements ClientModInitializer {

    public static final BotContext BOT_CONTEXT = new BotContext();
    public static final PriorityTaskEngine TASK_ENGINE = new PriorityTaskEngine();
    
    public static final KeyMapping TOGGLE_SHADOW_KEY = new KeyMapping(
        "key.mobileminerong.toggle_shadow",
        GLFW.GLFW_KEY_O,
        "category.mobileminerong.general"
    );

    private static int debugTimer = 0;
    private static int perceptionTimer = 0;

    @Override
    public void onInitializeClient() {

        DebugLogger.init();
        KeyBindingHelper.registerKeyBinding(TOGGLE_SHADOW_KEY);
        TASK_ENGINE.registerTask(new DiagnosticTestTask());
        TASK_ENGINE.registerTask(new TargetSearchTask());
        TASK_ENGINE.registerTask(new ShadowBotTask());

        DebugLogger.info("SYSTEM", "MobileMinerOng initialized");



        ClientSendMessageEvents.ALLOW_CHAT.register(message -> {

            ChatLogger.log("[SENT] " + message);
            if(message.startsWith("!macro")) {

                return !MacroCommandHandler.handle(message, BOT_CONTEXT);

            }

            return true;
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            ChatLogger.log("[RECEIVED] " + message.getString());
        });


        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            if(client.player == null || client.level == null)
                return;

            if (TOGGLE_SHADOW_KEY.consumeClick()) {
                if (BOT_CONTEXT.getTargetPlayer() != null || BOT_CONTEXT.isPendingPlayerSearch()) {
                    BOT_CONTEXT.setTargetPlayer(null);
                    BOT_CONTEXT.setPendingPlayerSearch(false);
                    client.player.sendSystemMessage(Component.literal("§c[MobileMinerOng] Player targeting OFF"));
                } else {
                    BOT_CONTEXT.setPendingPlayerSearch(true);
                    client.player.sendSystemMessage(Component.literal("§a[MobileMinerOng] Searching for nearest player..."));
                }
            }

            // Deferred player search
            if (BOT_CONTEXT.isPendingPlayerSearch()) {
                net.minecraft.world.entity.player.Player nearest = null;
                double minDist = Double.MAX_VALUE;
                for (net.minecraft.world.entity.player.Player p : client.level.players()) {
                    if (p == null || p == client.player || !p.isAlive()) continue;
                    double dist = p.distanceToSqr(client.player);
                    if (dist < minDist) {
                        minDist = dist;
                        nearest = p;
                    }
                }
                if (nearest != null) {
                    BOT_CONTEXT.setTargetPlayer(nearest);
                    client.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§a[MobileMinerOng] Targeting: " + nearest.getName().getString()));
                } else {
                    client.player.sendSystemMessage(net.minecraft.network.chat.Component.literal("§c[MobileMinerOng] No players found"));
                }
                BOT_CONTEXT.setPendingPlayerSearch(false);
            }

            updateContext(client);
            TASK_ENGINE.tick(BOT_CONTEXT);



            perceptionTimer++;

            // Run perception once per second
            if(perceptionTimer >= 20) {

                perceptionTimer = 0;

                updatePerception();

            }


            if(MacroCommandHandler.isDebugEnabled()) {

                debugTimer++;

                // 20 ticks = 1 second
                if(debugTimer >= 20) {

                    debugTimer = 0;

                    DebugLogger.info(
                        "DEBUG",
                        buildDebugReport()
                    );

                }

            }

        });

    }


    private void updateContext(Minecraft client) {

        BOT_CONTEXT.setPlayerPos(
            client.player.position()
        );


        BOT_CONTEXT.setRotations(
            client.player.getYRot(),
            client.player.getXRot()
        );

    }



    private void updatePerception() {

        try {

            ScoreboardParser.updateZone(
                BOT_CONTEXT
            );


            List<BlockPos> targets =
                BlockScanner.findTargetOres(
                    BOT_CONTEXT,
                    10
                );


            if(!targets.isEmpty()) {

                BOT_CONTEXT.setCurrentTargetBlock(
                    targets.get(0)
                );
                BOT_CONTEXT.updatePerceptionHealth(true);

                DebugLogger.debug(
                    "PERCEPTION",
                    "Found target: " + targets.get(0)
                );


            } else {
                BOT_CONTEXT.updatePerceptionHealth(true);
            }

        } catch(Exception e) {
            BOT_CONTEXT.updatePerceptionHealth(false);
            DebugLogger.error(
                "PERCEPTION",
                e.toString()
            );

        }

    }



    private String buildDebugReport(){
        String stateHistory = BOT_CONTEXT.getStateHistory().stream()
            .map(e -> String.format("...->%s (%s)", e.state(), e.reason()))
            .collect(Collectors.joining("\n"));

        return
            "\n===== MOBILEMINER DIAGNOSTIC =====" +
            "\nBOT" +
            "\n- State: " + BOT_CONTEXT.getCurrentState() + " (" + BOT_CONTEXT.getStateChangeReason() + ")" +
            "\n- Active Task: " + (TASK_ENGINE.getActiveTask() != null ? TASK_ENGINE.getActiveTask().getName() : "None") +
            "\nTASK" +
            "\n- Pool Size: " + TASK_ENGINE.getTaskPoolSize() +
            "\n- Last Completed: " + (TASK_ENGINE.getLastCompletedTask() != null ? TASK_ENGINE.getLastCompletedTask().getName() : "None") +
            "\n- Last Failed: " + (TASK_ENGINE.getLastFailedTask() != null ? TASK_ENGINE.getLastFailedTask().getName() + " (" + TASK_ENGINE.getLastFailureReason() + ")" : "None") +
            "\nPLAYER" +
            "\n- Pos: " + BOT_CONTEXT.getPlayerPos() +
            "\nPERCEPTION" +
            "\n- Target: " + BOT_CONTEXT.getCurrentTargetBlock() +
            "\n- Failures: " + BOT_CONTEXT.getPerceptionFailures() +
            "\nENGINE" +
            "\n- Last Tick: " + BOT_CONTEXT.getLastTickDurationMicros() + "µs" +
            "\nRECENT STATE TRANSITIONS" +
            "\n" + stateHistory +
            "\n============================";

    }

}
