package com.mobileminerong;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.debug.DebugLogger;
import com.mobileminerong.planning.task.DiagnosticTestTask;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;


public class MobileMinerClient implements ClientModInitializer {


    public static final BotContext BOT_CONTEXT = new BotContext();


    private int tickCounter = 0;
    private boolean diagnosticRunning = false;


    @Override
    public void onInitializeClient() {


        DebugLogger.init();


        ClientTickEvents.END_CLIENT_TICK.register(client -> {


            if (client.player == null || client.level == null) {
                return;
            }


            updateContext(client);


            tickCounter++;


            // Temporary automatic diagnostic
            // Runs every 5 seconds
            if (tickCounter >= 100 && !diagnosticRunning) {


                tickCounter = 0;

                diagnosticRunning = true;


                DebugLogger.info(
                    "SYSTEM",
                    "Starting automatic diagnostic test"
                );


                new DiagnosticTestTask()
                    .onStart(BOT_CONTEXT);


                diagnosticRunning = false;

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


        BOT_CONTEXT.setLastAction(
            "Context updated"
        );

    }

}
