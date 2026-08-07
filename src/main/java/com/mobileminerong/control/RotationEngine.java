package com.mobileminerong.control;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import com.mobileminerong.util.OrnsteinUhlenbeckDrift;

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

    // Biological Drift (Continuous Resting Tremor)
    private final OrnsteinUhlenbeckDrift drift = new OrnsteinUhlenbeckDrift();

    public RotationEngine() {}

    public synchronized void abort() {
        this.active = false;
        this.yawResidue = 0.0;
        this.pitchResidue = 0.0;
    }

    public synchronized void setActive(boolean active) { this.active = active; }
    public synchronized boolean isActive() { return active; }

    public synchronized void startRotation(float currentYaw, float currentPitch, Vec3 targetPos) {
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

        // Fitts' Law Duration Calculation: MT = a + b * log2(1 + A / W)
        double amplitude = Math.sqrt(Math.pow(Mth.wrapDegrees(targetYaw - startYaw), 2) + Math.pow(targetPitch - startPitch, 2));
        double targetWidth = 5.0; // Dynamic bounding-width approximation constant (5.0 degrees)
        double a = 2.0; // Empirical min latency in ticks
        double b = 3.5; // Logarithmic scaling parameter in ticks
        double indexOfDifficulty = Math.log(1.0 + (amplitude / targetWidth)) / Math.log(2.0);
        double movementTimeTicks = a + b * indexOfDifficulty;

        this.totalTicks = Math.max(1, (int) Math.round(movementTimeTicks));
        this.currentTick = 0;
        this.active = true;
    }

    public synchronized int[] computeNextFrameSteps(Vec3 targetPos) {
        if (!active) return new int[]{0, 0};

        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return new int[]{0, 0};

        Vec3 eyesPos = client.player.getEyePosition();
        double dx = targetPos.x - eyesPos.x;
        double dy = targetPos.y - eyesPos.y;
        double dz = targetPos.z - eyesPos.z;
        float liveTargetYaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(dz, dx)) - 90.0f);
        float liveTargetPitch = Mth.clamp((float) -Math.toDegrees(Math.atan2(dy, Math.sqrt(dx*dx + dz*dz))), -90.0f, 90.0f);

        float currentYaw;
        float currentPitch;

        float prevYaw;
        float prevPitch;

        updateGcd();
        drift.update();

        // Apply stochastic physiological hand tremor
        double driftX = drift.getX();
        double driftY = drift.getY();

        if (currentTick < totalTicks) {
            // Finite-Horizon Reaching Phase (Minimum-Jerk)
            currentTick++;
            double tau = (double) currentTick / totalTicks;
            double smoothTau = tau * tau * tau * (10.0 - 15.0 * tau + 6.0 * tau * tau);

            currentYaw = (float) (startYaw + (Mth.wrapDegrees(liveTargetYaw - startYaw) * smoothTau) + driftX);
            currentPitch = (float) (startPitch + (Mth.wrapDegrees(liveTargetPitch - startPitch) * smoothTau) + driftY);

            double prevTau = (double) (currentTick - 1) / totalTicks;
            double smoothPrevTau = (currentTick == 1) ? 0.0 : prevTau * prevTau * prevTau * (10.0 - 15.0 * prevTau + 6.0 * prevTau * prevTau);

            prevYaw = (float) (startYaw + (Mth.wrapDegrees(liveTargetYaw - startYaw) * smoothPrevTau) + driftX);
            prevPitch = (float) (startPitch + (Mth.wrapDegrees(liveTargetPitch - startPitch) * smoothPrevTau) + driftY);
        } else {
            // Infinite-Horizon Regulation Phase (Lock-on & micro-tremor)
            currentYaw = (float) (liveTargetYaw + driftX);
            currentPitch = (float) (liveTargetPitch + driftY);

            prevYaw = client.player.getYRot();
            prevPitch = client.player.getXRot();
        }

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
