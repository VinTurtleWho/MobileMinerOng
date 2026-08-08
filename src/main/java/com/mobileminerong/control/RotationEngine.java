package com.mobileminerong.control;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import com.mobileminerong.util.OrnsteinUhlenbeckDrift;
import com.mobileminerong.debug.DebugLogger;

public class RotationEngine {

    private float startYaw, startPitch, targetYaw, targetPitch;
    private float internalYaw, internalPitch;
    private long startTimeNanos;
    private long lastFrameTimeNanos;
    private double durationNanos;
    
    // Mean-reverting drift
    private double driftYaw = 0.0, driftPitch = 0.0;
    private final OrnsteinUhlenbeckDrift drift = new OrnsteinUhlenbeckDrift();
    
    private boolean active = false;
    private double cachedGcd = 0.15;

    public RotationEngine() {}

    public synchronized void abort() {
        this.active = false;
        this.driftYaw = 0.0;
        this.driftPitch = 0.0;
    }

    public synchronized boolean isActive() { return active; }

    public synchronized void startRotation(float currentYaw, float currentPitch, Vec3 targetPos) {
        this.startYaw = Mth.wrapDegrees(currentYaw);
        this.startPitch = Mth.wrapDegrees(currentPitch);
        this.internalYaw = this.startYaw;
        this.internalPitch = this.startPitch;
        
        applyTarget(targetPos);
        
        // Initial trajectory duration
        this.durationNanos = calculateDuration(startYaw, startPitch, targetYaw, targetPitch);
        
        this.startTimeNanos = System.nanoTime();
        this.lastFrameTimeNanos = this.startTimeNanos;
        this.active = true;
    }

    public synchronized void updateTarget(Vec3 targetPos) {
        if (!active) return;
        
        // Calculate new target
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        Vec3 eyesPos = client.player.getEyePosition();
        double dx = targetPos.x - eyesPos.x;
        double dy = targetPos.y - eyesPos.y;
        double dz = targetPos.z - eyesPos.z;
        
        float nextYaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f);
        float nextPitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx*dx + dz*dz))), -90.0f, 90.0f);

        // Chain new micro-polynomial (40-80ms jitter)
        this.startYaw = internalYaw;
        this.startPitch = internalPitch;
        this.targetYaw = nextYaw;
        this.targetPitch = nextPitch;
        this.durationNanos = (40.0 + (Math.random() * 40.0)) * 1_000_000.0;
        this.startTimeNanos = System.nanoTime();
    }

    private void applyTarget(Vec3 targetPos) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        Vec3 eyesPos = client.player.getEyePosition();
        double dx = targetPos.x - eyesPos.x;
        double dy = targetPos.y - eyesPos.y;
        double dz = targetPos.z - eyesPos.z;
        
        this.targetYaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f);
        this.targetPitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx*dx + dz*dz))), -90.0f, 90.0f);
    }
    
    private double calculateDuration(float y1, float p1, float y2, float p2) {
        double amplitude = Math.sqrt(Math.pow(Mth.wrapDegrees(y2 - y1), 2) + Math.pow(p2 - p1, 2));
        double indexOfDifficulty = Math.log(1.0 + (amplitude / 5.0)) / Math.log(2.0);
        double MT_ticks = 1.0 + (1.8 * indexOfDifficulty);
        return MT_ticks * 50_000_000.0;
    }

    public synchronized int[] computeNextFrameSteps() {
        if (!active) return new int[]{0, 0};
        
        drift.update();
        driftYaw = drift.getX() * 0.2; // Very subtle drift
        driftPitch = drift.getY() * 0.2;

        long now = System.nanoTime();

        float prevInternalYaw = internalYaw;
        float prevInternalPitch = internalPitch;

        double tau = Math.min(1.0, (double) (now - startTimeNanos) / durationNanos);

        double smoothTau = tau * tau * tau * (10.0 - 15.0 * tau + 6.0 * tau * tau);
        internalYaw = (float) (startYaw + (Mth.wrapDegrees((targetYaw + driftYaw) - startYaw) * smoothTau));
        internalPitch = (float) (startPitch + (((targetPitch + driftPitch) - startPitch) * smoothTau));

        if (tau >= 1.0) {
            // Reached target
            this.active = false;
        }

        double desiredYawDelta = (internalYaw - prevInternalYaw);
        double desiredPitchDelta = (internalPitch - prevInternalPitch);
        
        updateGcd();
        int mouseStepX = (int) Math.round(desiredYawDelta / cachedGcd);
        int mouseStepY = (int) Math.round(desiredPitchDelta / cachedGcd);

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
