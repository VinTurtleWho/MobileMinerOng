package com.mobileminerong.control;

import com.mobileminerong.context.BotContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;

public class ActionController {

    public static void setKey(KeyBinding key, boolean pressed) {
        key.setPressed(pressed);
    }

    public static void selectHotbarSlot(BotContext ctx, int slot) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;
        if (slot >= 0 && slot < 9) {
            client.player.getInventory().selectedSlot = slot;
            ctx.setLastAction("Selected Hotbar Slot: " + slot);
        }
    }

    public static void stopAllInputs() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) return;
        client.options.forwardKey.setPressed(false);
        client.options.backKey.setPressed(false);
        client.options.leftKey.setPressed(false);
        client.options.rightKey.setPressed(false);
        client.options.jumpKey.setPressed(false);
        client.options.sneakKey.setPressed(false);
        client.options.attackKey.setPressed(false);
        client.options.useKey.setPressed(false);
    }
}
