package com.mobileminerong;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.state.PriorityTaskEngine;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

public class MobileMinerClient implements ClientModInitializer {
    private static final BotContext BOT_CONTEXT = new BotContext();
    private static final PriorityTaskEngine TASK_ENGINE = new PriorityTaskEngine();

    @Override
    public void onInitializeClient() {
        System.out.println("[MobileMinerOng] Initializing Master Framework Architecture...");

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.player == null || client.world == null) return;

            BOT_CONTEXT.setPlayerPos(client.player.getPos());
            BOT_CONTEXT.setRotations(client.player.getYaw(), client.player.getPitch());

            TASK_ENGINE.tick(BOT_CONTEXT);
        });
    }

    public static BotContext getContext() { return BOT_CONTEXT; }
    public static PriorityTaskEngine getTaskEngine() { return TASK_ENGINE; }
}
