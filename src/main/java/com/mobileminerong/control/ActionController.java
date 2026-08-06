package com.mobileminerong.control;

import com.mobileminerong.context.BotContext;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;

public class ActionController {

    public static void setKey(KeyMapping key, boolean pressed) {
        key.setDown(pressed);
    }

    public static void selectHotbarSlot(BotContext ctx, int slot) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (slot >= 0 && slot < 9) {
            client.player.getInventory().selected = slot;
            ctx.setLastAction("Selected Hotbar Slot: " + slot);
        }
    }

    public static void startAttack() {
        Minecraft client = Minecraft.getInstance();
        if (client.options != null) {
            client.options.keyAttack.setDown(true);
        }
    }

    public static void stopAttack() {
        Minecraft client = Minecraft.getInstance();
        if (client.options != null) {
            client.options.keyAttack.setDown(false);
        }
    }

    public static void startMining(BlockPos pos) {
        startAttack();
    }

    public static void stopMining() {
        stopAttack();
    }

    public static void stopAllInputs() {
        Minecraft client = Minecraft.getInstance();
        if (client == null) return;
        client.options.keyUp.setDown(false);
        client.options.keyDown.setDown(false);
        client.options.keyLeft.setDown(false);
        client.options.keyRight.setDown(false);
        client.options.keyJump.setDown(false);
        client.options.keyShift.setDown(false);
        client.options.keyAttack.setDown(false);
        client.options.keyUse.setDown(false);
    }
}
