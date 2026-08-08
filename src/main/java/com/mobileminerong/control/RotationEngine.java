package com.mobileminerong.control;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import com.mobileminerong.debug.DebugLogger;

public class RotationEngine {

    private float startYaw, startPitch, targetYaw, targetPitch;
    private float internalYaw, internalPitch;
    private long startTimeNanos;
    private long lastFrameTimeNanos;
    private double durationNanos;
    
    private boolean active = false;
    private double yawResidue = 0.0, pitchResidue = 0.0;
    private double cachedGcd = 0.15;

    public RotationEngine() {}

    public synchronized void abort() {
        this.active = false;
        this.yawResidue = 0.0;
        this.pitchResidue = 0.0;
    }

    public synchronized boolean isActive() { return active; }

    public synchronized void startRotation(float currentYaw, float currentPitch, Vec3 targetPos) {
        this.startYaw = Mth.wrapDegrees(currentYaw);
        this.startPitch = Mth.wrapDegrees(currentPitch);
        this.internalYaw = this.startYaw;
        this.internalPitch = this.startPitch;
        
        updateTarget(targetPos);

        double amplitude = Math.sqrt(Math.pow(Mth.wrapDegrees(targetYaw - startYaw), 2) + Math.pow(targetPitch - startPitch, 2));
        // Smoother, consistent duration based on amplitude
        double MT_ticks = 5.0 + (amplitude / 10.0);
        this.durationNanos = MT_ticks * 50_000_000.0;
        
        this.startTimeNanos = System.nanoTime();
        this.lastFrameTimeNanos = this.startTimeNanos;
        this.active = true;
    }

    public synchronized void updateTarget(Vec3 targetPos) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        Vec3 eyesPos = client.player.getEyePosition();
        double dx = targetPos.x - eyesPos.x;
        double dy = targetPos.y - eyesPos.y;
        double dz = targetPos.z - eyesPos.z;
        
        float newTargetYaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f);
        float newTargetPitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx*dx + dz*dz))), -90.0f, 90.0f);
        
        // Hysteresis: Only update if the change is significant (e.g., > 0.5 degrees)
        if (Math.abs(newTargetYaw - targetYaw) > 0.5f || Math.abs(newTargetPitch - targetPitch) > 0.5f) {
            this.targetYaw = newTargetYaw;
            this.targetPitch = newTargetPitch;
        }
    }

    public synchronized int[] computeNextFrameSteps() {
        if (!active) return new int[]{0, 0};
        
        long now = System.nanoTime();
        lastFrameTimeNanos = now;

        float prevInternalYaw = internalYaw;
        float prevInternalPitch = internalPitch;

        double tau = Math.min(1.0, (double) (now - startTimeNanos) / durationNanos);

        // Clean Minimum-Jerk Reaching Phase
        double smoothTau = tau * tau * tau * (10.0 - 15.0 * tau + 6.0 * tau * tau);
        internalYaw = (float) (startYaw + (Mth.wrapDegrees(targetYaw - startYaw) * smoothTau));
        internalPitch = (float) (startPitch + ((targetPitch - startPitch) * smoothTau));

        if (tau >= 1.0) {
            this.active = false;
        }

        // Apply Differential Quantization
        double desiredYawDelta = (internalYaw - prevInternalYaw) + yawResidue;
        double desiredPitchDelta = (internalPitch - prevInternalPitch) + pitchResidue;
        
        updateGcd();
        int mouseStepX = (int) Math.round(desiredYawDelta / cachedGcd);
        yawResidue = desiredYawDelta - (mouseStepX * cachedGcd);

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
