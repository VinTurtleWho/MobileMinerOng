package com.mobileminerong.planning.pathfinding;

import net.minecraft.world.phys.Vec3;

/**
 * Centripetal Catmull-Rom Spline Math Utility (alpha = 0.5)
 * Reference: Barry-Goldman Pyramidal Formulation
 */
public class CentripetalSpline {

    public static Vec3 evaluate(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, double t) {
        double t0 = 0.0;
        double t1 = t0 + Math.pow(p1.distanceTo(p0), 0.5);
        double t2 = t1 + Math.pow(p2.distanceTo(p1), 0.5);
        double t3 = t2 + Math.pow(p3.distanceTo(p2), 0.5);

        // Map t into [t1, t2] range
        t = t1 + t * (t2 - t1);

        Vec3 a1 = p0.scale((t1 - t) / (t1 - t0)).add(p1.scale((t - t0) / (t1 - t0)));
        Vec3 a2 = p1.scale((t2 - t) / (t2 - t1)).add(p2.scale((t - t1) / (t2 - t1)));
        Vec3 a3 = p2.scale((t3 - t) / (t3 - t2)).add(p3.scale((t - t2) / (t3 - t2)));

        Vec3 b1 = a1.scale((t2 - t) / (t2 - t0)).add(a2.scale((t - t0) / (t2 - t0)));
        Vec3 b2 = a2.scale((t3 - t) / (t3 - t1)).add(a3.scale((t - t1) / (t3 - t1)));

        return b1.scale((t2 - t) / (t2 - t1)).add(b2.scale((t - t1) / (t2 - t1)));
    }
}
