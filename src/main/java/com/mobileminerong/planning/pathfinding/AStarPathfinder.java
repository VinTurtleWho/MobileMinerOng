package com.mobileminerong.planning.pathfinding;

import com.mobileminerong.context.BotContext;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

public class AStarPathfinder {

    public static List<BlockPos> findPath(BotContext ctx, BlockPos start, BlockPos target, int maxIterations) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) return Collections.emptyList();

        World world = client.world;
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

            if (current.pos.equals(target) || current.pos.getSquaredDistance(target) <= 2.25) {
                return retracePath(current);
            }

            closedSet.add(current.pos);

            for (BlockPos neighborPos : getNeighbors(world, current.pos)) {
                if (closedSet.contains(neighborPos)) continue;

                double newGCost = current.gCost + Math.sqrt(current.pos.getSquaredDistance(neighborPos));
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
        return Math.sqrt(a.getSquaredDistance(b));
    }

    private static List<BlockPos> getNeighbors(World world, BlockPos pos) {
        List<BlockPos> neighbors = new ArrayList<>();
        BlockPos[] candidates = new BlockPos[]{
            pos.north(), pos.south(), pos.east(), pos.west(),
            pos.north().up(), pos.south().up(), pos.east().up(), pos.west().up(),
            pos.north().down(), pos.south().down(), pos.east().down(), pos.west().down()
        };

        for (BlockPos candidate : candidates) {
            if (isWalkable(world, candidate)) {
                neighbors.add(candidate);
            }
        }
        return neighbors;
    }

    public static boolean isWalkable(World world, BlockPos pos) {
        BlockState feet = world.getBlockState(pos);
        BlockState head = world.getBlockState(pos.up());
        BlockState floor = world.getBlockState(pos.down());

        boolean feetClear = feet.isAir() || !feet.isFullCube(world, pos);
        boolean headClear = head.isAir() || !head.isFullCube(world, pos.up());
        boolean floorSolid = !floor.isAir() && floor.isFullCube(world, pos.down());

        return feetClear && headClear && floorSolid;
    }
}
