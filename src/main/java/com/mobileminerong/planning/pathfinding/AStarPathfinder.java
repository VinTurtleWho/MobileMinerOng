package com.mobileminerong.planning.pathfinding;

import com.mobileminerong.context.BotContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.*;

public class AStarPathfinder {

    public static List<BlockPos> findPath(BotContext ctx, BlockPos start, BlockPos target, int maxIterations) {
        Minecraft client = Minecraft.getInstance();
        if (client.level == null) return Collections.emptyList();

        Level world = client.level;
        PriorityQueue<Node> openSet = new PriorityQueue<>(Comparator.comparingDouble(Node::getFCost));
        Set<BlockPos> openSetMembership = new HashSet<>();
        Map<BlockPos, Node> allNodes = new HashMap<>();
        Set<BlockPos> closedSet = new HashSet<>();

        Node startNode = new Node(start);
        startNode.gCost = 0;
        startNode.hCost = heuristic(start, target);
        openSet.add(startNode);
        openSetMembership.add(start);
        allNodes.put(start, startNode);

        int iterations = 0;

        while (!openSet.isEmpty() && iterations < maxIterations) {
            iterations++;
            Node current = openSet.poll();
            openSetMembership.remove(current.pos);

            if (current.pos.equals(target) || current.pos.distSqr(target) <= 2.25) {
                return smoothPath(retracePath(current), world);
            }

            closedSet.add(current.pos);

            for (BlockPos neighborPos : getNeighbors(world, current.pos)) {
                if (closedSet.contains(neighborPos)) continue;

                double newGCost = current.gCost + current.pos.distSqr(neighborPos);
                Node neighbor = allNodes.getOrDefault(neighborPos, new Node(neighborPos));

                if (newGCost < neighbor.gCost || !openSetMembership.contains(neighborPos)) {
                    neighbor.gCost = newGCost;
                    neighbor.hCost = heuristic(neighborPos, target);
                    neighbor.parent = current;

                    allNodes.put(neighborPos, neighbor);
                    if (!openSetMembership.contains(neighborPos)) {
                        openSet.add(neighbor);
                        openSetMembership.add(neighborPos);
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

    private static List<BlockPos> smoothPath(List<BlockPos> path, Level world) {
        if (path.size() < 3) return path;

        List<BlockPos> smoothed = new ArrayList<>();
        smoothed.add(path.get(0));

        int i = 0;
        while (i < path.size() - 2) {
            BlockPos p1 = path.get(i);
            BlockPos p2 = path.get(i + 2);

            if (isStraightLineWalkable(world, p1, p2)) {
                i += 2;
            } else {
                smoothed.add(path.get(i + 1));
                i++;
            }
        }
        smoothed.add(path.get(path.size() - 1));
        return smoothed;
    }

    private static boolean isStraightLineWalkable(Level world, BlockPos start, BlockPos end) {
        Vec3 startVec = Vec3.atCenterOf(start);
        Vec3 endVec = Vec3.atCenterOf(end);
        
        net.minecraft.world.entity.player.Player player = Minecraft.getInstance().player;
        net.minecraft.world.phys.shapes.CollisionContext context = (player != null) ? 
            net.minecraft.world.phys.shapes.CollisionContext.of(player) : 
            net.minecraft.world.phys.shapes.CollisionContext.empty();

        // Raycast to check for block collisions
        net.minecraft.world.phys.HitResult hitResult = world.clip(new net.minecraft.world.level.ClipContext(
                startVec, endVec,
                net.minecraft.world.level.ClipContext.Block.COLLIDER,
                net.minecraft.world.level.ClipContext.Fluid.NONE,
                context
        ));
        
        return hitResult.getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }

    private static double heuristic(BlockPos a, BlockPos b) {
        return a.distSqr(b);
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

        // A position blocks passage if it has any collision shape at all (stairs, slabs, fences, etc.)
        boolean feetClear = feet.getCollisionShape(world, pos).isEmpty();
        boolean headClear = head.getCollisionShape(world, pos.above()).isEmpty();
        // Floor must have collision to stand on
        boolean floorSolid = !floor.getCollisionShape(world, pos.below()).isEmpty();

        return feetClear && headClear && floorSolid;
    }
}
