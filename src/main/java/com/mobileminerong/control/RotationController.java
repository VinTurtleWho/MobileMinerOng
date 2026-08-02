package com.mobileminerong.control;

import com.mobileminerong.context.BotContext;
import net.minecraft.client.Minecraft;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class RotationController {
    // Maximum turn rate per tick to maintain smooth humanized movement
    private static final float MAX_DEGREES_PER_TICK = 22.5f;

    public static void lookAt(BotContext ctx, Vec3 targetPos) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null) return;

        Vec3 eyesPos = client.player.getEyePos();
        double diffX = targetPos.x - eyesPos.x;
        double diffY = targetPos.y - eyesPos.y;
        double diffZ = targetPos.z - eyesPos.z;

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float targetYaw = Mth.wrapDegrees((float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f);
        float targetPitch = Mth.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)));

        float currentYaw = client.player.getYaw();
        float currentPitch = client.player.getPitch();

        // Calculate angular delta
        float deltaYaw = Mth.wrapDegrees(targetYaw - currentYaw);
        float deltaPitch = Mth.wrapDegrees(targetPitch - currentPitch);

        // Clamp turn rates
        float clampedDeltaYaw = Mth.clamp(deltaYaw, -MAX_DEGREES_PER_TICK, MAX_DEGREES_PER_TICK);
        float clampedDeltaPitch = Mth.clamp(deltaPitch, -MAX_DEGREES_PER_TICK, MAX_DEGREES_PER_TICK);

        float newYaw = currentYaw + clampedDeltaYaw;
        float newPitch = Mth.clamp(currentPitch + clampedDeltaPitch, -90.0f, 90.0f);

        client.player.setYaw(newYaw);
        client.player.setPitch(newPitch);
        ctx.setRotations(newYaw, newPitch);
        ctx.setLastAction("Rotating -> Yaw: " + (int)newYaw + " Pitch: " + (int)newPitch);
    }
}
