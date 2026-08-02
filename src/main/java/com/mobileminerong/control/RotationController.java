package com.mobileminerong.control;

import com.mobileminerong.context.BotContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class RotationController {
    // Maximum turn rate per tick to maintain smooth humanized movement
    private static final float MAX_DEGREES_PER_TICK = 22.5f;

    public static void lookAt(BotContext ctx, Vec3d targetPos) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        Vec3d eyesPos = client.player.getEyePos();
        double diffX = targetPos.x - eyesPos.x;
        double diffY = targetPos.y - eyesPos.y;
        double diffZ = targetPos.z - eyesPos.z;

        double diffXZ = Math.sqrt(diffX * diffX + diffZ * diffZ);
        float targetYaw = MathHelper.wrapDegrees((float) Math.toDegrees(Math.atan2(diffZ, diffX)) - 90.0f);
        float targetPitch = MathHelper.wrapDegrees((float) -Math.toDegrees(Math.atan2(diffY, diffXZ)));

        float currentYaw = client.player.getYaw();
        float currentPitch = client.player.getPitch();

        // Calculate angular delta
        float deltaYaw = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float deltaPitch = MathHelper.wrapDegrees(targetPitch - currentPitch);

        // Clamp turn rates
        float clampedDeltaYaw = MathHelper.clamp(deltaYaw, -MAX_DEGREES_PER_TICK, MAX_DEGREES_PER_TICK);
        float clampedDeltaPitch = MathHelper.clamp(deltaPitch, -MAX_DEGREES_PER_TICK, MAX_DEGREES_PER_TICK);

        float newYaw = currentYaw + clampedDeltaYaw;
        float newPitch = MathHelper.clamp(currentPitch + clampedDeltaPitch, -90.0f, 90.0f);

        client.player.setYaw(newYaw);
        client.player.setPitch(newPitch);
        ctx.setRotations(newYaw, newPitch);
        ctx.setLastAction("Rotating -> Yaw: " + (int)newYaw + " Pitch: " + (int)newPitch);
    }
}
