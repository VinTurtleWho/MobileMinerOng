package com.mobileminerong;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.debug.DebugLogger;
import com.mobileminerong.planning.task.DiagnosticTestTask;
import com.mobileminerong.state.PriorityTaskEngine;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;

public class MobileMinerClient implements ClientModInitializer {
    private static final BotContext BOT_CONTEXT = new BotContext();
    private static final PriorityTaskEngine TASK_ENGINE = new PriorityTaskEngine();
    private static KeyBinding diagnosticKey;

    @Override
    public void onInitializeClient() {
        DebugLogger.init();
        DebugLogger.info("CLIENT", "Initializing MobileMinerOng Framework Architecture...");

        // Keybind 'G' for diagnostic test
        diagnosticKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.mobileminerong.test",
            InputUtil.Type.KEYSYM,
            GLFW.GLFW_KEY_G,
            "category.mobileminerong"
        ));

        // Lifecycle Hook: Safely close log file on client shutdown
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            DebugLogger.info("CLIENT", "Client stopping, closing DebugLogger...");
            DebugLogger.close();
        });

        // Tick loop hook
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            // Sync live context
            BOT_CONTEXT.setPlayerPos(client.player.getPos());
            BOT_CONTEXT.setRotations(client.player.getYaw(), client.player.getPitch());

            // Handle Keypress
            while (diagnosticKey.wasPressed()) {
                DebugLogger.info("KEYBIND", "Key 'G' pressed. Registering DiagnosticTestTask...");
                TASK_ENGINE.registerTask(new DiagnosticTestTask());
            }

            // Tick task engine
            TASK_ENGINE.tick(BOT_CONTEXT);
        });
    }

    public static BotContext getContext() { return BOT_CONTEXT; }
    public static PriorityTaskEngine getTaskEngine() { return TASK_ENGINE; }
}
