package com.mobileminerong.control;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import java.util.Random;

public class ClickGenerator {
    private static final Random random = new Random();
    private static long clickDownTime = 0;
    private static boolean isHolding = false;

    // Call this in the client tick loop to manage click release
    public static void tick() {
        if (isHolding && System.currentTimeMillis() > clickDownTime + (20 + random.nextInt(20))) {
            forceRelease();
        }
    }

    public static void forceRelease() {
        Minecraft client = Minecraft.getInstance();
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
        com.mobileminerong.debug.DebugLogger.info("CLICK", "Attempting performClick()");

        client.execute(() -> {
            KeyMapping attackKey = client.options.keyAttack;
            // 1. Set holding state to true
            attackKey.setDown(true);

            // 2. Inject click by incrementing timesPressed using the Accessor
            com.mobileminerong.mixin.KeyMappingAccessor accessor = (com.mobileminerong.mixin.KeyMappingAccessor) attackKey;
            accessor.setTimesPressed(accessor.getTimesPressed() + 1);

            com.mobileminerong.debug.DebugLogger.info("CLICK", "Dispatched native attack key event using timesPressed injection");
        });

        clickDownTime = System.currentTimeMillis();
        isHolding = true;
    }

    public static long calculateInterval(int bonusAttackSpeed, boolean isShortbow) {
        int baseDelay = isShortbow ? 200 : (400 - (int)(bonusAttackSpeed * 2));
        return Math.max(100, baseDelay + random.nextInt(50));
    }
}
