package com.mobileminerong.control;

import com.mobileminerong.context.BotContext;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class RotationController {

    private float startYaw;
    private float startPitch;
    private float targetYaw;
    private float targetPitch;
    private float progress = 0.0f; // 0.0 to 1.0
    private int totalTicks;
    private int currentTick = 0;
    private final Random jitterRandom = new Random();

    public void setTarget(Vec3 targetPos, float playerCurrentYaw, float playerCurrentPitch) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        Vec3 eyesPos = client.player.getEyePosition();
        double diffX = targetPos.x - eyesPos.x;
        double diffY = targetPos.y - eyesPos.y;
        double diffZ = targetPos.z - eyesPos.z;

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        
        this.startYaw = Mth.wrapDegrees(playerCurrentYaw);
        this.startPitch = Mth.wrapDegrees(playerCurrentPitch);

        this.targetYaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f);
        this.targetPitch = Mth.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)));

        // Calculate total angular distance for duration (basic scaling)
        float deltaYaw = Math.abs(Mth.wrapDegrees(targetYaw - startYaw));
        float deltaPitch = Math.abs(Mth.wrapDegrees(targetPitch - startPitch));
        float maxDelta = Math.max(deltaYaw, deltaPitch);

        // Assume roughly 22.5 degrees per tick as base speed, so total ticks = maxDelta / 22.5
        this.totalTicks = Math.max(1, (int) (maxDelta / 22.5f));
        this.currentTick = 0;
        this.progress = 0.0f;
    }

    public boolean tick(BotContext ctx) {
        if (currentTick >= totalTicks) return true;

        currentTick++;
        progress = (float) currentTick / totalTicks;

        // Smoothstep: t * t * (3 - 2 * t)
        float smoothProgress = progress * progress * (3.0f - 2.0f * progress);

        float yaw = Mth.lerp(smoothProgress, startYaw, targetYaw);
        float pitch = Mth.lerp(smoothProgress, startPitch, targetPitch);

        // Add micro-jitter: ±0.05° to ±0.25°
        float jitterYaw = (jitterRandom.nextFloat() * 0.2f - 0.1f) + (jitterRandom.nextBoolean() ? 0.15f : -0.15f);
        float jitterPitch = (jitterRandom.nextFloat() * 0.2f - 0.1f) + (jitterRandom.nextBoolean() ? 0.15f : -0.15f);

        Minecraft client = Minecraft.getInstance();
        if (client.player != null) {
            client.player.setYRot(yaw + jitterYaw);
            client.player.setXRot(Mth.clamp(pitch + jitterPitch, -90.0f, 90.0f));
            
            ctx.setRotations(yaw, pitch);
            ctx.setLastAction("Rotating -> Yaw: " + (int)yaw + " Pitch: " + (int)pitch);
        }

        return currentTick >= totalTicks;
    }

    public boolean isAligned() {
        return currentTick >= totalTicks;
    }
}
