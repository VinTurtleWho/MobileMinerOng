package com.mobileminerong;

import com.mobileminerong.command.MacroCommandHandler;
import com.mobileminerong.context.BotContext;
import com.mobileminerong.state.MacroMode;
import com.mobileminerong.debug.DebugLogger;
import com.mobileminerong.perception.BlockScanner;
import com.mobileminerong.perception.ScoreboardParser;
import com.mobileminerong.planning.task.DiagnosticTestTask;
import com.mobileminerong.planning.task.TargetSearchTask;
import com.mobileminerong.planning.task.MiningTask;
import com.mobileminerong.planning.task.CombatFollowTask;
import com.mobileminerong.control.ActionController;
import com.mobileminerong.state.PriorityTaskEngine;
import com.mobileminerong.util.ChatLogger;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.stream.Collectors;

public class MobileMinerClient implements ClientModInitializer {

    public static final BotContext BOT_CONTEXT = new BotContext();
    public static final PriorityTaskEngine TASK_ENGINE = new PriorityTaskEngine();
    private static KeyMapping toggleKey;
    private static final KeyMapping.Category CATEGORY = KeyMapping.Category.register(
        Identifier.fromNamespaceAndPath("mobileminerong", "main")
    );
    private static MacroMode lastMode = MacroMode.IDLE;

    private static int debugTimer = 0;
    private static int perceptionTimer = 0;

    @Override
    public void onInitializeClient() {
        DebugLogger.init();
        TASK_ENGINE.registerTask(new DiagnosticTestTask());
        TASK_ENGINE.registerTask(new TargetSearchTask());

        toggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
            "key.mobileminerong.toggle",
            InputConstants.Type.KEYSYM,
            GLFW.GLFW_KEY_O,
            CATEGORY
        ));

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

            while (toggleKey.consumeClick()) {
                if (BOT_CONTEXT.isActive()) {
                    // Deactivate: stop tasks and release inputs
                    BOT_CONTEXT.setActive(false);
                    TASK_ENGINE.clearTasks();
                    ActionController.stopAllInputs();
                    com.mobileminerong.control.ClickGenerator.forceRelease(client);
                    client.player.sendSystemMessage(Component.literal("§c[MobileMinerOng] Macro Stopped"));
                } else {
                    // Activate
                    BOT_CONTEXT.setActive(true);
                    client.player.sendSystemMessage(Component.literal("§a[MobileMinerOng] Macro Started"));
                }
            }

            if (BOT_CONTEXT.isActive()) {
                if (BOT_CONTEXT.getMode() != lastMode) {
                    lastMode = BOT_CONTEXT.getMode();
                    TASK_ENGINE.clearTasks();
                    if (lastMode == MacroMode.MINER) {
                        TASK_ENGINE.registerTask(new TargetSearchTask());
                        TASK_ENGINE.registerTask(new MiningTask());
                    } else if (lastMode == MacroMode.COMBAT) {
                        TASK_ENGINE.registerTask(new CombatFollowTask());
                    }
                    client.player.sendSystemMessage(Component.literal("§e[MobileMinerOng] Tasks updated for " + lastMode));
                }
                
                updateContext(client);
                com.mobileminerong.perception.PerceptionManager.updatePerception(BOT_CONTEXT);
                com.mobileminerong.control.ClickGenerator.tick(); // Added this
                TASK_ENGINE.tick(BOT_CONTEXT);
            }

            perceptionTimer++;
            if(perceptionTimer >= 20) {
                perceptionTimer = 0;
                updatePerception();
            }

            if(MacroCommandHandler.isDebugEnabled()) {
                debugTimer++;
                if(debugTimer >= 20) {
                    debugTimer = 0;
                    DebugLogger.info("DEBUG", buildDebugReport());
                }
            }
        });
    }

    private void updateContext(Minecraft client) {
        BOT_CONTEXT.setPlayerPos(client.player.position());
        BOT_CONTEXT.setRotations(client.player.getYRot(), client.player.getXRot());
    }

    private void updatePerception() {
        try {
            ScoreboardParser.updateZone(BOT_CONTEXT);
            List<BlockPos> targets = BlockScanner.findTargetOres(BOT_CONTEXT, 10);
            if(!targets.isEmpty()) {
                BOT_CONTEXT.setCurrentTargetBlock(targets.get(0));
                BOT_CONTEXT.updatePerceptionHealth(true);
            } else {
                BOT_CONTEXT.updatePerceptionHealth(true);
            }
        } catch(Exception e) {
            BOT_CONTEXT.updatePerceptionHealth(false);
            DebugLogger.error("PERCEPTION", e.toString());
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
