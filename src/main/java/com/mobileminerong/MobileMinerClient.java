package com.mobileminerong;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.planning.task.DiagnosticTestTask;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.Minecraft;

import org.lwjgl.glfw.GLFW;

public class MobileMinerClient implements ClientModInitializer {

    public static final BotContext BOT_CONTEXT = new BotContext();

    private boolean diagnosticKeyWasDown = false;


    @Override
    public void onInitializeClient() {

        Minecraft.getInstance();


        net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents.END_CLIENT_TICK.register(client -> {


            if (client.player == null || client.level == null)
                return;


            // Update player snapshot

            BOT_CONTEXT.setPlayerPos(
                    client.player.position()
            );


            BOT_CONTEXT.setRotations(
                    client.player.getYRot(),
                    client.player.getXRot()
            );


            boolean keyDown =
                    org.lwjgl.glfw.GLFW.glfwGetKey(
                            client.getWindow().getWindow(),
                            GLFW.GLFW_KEY_O
                    )
                    == GLFW.GLFW_PRESS;



            // Detect press, not hold

            if (keyDown && !diagnosticKeyWasDown) {

                new DiagnosticTestTask()
                        .onStart(BOT_CONTEXT);

            }


            diagnosticKeyWasDown = keyDown;


        });
    }
}
