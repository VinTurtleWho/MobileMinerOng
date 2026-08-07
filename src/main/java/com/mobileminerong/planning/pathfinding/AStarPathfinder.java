package com.mobileminerong.planning.pathfinding;

import com.mobileminerong.context.BotContext;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.*;

public class AStarPathfinder {

    /**
     * Find a path from start to target using A* with 26-neighbor 3D search.
     * Uses Euclidean distance for both heuristic and gCost to produce correct paths.
     * Thread-safe — call from any thread.
     *
     * @param start         Starting block position (player's feet)
     * @param target        Target block position (ore/destination)
     * @param maxIterations Maximum A* iterations before giving up (use 5000 for Dwarven Mines)
     * @param world         The level to pathfind in (pass client.level)
     * @return Smoothed list of waypoints, or empty list if no path found
     */
    public static List<BlockPos> findPath(BlockPos start, BlockPos target, int maxIterations, Level world) {
        if (world == null) return Collections.emptyList();

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(Node::getFCost));
        Set<BlockPos> openSetMembership = new HashSet<>();
        Map<BlockPos, Node> allNodes = new HashMap<>();
        Set<BlockPos> closedSet = new HashSet<>();

        Node startNode = new Node(start);
        startNode.gCost = 0;
        startNode.hCost = euclidean(start, target);
        openSet.add(startNode);
        openSetMembership.add(start);
        allNodes.put(start, startNode);

        int iterations = 0;

        while (!openSet.isEmpty() && iterations < maxIterations) {
            iterations++;
            Node current = openSet.poll();
            openSetMembership.remove(current.pos);

            // Accept if within 1.5 blocks of target (handles non-walkable target positions)
            if (current.pos.equals(target) || euclidean(current.pos, target) <= 1.5) {
                List<BlockPos> raw = retracePath(current);
                return smoothPath(raw, world);
            }

            closedSet.add(current.pos);

            for (BlockPos neighborPos : getNeighbors(world, current.pos)) {
                if (closedSet.contains(neighborPos)) continue;

                // Euclidean step cost — correct unit for heuristic comparison
                double stepCost = euclidean(current.pos, neighborPos);
                double newGCost = current.gCost + stepCost;

                Node neighbor = allNodes.getOrDefault(neighborPos, new Node(neighborPos));

                if (newGCost < neighbor.gCost || !openSetMembership.contains(neighborPos)) {
                    neighbor.gCost = newGCost;
                    neighbor.hCost = euclidean(neighborPos, target);
                    neighbor.parent = current;

                    allNodes.put(neighborPos, neighbor);
                    if (!openSetMembership.contains(neighborPos)) {
                        openSet.add(neighbor);
                        openSetMembership.add(neighborPos);
                    }
                }
            }
        }

