package com.mobileminerong;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.planning.task.DiagnosticTestTask;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;

import org.lwjgl.glfw.GLFW;


public class MobileMinerClient implements ClientModInitializer {

    public static final BotContext BOT_CONTEXT = new BotContext();

    private static KeyMapping diagnosticKey;


    @Override
    public void onInitializeClient() {

        diagnosticKey = KeyBindingHelper.registerKeyBinding(
                new KeyMapping(
                        "key.mobileminerong.diagnostic",
                        GLFW.GLFW_KEY_O,
                        "category.mobileminerong"
                )
        );


        ClientTickEvents.END_CLIENT_TICK.register(client -> {

            if (client.player == null || client.level == null) {
                return;
            }


            BOT_CONTEXT.setPlayerPos(client.player.position());

            BOT_CONTEXT.setRotations(
                    client.player.getYRot(),
                    client.player.getXRot()
            );


            while (diagnosticKey.consumeClick()) {

                new DiagnosticTestTask()
                        .onStart(BOT_CONTEXT);

            }

        });
    }
}
