package com.mobileminerong.control;

import net.minecraft.client.Minecraft;
import java.util.Random;

public class ClickGenerator {
    private static final Random random = new Random();
    private static long clickDownTime = 0;
    private static boolean isHolding = false;

    // Call this in the client tick loop to manage click release
    public static void tick() {
        Minecraft client = Minecraft.getInstance();
        if (isHolding && System.currentTimeMillis() > clickDownTime + (20 + random.nextInt(20))) {
            forceRelease(client);
        }
    }

    public static void forceRelease(Minecraft client) {
        if (isHolding) {
            client.execute(() -> {
                client.options.keyAttack.setDown(false);
                com.mobileminerong.debug.DebugLogger.debug("CLICK", "Released attack key");
            });
            isHolding = false;
        }
    }

    public static void performClick() {
        Minecraft client = Minecraft.getInstance();
        client.execute(() -> {
            client.options.keyAttack.setDown(true);
            com.mobileminerong.debug.DebugLogger.debug("CLICK", "Pressed attack key - IS_DOWN: " + client.options.keyAttack.isDown());
        });
        clickDownTime = System.currentTimeMillis();
        isHolding = true;
    }

    public static long calculateInterval(int bonusAttackSpeed, boolean isShortbow) {
        int baseDelay = isShortbow ? 200 : (400 - (int)(bonusAttackSpeed * 2));
        return Math.max(100, baseDelay + random.nextInt(50));
    }
}