        return Collections.emptyList();
    }

    // -------------------------------------------------------------------------
    // Heuristic — Euclidean distance (correct, not squared)
    // -------------------------------------------------------------------------

    private static double euclidean(BlockPos a, BlockPos b) {
        return Math.sqrt(a.distSqr(b));
    }

    // -------------------------------------------------------------------------
    // Neighbor generation — full 26-neighbor 3D cube
    // -------------------------------------------------------------------------

    private static List<BlockPos> getNeighbors(Level world, BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>(26);

        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;

                    BlockPos candidate = pos.offset(dx, dy, dz);

                    // For diagonal moves, verify no corner-cutting through solid blocks
                    if (!isWalkable(world, candidate)) continue;

                    // Cardinal cut check: diagonals must not clip through adjacent solid blocks
                    if (dx != 0 && dz != 0) {
                        // Horizontal diagonal — check both adjacent cardinals are clear
                        if (!isPassable(world, pos.offset(dx, 0, 0)) || 
                            !isPassable(world, pos.offset(0, 0, dz))) continue;
                    }
                    if (dx != 0 && dy != 0 && dz == 0) {
                        // Vertical+horizontal diagonal
                        if (!isPassable(world, pos.offset(dx, 0, 0)) || 
                            !isPassable(world, pos.offset(0, dy, 0))) continue;
                    }
                    if (dx == 0 && dy != 0 && dz != 0) {
                        if (!isPassable(world, pos.offset(0, 0, dz)) || 
                            !isPassable(world, pos.offset(0, dy, 0))) continue;
                    }
                    if (dx != 0 && dy != 0 && dz != 0) {
                        // Full corner diagonal — check all three adjacent faces
                        if (!isPassable(world, pos.offset(dx, 0, 0)) || 
                            !isPassable(world, pos.offset(0, 0, dz)) ||
                            !isPassable(world, pos.offset(0, dy, 0))) continue;
                    }

                    neighbors.add(candidate);
                }
            }
        }

        return neighbors;
    }

    // -------------------------------------------------------------------------
    // Walkability — full player collision shape check
    // -------------------------------------------------------------------------

    /**
     * A position is walkable if:
     * - The feet block (pos) has no collision shape (can stand in it)
     * - The head block (pos+1) has no collision shape (can stand in it)
     * - The floor block (pos-1) has a collision shape (something to stand on)
     */
    public static boolean isWalkable(Level world, BlockPos pos) {
        BlockState feet = world.getBlockState(pos);
        BlockState head = world.getBlockState(pos.above());
        BlockState floor = world.getBlockState(pos.below());

        boolean feetClear = feet.getCollisionShape(world, pos).isEmpty();
        boolean headClear = head.getCollisionShape(world, pos.above()).isEmpty();
        boolean floorSolid = !floor.getCollisionShape(world, pos.below()).isEmpty();

        return feetClear && headClear && floorSolid;
    }

    /**
     * A position is passable if it has no collision shape.
     * Used for diagonal corner-cutting checks — we only need to verify
     * intermediate blocks don't have collision, not full walkability.
     */
    private static boolean isPassable(Level world, BlockPos pos) {
        return world.getBlockState(pos).getCollisionShape(world, pos).isEmpty();
    }

    // -------------------------------------------------------------------------
    // Path retracing
    // -------------------------------------------------------------------------

    private static List<BlockPos> retracePath(Node endNode) {
        List<BlockPos> path = new ArrayList<>();
        Node current = endNode;
        while (current != null) {
            path.add(current.pos);
            current = current.parent;
        }
        Collections.reverse(path);
        return path;
    }

    // -------------------------------------------------------------------------
    // Greedy forward path smoothing
    //
    // For each anchor point, find the FURTHEST point reachable in a straight
    // line (via raycast), then jump directly to it. One pass, maximum
    // compression. A 30-waypoint straight tunnel becomes 2 waypoints.
    // -------------------------------------------------------------------------

    private static List<BlockPos> smoothPath(List<BlockPos> path, Level world) {
        if (path.size() < 3) return path;

        List<BlockPos> smoothed = new ArrayList<>();
        smoothed.add(path.get(0));

        int anchor = 0;
        while (anchor < path.size() - 1) {
            // Scan forward: find furthest waypoint reachable in straight line
            int furthest = anchor + 1;
            for (int i = anchor + 2; i < path.size(); i++) {
                if (isStraightLineWalkable(world, path.get(anchor), path.get(i))) {
                    furthest = i;
                } else {
                    break; // Can't see further, stop scanning
                }
            }
            anchor = furthest;
            smoothed.add(path.get(anchor));
        }

        return smoothed;
    }

    /**
     * Raycast between two block positions using player collision context.
     * Returns true if the straight line is unobstructed.
     */
    private static boolean isStraightLineWalkable(Level world, BlockPos start, BlockPos end) {
        Vec3 startVec = Vec3.atCenterOf(start);
        Vec3 endVec = Vec3.atCenterOf(end);

        net.minecraft.client.Minecraft client = Minecraft.getInstance();
        net.minecraft.world.entity.player.Player player = client.player;
        CollisionContext context = (player != null)
            ? CollisionContext.of(player)
            : CollisionContext.empty();

        HitResult hit = world.clip(new ClipContext(
            startVec, endVec,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            context
        ));

        return hit.getType() == HitResult.Type.MISS;
    }
}
