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

    public RotationController(float initialYaw, float initialPitch) {
        this.targetYaw = initialYaw;
        this.targetPitch = initialPitch;
        this.startYaw = initialYaw;
        this.startPitch = initialPitch;
    }

    public RotationController() {
        this(0, 0);
    }

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

        // Shortest-path angular lerping
        float deltaYaw = Mth.wrapDegrees(targetYaw - startYaw);
        float deltaPitch = Mth.wrapDegrees(targetPitch - startPitch);
        float yaw = startYaw + smoothProgress * deltaYaw;
        float pitch = startPitch + smoothProgress * deltaPitch;

        // Add micro-jitter using Gaussian distribution (center 0, std 0.07)
        float jitterYaw = (float)(jitterRandom.nextGaussian() * 0.07);
        float jitterPitch = (float)(jitterRandom.nextGaussian() * 0.07);

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

    public float getTargetYaw() {
        return this.targetYaw;
    }

    public boolean isWithinThreshold(float thresholdDegrees) {
        float yawDiff = Math.abs(Mth.wrapDegrees(targetYaw - startYaw));
        // Need to know current rotation to calculate error accurately.
        // Simplified: check if target is close enough.
        // Requires current rotation state from Minecraft.
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return true;
        
        float currentYaw = Mth.wrapDegrees(client.player.getYRot());
        float currentPitch = Mth.wrapDegrees(client.player.getXRot());
        
        return Math.abs(Mth.wrapDegrees(targetYaw - currentYaw)) < thresholdDegrees &&
               Math.abs(Mth.wrapDegrees(targetPitch - currentPitch)) < thresholdDegrees;
    }
}
