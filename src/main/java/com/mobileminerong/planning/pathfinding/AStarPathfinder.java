package com.mobileminerong.planning.pathfinding;

import com.mobileminerong.context.BotContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;

import java.util.*;

public class AStarPathfinder {

    public static List<BlockPos> findPath(BotContext ctx, BlockPos start, BlockPos target, int maxIterations) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return Collections.emptyList();

        Level world = client.level;
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(Node::getFCost));
        Map<BlockPos, Node> allNodes = new HashMap<>();
        Set<BlockPos> closedSet = new HashSet<>();

        Node startNode = new Node(start);
        startNode.gCost = 0;
        startNode.hCost = heuristic(start, target);
        openSet.add(startNode);
        allNodes.put(start, startNode);

        int iterations = 0;

        while (!openSet.isEmpty() && iterations < maxIterations) {
            iterations++;
            Node current = openSet.poll();

            if (current.pos.equals(target) || current.pos.distSqr(target) <= 2.25) {
                return retracePath(current);
            }

            closedSet.add(current.pos);

            for (BlockPos neighborPos : getNeighbors(world, current.pos)) {
                if (closedSet.contains(neighborPos)) continue;

                double newGCost = current.gCost + Math.sqrt(current.pos.distSqr(neighborPos));
                Node neighbor = allNodes.getOrDefault(neighborPos, new Node(neighborPos));

                if (newGCost < neighbor.gCost || !openSet.contains(neighbor)) {
                    neighbor.gCost = newGCost;
                    neighbor.hCost = heuristic(neighborPos, target);
                    neighbor.parent = current;

                    allNodes.put(neighborPos, neighbor);
                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }

        ctx.setLastAction("Pathfinder failed to find valid route within limits");
        return Collections.emptyList();
    }

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

    private static double heuristic(BlockPos a, BlockPos b) {
        return Math.sqrt(a.distSqr(b));
    }

    private static List<BlockPos> getNeighbors(Level world, BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();
        BlockPos[] candidates = new BlockPos[]{
            pos.north(), pos.south(), pos.east(), pos.west(),
            pos.north().above(), pos.south().above(), pos.east().above(), pos.west().above(),
            pos.north().below(), pos.south().below(), pos.east().below(), pos.west().below()
        };

        for (BlockPos candidate : candidates) {
            if (isWalkable(world, candidate)) {
                neighbors.add(candidate);
            }
        }
        return neighbors;
    }

    public static boolean isWalkable(Level world, BlockPos pos) {
        BlockState feet = world.getBlockState(pos);
        BlockState head = world.getBlockState(pos.above());
        BlockState floor = world.getBlockState(pos.below());

        boolean feetClear = feet.isAir() || !feet.isCollisionShapeFullBlock(world, pos);
        boolean headClear = head.isAir() || !head.isCollisionShapeFullBlock(world, pos.above());
        boolean floorSolid = !floor.isAir() && floor.isCollisionShapeFullBlock(world, pos.below());

        return feetClear && headClear && floorSolid;
    }
}
