package com.mobileminerong.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import java.util.Random;

public class TargetSurfaceScanner {
    private static final Random random = new Random();

    public static Vec3 samplePoint(Entity entity) {
        AABB box = entity.getBoundingBox();
        double x = box.minX + (random.nextDouble() * (box.maxX - box.minX));
        double y = box.minY + (random.nextDouble() * (box.maxY - box.minY));
        double z = box.minZ + (random.nextDouble() * (box.maxZ - box.minZ));
        return new Vec3(x, y, z);
    }
}
