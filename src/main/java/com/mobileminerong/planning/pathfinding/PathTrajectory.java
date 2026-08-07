package com.mobileminerong.planning.pathfinding;

import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

public class PathTrajectory {
    private final List<Vec3> waypoints;
    private int segmentIndex = 0;

    public PathTrajectory(List<BlockPos> rawWaypoints) {
        this.waypoints = new ArrayList<>();
        if (rawWaypoints == null || rawWaypoints.isEmpty()) return;

        // Populate and handle edge cases via duplication
        // Duplicate start and end for control points boundary
        Vec3 start = Vec3.atCenterOf(rawWaypoints.get(0));
        this.waypoints.add(start); // P0 = start

        for (BlockPos pos : rawWaypoints) {
            this.waypoints.add(Vec3.atCenterOf(pos));
        }

        Vec3 end = Vec3.atCenterOf(rawWaypoints.get(rawWaypoints.size() - 1));
        this.waypoints.add(end); // P3 = end
        this.segmentIndex = 0;
    }

    public Vec3 getLookahead(Vec3 playerPos) {
        if (waypoints.size() < 4) {
            return waypoints.isEmpty() ? Vec3.ZERO : waypoints.get(0);
        }

        // Clip segment index
        if (segmentIndex >= waypoints.size() - 3) {
            segmentIndex = waypoints.size() - 4;
        }

        Vec3 p0 = waypoints.get(segmentIndex);
        Vec3 p1 = waypoints.get(segmentIndex + 1);
        Vec3 p2 = waypoints.get(segmentIndex + 2);
        Vec3 p3 = waypoints.get(segmentIndex + 3);

        // Closest point projection to calculate precise 't' relative to segment length
        double t = findClosestT(p0, p1, p2, p3, playerPos);

        // Segment advancement logic
        if (t >= 0.95 && segmentIndex < waypoints.size() - 4) {
            segmentIndex++;
            t = 0.0;
            p0 = waypoints.get(segmentIndex);
            p1 = waypoints.get(segmentIndex + 1);
            p2 = waypoints.get(segmentIndex + 2);
            p3 = waypoints.get(segmentIndex + 3);
        }

        // Apply a safe 1.5 blocks dynamic lookahead
        double lookaheadDist = 1.5;
        double segmentLength = p1.distanceTo(p2);
        double deltaT = (segmentLength > 0) ? (lookaheadDist / segmentLength) : 0;
        double targetT = Math.min(1.0, t + deltaT);

        return CentripetalSpline.evaluate(p0, p1, p2, p3, targetT);
    }

    private double findClosestT(Vec3 p0, Vec3 p1, Vec3 p2, Vec3 p3, Vec3 playerPos) {
        double bestT = 0.0;
        double minDist = Double.MAX_VALUE;

        // Sample segment [P1, P2] at 10 slices for closest projection
        for (int i = 0; i <= 10; i++) {
            double candidateT = i / 10.0;
            Vec3 pointOnSpline = CentripetalSpline.evaluate(p0, p1, p2, p3, candidateT);
            double dist = playerPos.distanceToSqr(pointOnSpline);
            if (dist < minDist) {
                minDist = dist;
                bestT = candidateT;
            }
        }
        return bestT;
    }

    public boolean isDestinationReached(Vec3 playerPos) {
        if (waypoints.isEmpty()) return true;
        Vec3 destination = waypoints.get(waypoints.size() - 2); // P2 of final segment
        return playerPos.distanceToSqr(destination) <= 0.36; // 0.6 blocks limit
    }
}
