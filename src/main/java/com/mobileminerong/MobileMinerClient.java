package com.mobileminerong;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.planning.task.DiagnosticTestTask;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;


public class MobileMinerClient implements ClientModInitializer {

    public static final BotContext BOT_CONTEXT = new BotContext();

    private int tickCounter = 0;


    @Override
    public void onInitializeClient() {

        Minecraft.getInstance();


        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {

            if (client.player == null || client.level == null) {
                return;
            }


            BOT_CONTEXT.setPlayerPos(
                    client.player.position()
            );


            BOT_CONTEXT.setRotations(
                    client.player.getYRot(),
                    client.player.getXRot()
            );


            tickCounter++;


            // Temporary diagnostic test every 200 ticks (10 seconds)
            if (tickCounter >= 200) {

                tickCounter = 0;

                new DiagnosticTestTask()
                        .onStart(BOT_CONTEXT);

            }

        });
    }
}
