package com.mobileminerong.control;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class RotationEngine {

    // State maintained per target/task
    private float startYaw;
    private float startPitch;
    private float targetYaw;
    private float targetPitch;
    private int totalTicks;
    private int currentTick = 0;
    private boolean active = false;
    private double yawResidue = 0.0;
    private double pitchResidue = 0.0;
    private double cachedGcd = 0.15;

    public RotationEngine() {}

    public synchronized void abort() {
        this.active = false;
        this.yawResidue = 0.0;
        this.pitchResidue = 0.0;
    }

    public synchronized void setActive(boolean active) { this.active = active; }
    public synchronized boolean isActive() { return active; }

    public synchronized void startRotation(float currentYaw, float currentPitch, Vec3 targetPos, int durationTicks) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        Vec3 eyesPos = client.player.getEyePosition();
        double dx = targetPos.x - eyesPos.x;
        double dy = targetPos.y - eyesPos.y;
        double dz = targetPos.z - eyesPos.z;

        this.startYaw = Mth.wrapDegrees(currentYaw);
        this.startPitch = Mth.wrapDegrees(currentPitch);
        this.targetYaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f);
        this.targetPitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx*dx + dz*dz))), -90.0f, 90.0f);
        
        this.totalTicks = Math.max(1, durationTicks);
        this.currentTick = 0;
        this.active = true;
    }

    public synchronized int[] computeNextFrameSteps() {
        if (!active || currentTick >= totalTicks) {
            active = false;
            return new int[]{0, 0};
        }

        currentTick++;
        double tau = (double) currentTick / totalTicks;
        // 5th-order polynomial: 10τ³ - 15τ⁴ + 6τ⁵
        double smoothTau = tau * tau * tau * (10.0 - 15.0 * tau + 6.0 * tau * tau);

        float currentYaw = (float) (startYaw + (Mth.wrapDegrees(targetYaw - startYaw) * smoothTau));
        float currentPitch = (float) (startPitch + (Mth.wrapDegrees(targetPitch - startPitch) * smoothTau));

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return new int[]{0, 0};

        updateGcd();
        
        // Convert to mouse deltas
        double prevTau = (double) (currentTick - 1) / totalTicks;
        double smoothPrevTau = (currentTick == 1) ? 0.0 : prevTau * prevTau * prevTau * (10.0 - 15.0 * prevTau + 6.0 * prevTau * prevTau);
        
        float prevYaw = (float) (startYaw + (Mth.wrapDegrees(targetYaw - startYaw) * smoothPrevTau));
        float prevPitch = (float) (startPitch + (Mth.wrapDegrees(targetPitch - startPitch) * smoothPrevTau));

        double desiredYawDelta = Mth.wrapDegrees(currentYaw - prevYaw) + yawResidue;
        int mouseStepX = (int) Math.round(desiredYawDelta / cachedGcd);
        yawResidue = desiredYawDelta - (mouseStepX * cachedGcd);

        double desiredPitchDelta = (currentPitch - prevPitch) + pitchResidue;
        int mouseStepY = (int) Math.round(desiredPitchDelta / cachedGcd);
        pitchResidue = desiredPitchDelta - (mouseStepY * cachedGcd);

        return new int[]{mouseStepX, mouseStepY};
    }

    private void updateGcd() {
        double sens = Minecraft.getInstance().options.sensitivity().get();
        if (sens != 0.0) {
            double f = sens * 0.6 + 0.2;
            cachedGcd = f * f * f * 8.0 * 0.15;
        }
    }
}
