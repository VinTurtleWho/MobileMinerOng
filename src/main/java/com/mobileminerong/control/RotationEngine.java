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
    
    private boolean active = false;
    private double yawResidue = 0.0, pitchResidue = 0.0;
    private double cachedGcd = 0.15;
    
    // Differential Drift Tracking
    private double prevDriftX = 0.0, prevDriftY = 0.0;
    private final OrnsteinUhlenbeckDrift drift = new OrnsteinUhlenbeckDrift();

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
        double MT_ticks = 1.0 + 1.8 * Math.log(1.0 + (amplitude / 5.0)) / Math.log(2.0);
        this.durationNanos = MT_ticks * 50_000_000.0; // 50ms per tick
        
        this.startTimeNanos = System.nanoTime();
        this.lastFrameTimeNanos = this.startTimeNanos;
        this.active = true;
        this.prevDriftX = drift.getX();
        this.prevDriftY = drift.getY();
    }

    public synchronized void updateTarget(Vec3 targetPos) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        Vec3 eyesPos = client.player.getEyePosition();
        double dx = targetPos.x - eyesPos.x;
        double dy = targetPos.y - eyesPos.y;
        double dz = targetPos.z - eyesPos.z;
        this.targetYaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f);
        this.targetPitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx*dx + dz*dz))), -90.0f, 90.0f);
    }

    public synchronized int[] computeNextFrameSteps() {
        if (!active) return new int[]{0, 0};

        long now = System.nanoTime();
        double dt = (now - lastFrameTimeNanos) / 1_000_000_000.0;
        lastFrameTimeNanos = now;

        drift.update();
        double driftX = drift.getX();
        double driftY = drift.getY();

        float prevInternalYaw = internalYaw;
        float prevInternalPitch = internalPitch;

        double tau = Math.min(1.0, (double) (now - startTimeNanos) / durationNanos);

        if (tau < 1.0) {
            // Minimum-Jerk Reaching Phase
            double smoothTau = tau * tau * tau * (10.0 - 15.0 * tau + 6.0 * tau * tau);
            internalYaw = (float) (startYaw + (Mth.wrapDegrees(targetYaw - startYaw) * smoothTau));
            internalPitch = (float) (startPitch + (Mth.wrapDegrees(targetPitch - startPitch) * smoothTau));
        } else {
            // Infinite-Horizon Regulation Phase (Proportional Controller)
            float yawErr = Mth.wrapDegrees(targetYaw - internalYaw);
            float pitchErr = targetPitch - internalPitch;
            
            // Smoothing constant: 15.0 * dt
            double alpha = 1.0 - Math.exp(-15.0 * dt);
            internalYaw += yawErr * alpha;
            internalPitch += pitchErr * alpha;
        }

        // Apply Differential Drift & Quantization
        double desiredYawDelta = (internalYaw - prevInternalYaw) + (driftX - prevDriftX) + yawResidue;
        double desiredPitchDelta = (internalPitch - prevInternalPitch) + (driftY - prevDriftY) + pitchResidue;
        
        prevDriftX = driftX;
        prevDriftY = driftY;

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
