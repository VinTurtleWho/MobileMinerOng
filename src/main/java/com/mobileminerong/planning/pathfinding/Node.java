package com.mobileminerong.planning.pathfinding;

import net.minecraft.core.BlockPos;
import java.util.Objects;

public class Node {
    public final BlockPos pos;
    public Node parent;
    public double gCost = Double.MAX_VALUE; // Distance from start
    public double hCost = 0;                // Heuristic to target

    public Node(BlockPos pos) {
        this.pos = pos;
    }

    public double getFCost() {
        return gCost + hCost;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Node)) return false;
        Node node = (Node) o;
        return Objects.equals(pos, node.pos);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pos);
    }
}
