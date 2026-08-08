package com.mobileminerong.control;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import com.mobileminerong.util.OrnsteinUhlenbeckDrift;
import com.mobileminerong.debug.DebugLogger;

public class RotationEngine {

    private float startYaw, startPitch, targetYaw, targetPitch;
    private int totalTicks;
    private long startTime;
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
        updateTarget(targetPos);

        double amplitude = Math.sqrt(Math.pow(Mth.wrapDegrees(targetYaw - startYaw), 2) + Math.pow(targetPitch - startPitch, 2));
        double MT = 1.0 + 1.8 * Math.log(1.0 + (amplitude / 5.0)) / Math.log(2.0);
        this.totalTicks = Math.max(1, (int) Math.round(MT));
        this.startTime = System.currentTimeMillis();
        this.active = true;
        this.prevDriftX = drift.getX();
        this.prevDriftY = drift.getY();
    }

    public synchronized void updateTarget(Vec3 targetPos) {
        if (!active) return;
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;
        Vec3 eyesPos = client.player.getEyePosition();
        double dx = targetPos.x - eyesPos.x;
        double dy = targetPos.y - eyesPos.y;
        double dz = targetPos.z - eyesPos.z;
        this.targetYaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f);
        this.targetPitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx*dx + dz*dz))), -90.0f, 90.0f);
        
        DebugLogger.debug("ROTATION", "Target updated to: " + targetYaw + "/" + targetPitch);
    }

    public synchronized int[] computeNextFrameSteps() {
        if (!active) return new int[]{0, 0};
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return new int[]{0, 0};

        drift.update();
        double driftX = drift.getX();
        double driftY = drift.getY();

        float currentYaw, currentPitch;
        float prevYaw, prevPitch;
        
        long elapsed = System.currentTimeMillis() - startTime;
        double tau = Math.min(1.0, (double) elapsed / (totalTicks * 50.0));
        
        DebugLogger.debug("ROTATION", "Tau: " + tau + " Active: " + active);

        if (tau < 1.0) {
            double smoothTau = tau * tau * tau * (10.0 - 15.0 * tau + 6.0 * tau * tau);
            currentYaw = (float) (startYaw + (Mth.wrapDegrees(targetYaw - startYaw) * smoothTau));
            currentPitch = (float) (startPitch + (Mth.wrapDegrees(targetPitch - startPitch) * smoothTau));

            double prevTau = Math.max(0.0, tau - (1.0 / (totalTicks * 50.0)));
            double smoothPrevTau = prevTau * prevTau * prevTau * (10.0 - 15.0 * prevTau + 6.0 * prevTau * prevTau);
            prevYaw = (float) (startYaw + (Mth.wrapDegrees(targetYaw - startYaw) * smoothPrevTau));
            prevPitch = (float) (startPitch + (Mth.wrapDegrees(targetPitch - startPitch) * smoothPrevTau));
        } else {
            currentYaw = targetYaw;
            currentPitch = targetPitch;
            prevYaw = client.player.getYRot();
            prevPitch = client.player.getXRot();
        }

        // Apply Differential Drift: Delta = (P_n - P_n-1) + (D_n - D_n-1)
        double desiredYawDelta = Mth.wrapDegrees(currentYaw - prevYaw) + (driftX - prevDriftX) + yawResidue;
        double desiredPitchDelta = (currentPitch - prevPitch) + (driftY - prevDriftY) + pitchResidue;
        
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
