package com.mobileminerong;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.debug.DebugLogger;
import com.mobileminerong.planning.task.DiagnosticTestTask;
import com.mobileminerong.state.PriorityTaskEngine;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;

public class MobileMinerClient implements ClientModInitializer {
    private static final BotContext BOT_CONTEXT = new BotContext();
    private static final PriorityTaskEngine TASK_ENGINE = new PriorityTaskEngine();

    @Override
    public void onInitializeClient() {
        DebugLogger.init();
        DebugLogger.info("CLIENT", "Initializing MobileMinerOng Framework Architecture...");

        // Keybind 'G' for diagnostic test

        // Lifecycle Hook: Safely close log file on client shutdown
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            DebugLogger.info("CLIENT", "Client stopping, closing DebugLogger...");
            DebugLogger.close();
        });

        // Tick loop hook
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.level == null) return;

            // Sync live context

            // Handle Keypress
            if (com.mojang.blaze3d.platform.InputConstants.isKeyDown(org.lwjgl.glfw.GLFW.glfwGetCurrentContext(), 71)) {
                DebugLogger.info("KEYBIND", "Key 'G' pressed. Registering DiagnosticTestTask...");
            }

            // Tick task engine
            TASK_ENGINE.tick(BOT_CONTEXT);
        });
    }

    public static BotContext getContext() { return BOT_CONTEXT; }
    public static PriorityTaskEngine getTaskEngine() { return TASK_ENGINE; }
}
