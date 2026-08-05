package com.mobileminerong.control;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class RotationEngine {

    // Internal State
    private double yawResidue = 0.0;
    private double pitchResidue = 0.0;
    private double cachedGcd = 0.15;
    private double lastSensitivity = -1.0;
    
    // State maintained per target/task
    private float lastYaw = 0.0f;
    private float lastPitch = 0.0f;
    private double lastVelYaw = 0.0;
    private double lastVelPitch = 0.0;
    private long lastTimeNano = 0;
    private boolean initialized = false;
    private boolean active = false;

    public RotationEngine() {}

    public synchronized void abort() {
        this.active = false;
        this.yawResidue = 0.0;
        this.pitchResidue = 0.0;
        this.initialized = false;
    }

    public synchronized void setActive(boolean active) { this.active = active; }
    public synchronized boolean isActive() { return active; }

    public synchronized int[] computeNextFrameSteps(float currentYaw, float currentPitch, Vec3 targetPos) {
        if (!active) return new int[]{0, 0};

        long now = System.nanoTime();
        if (!initialized) {
            lastYaw = currentYaw;
            lastPitch = currentPitch;
            lastTimeNano = now;
            initialized = true;
            return new int[]{0, 0};
        }

        double dt = (now - lastTimeNano) / 1_000_000_000.0;
        if (dt <= 0.0001) dt = 0.0001;

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return new int[]{0, 0};

        Vec3 eyesPos = client.player.getEyePosition();
        double dx = targetPos.x - eyesPos.x;
        double dy = targetPos.y - eyesPos.y;
        double dz = targetPos.z - eyesPos.z;
        float targetYaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f);
        float targetPitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx*dx + dz*dz))), -90.0f, 90.0f);

        // PD Control
        double currentVelYaw = Mth.wrapDegrees(currentYaw - lastYaw) / dt;
        double currentVelPitch = (currentPitch - lastPitch) / dt;

        double yawError = Mth.wrapDegrees(targetYaw - currentYaw);
        double pitchError = targetPitch - currentPitch;

        double targetVelYaw = (yawError * 0.15) - (currentVelYaw * 0.05);
        double targetVelPitch = (pitchError * 0.15) - (currentVelPitch * 0.05);

        // Accel Limiting
        double yawAccel = (targetVelYaw - lastVelYaw) / dt;
        double pitchAccel = (targetVelPitch - lastVelPitch) / dt;

        double yawAccelClamped = Mth.clamp(yawAccel, -500.0, 500.0);
        double pitchAccelClamped = Mth.clamp(pitchAccel, -500.0, 500.0);

        double finalVelYaw = lastVelYaw + (yawAccelClamped * dt);
        double finalVelPitch = lastVelPitch + (pitchAccelClamped * dt);

        lastYaw = currentYaw;
        lastPitch = currentPitch;
        lastVelYaw = finalVelYaw;
        lastVelPitch = finalVelPitch;
        lastTimeNano = now;

        updateGcd();
        double desiredYawDelta = (finalVelYaw * dt) + yawResidue;
        int mouseStepX = (int) Math.round(desiredYawDelta / cachedGcd);
        yawResidue = desiredYawDelta - (mouseStepX * cachedGcd);

        double desiredPitchDelta = (finalVelPitch * dt) + pitchResidue;
        int mouseStepY = (int) Math.round(desiredPitchDelta / cachedGcd);
        pitchResidue = desiredPitchDelta - (mouseStepY * cachedGcd);

        return new int[]{mouseStepX, mouseStepY};
    }

    private void updateGcd() {
        double sens = Minecraft.getInstance().options.sensitivity().get();
        if (sens != lastSensitivity) {
            double f = sens * 0.6 + 0.2;
            cachedGcd = f * f * f * 8.0 * 0.15;
            lastSensitivity = sens;
        }
    }
}
