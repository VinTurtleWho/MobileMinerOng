package com.mobileminerong.planning.pathfinding;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

import java.util.*;

public class LazyThetaPathfinder {

    public static List<BlockPos> findPath(BlockPos start, BlockPos target, int maxIterations, Level world) {
        if (world == null) return Collections.emptyList();

        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(Node::getFCost));
        Map<BlockPos, Node> allNodes = new HashMap<>();
        Set<BlockPos> closedSet = new HashSet<>();

        Node startNode = new Node(start);
        startNode.gCost = 0;
        startNode.hCost = euclidean(start, target);
        startNode.parent = startNode;
        openSet.add(startNode);
        allNodes.put(start, startNode);

        int iterations = 0;
        while (!openSet.isEmpty() && iterations < maxIterations) {
            iterations++;
            Node s = openSet.poll();

            if (s.pos.equals(target) || euclidean(s.pos, target) <= 1.5) {
                return retracePath(s);
            }

            closedSet.add(s.pos);

            // Lazy Theta*: Deferred LoS and Cost Repair
            updateVertex(s, s.parent, world);

            for (BlockPos neighborPos : getNeighbors(world, s.pos)) {
                if (closedSet.contains(neighborPos)) continue;

                Node sPrime = allNodes.getOrDefault(neighborPos, new Node(neighborPos));

                // Lazy Theta*: Optimistic parent assignment
                double tempCost = s.parent.gCost + euclidean(s.parent.pos, sPrime.pos);
                if (tempCost < sPrime.gCost) {
                    sPrime.gCost = tempCost;
                    sPrime.hCost = euclidean(sPrime.pos, target);
                    sPrime.parent = s.parent;
                    allNodes.put(neighborPos, sPrime);
                    openSet.add(sPrime);
                }
            }
        }
        return Collections.emptyList();
    }

    private static void updateVertex(Node s, Node parent, Level world) {
        if (s.parent != parent) {
            if (isStraightLineWalkable(world, parent.pos, s.pos)) {
                s.parent = parent;
                s.gCost = parent.gCost + euclidean(parent.pos, s.pos);
            }
        }
    }

    private static double euclidean(BlockPos a, BlockPos b) {
        return Math.sqrt(a.distSqr(b));
    }

    private static List<BlockPos> getNeighbors(Level world, BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>(26);
        for (int dx = -1; dx <= 1; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -1; dz <= 1; dz++) {
                    if (dx == 0 && dy == 0 && dz == 0) continue;
                    BlockPos candidate = pos.offset(dx, dy, dz);
                    if (isWalkable(world, candidate)) neighbors.add(candidate);
                }
            }
        }
        return neighbors;
    }

    public static boolean isWalkable(Level world, BlockPos pos) {
        BlockState feet = world.getBlockState(pos);
        BlockState head = world.getBlockState(pos.above());
        BlockState floor = world.getBlockState(pos.below());
        return feet.getCollisionShape(world, pos).isEmpty() && 
               head.getCollisionShape(world, pos.above()).isEmpty() && 
               !floor.getCollisionShape(world, pos.below()).isEmpty();
    }

    private static List<BlockPos> retracePath(Node endNode) {
        List<BlockPos> path = new ArrayList<>();
        Node current = endNode;
        while (current != null && current != current.parent) {
            path.add(current.pos);
            current = current.parent;
        }
        path.add(current.pos);
        Collections.reverse(path);
        return path;
    }

    private static boolean isStraightLineWalkable(Level world, BlockPos start, BlockPos end) {
        Vec3 startVec = Vec3.atCenterOf(start);
        Vec3 endVec = Vec3.atCenterOf(end);
        CollisionContext context = CollisionContext.of(Minecraft.getInstance().player);
        HitResult hit = world.clip(new ClipContext(startVec, endVec, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, context));
        return hit.getType() == HitResult.Type.MISS;
    }
}
