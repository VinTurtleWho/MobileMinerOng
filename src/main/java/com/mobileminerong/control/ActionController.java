package com.mobileminerong.control;

import com.mobileminerong.context.BotContext;
import com.mobileminerong.mixin.accessor.InventoryAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;

public class ActionController {

    public static void setKey(KeyMapping key, boolean pressed) {
        key.setDown(pressed);
    }

    public static void selectHotbarSlot(BotContext ctx, int slot) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        if (slot >= 0 && slot < 9) {
            ((InventoryAccessor) client.player.getInventory()).setSelectedSlot(slot);
            ctx.setLastAction("Selected Hotbar Slot: " + slot);
        }
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
