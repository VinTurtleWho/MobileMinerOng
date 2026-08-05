package com.mobileminerong.control;

import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

import java.util.Random;

public class RotationEngine {

    // Internal State
    private double yawResidue = 0.0;
    private double pitchResidue = 0.0;
    
    // Trajectory State (Minimum-Jerk)
    private float startYaw;
    private float startPitch;
    private float targetYaw;
    private float targetPitch;
    private int totalTicks;
    private int currentTick = 0;
    private boolean active = false;

    public RotationEngine() {}

    public void abort() {
        this.active = false;
        this.yawResidue = 0.0;
        this.pitchResidue = 0.0;
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

        float deltaYaw = Math.abs(Mth.wrapDegrees(targetYaw - startYaw));
        float deltaPitch = Math.abs(Mth.wrapDegrees(targetPitch - startPitch));
        float maxDelta = Math.max(deltaYaw, deltaPitch);

        this.totalTicks = Math.max(1, (int) (maxDelta / 5.0f)); // Slower, smoother base
        this.currentTick = 0;
        this.active = true;
    }

    // This will be called by MouseHandlerMixin (Render Tick Frequency)
    public int[] computeNextFrameSteps(float currentYaw, float currentPitch, float tickDelta) {
        if (!active) return new int[]{0, 0};

        currentTick++;
        float tau = (float) currentTick / totalTicks;
        // 5th-order minimum-jerk: 10τ³ - 15τ⁴ + 6τ⁵
        float smoothTau = tau * tau * tau * (10.0f - 15.0f * tau + 6.0f * tau * tau);

        float yaw = startYaw + smoothTau * Mth.wrapDegrees(targetYaw - startYaw);
        float pitch = startPitch + smoothTau * Mth.wrapDegrees(targetPitch - startPitch);

        // ... [To be continued in implementation: GCD and Residue]
        return new int[]{0, 0}; 
    }

    public boolean isActive() { return active; }
}
