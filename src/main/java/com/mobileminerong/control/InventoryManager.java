package com.mobileminerong.control;

import net.minecraft.client.Minecraft;

public class InventoryManager {

    public static void selectHotbarSlot(int slot) {
        if (slot < 0 || slot > 8) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.getInventory().selected = slot;
        }
    }
}
