package com.mobileminerong.control;

import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mobileminerong.util.ExGaussianGenerator;

public class ClickGenerator {
    private static long clickDownTime = 0;
    private static boolean isHolding = false;

    // Call this in the client tick loop to manage click release
    public static void tick() {
        if (isHolding && System.currentTimeMillis() > clickDownTime + ExGaussianGenerator.nextDelay(20, 5, 10)) {
            forceRelease();
        }
    }

    public static void forceRelease() {
        Minecraft client = Minecraft.getInstance();
        if (isHolding) {
            client.execute(() -> {
                client.options.keyAttack.setDown(false);
            });
            isHolding = false;
        }
    }

    // Pure cadence trigger: directly increment clickCount.
    // Minecraft handles the rest (raytrace, animation, packets).
    public static void performClick() {
        Minecraft client = Minecraft.getInstance();
        if (client.options == null) return;
        
        KeyMapping attackKey = client.options.keyAttack;
        com.mobileminerong.mixin.KeyMappingAccessor accessor = (com.mobileminerong.mixin.KeyMappingAccessor) attackKey;
        
        // Increment the native click counter
        accessor.setClickCount(accessor.getClickCount() + 1);
        
        // Also set down for safety to ensure it registers in the tick
        client.execute(() -> attackKey.setDown(true));
        
        clickDownTime = System.currentTimeMillis();
        isHolding = true;
        
        com.mobileminerong.debug.DebugLogger.info("CLICK", "Dispatched native attack key event using clickCount injection");
    }

    public static long calculateInterval(int bonusAttackSpeed, boolean isShortbow) {
        // BAS caps at 5 CPS (200ms) or 100 BAS.
        int baseDelay = isShortbow ? 200 : (400 - (int)(bonusAttackSpeed * 2));
        return Math.max(100, ExGaussianGenerator.nextDelay(baseDelay, 15, 20));
    }
}
