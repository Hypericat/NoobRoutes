package noobroutes.utils.pathfinding;

import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3i;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class PathNode {
    private final BlockPos pos;
    private double moveCost;
    private final double heuristicCost;
    public int heapPosition;
    private final Goal goal;
    private PathNode parent;

    public PathNode(BlockPos pos, PathNode parent, Goal goal) {
        this.pos = pos;
        this.goal = goal;
        this.parent = parent;
        this.moveCost = 2147483647;
        this.heapPosition = -1;
        this.heuristicCost = goal.heuristic(pos);
    }

    public BlockPos getPos() {
        return pos;
    }

    public List<BlockPos> getNear(Predicate<BlockPos> predicate) {
        List<BlockPos> blocks = new ArrayList<>();
        testOffset(predicate, blocks, pos.add(1, 0, 0));
        testOffset(predicate, blocks, pos.add(-1, 0, 0));
        testOffset(predicate, blocks, pos.add(0, 0, -1));
        testOffset(predicate, blocks, pos.add(0, 0, 1));
        testOffset(predicate, blocks, pos.add(0, -1, 0));
        testOffset(predicate, blocks, pos.add(0, 1, 0));
        return blocks;
    }

    public void setMoveCost(double cost) {
        this.moveCost = cost;
    }

    public boolean isOpen() {
        return heapPosition != -1;
    }

    @Override
    public int hashCode() {
        long hash = 3241;
        hash = 3457689L * hash + this.pos.getX();
        hash = 8734625L * hash + this.pos.getY();
        hash = 2873465L * hash + this.pos.getZ();
        return (int) hash;
    }

    public static int hashCode(BlockPos pos) {
        long hash = 3241;
        hash = 3457689L * hash + pos.getX();
        hash = 8734625L * hash + pos.getY();
        hash = 2873465L * hash + pos.getZ();
        return (int) hash;
    }

    public synchronized PathNode getParent() {
        return this.parent;
    }

    private void testOffset(Predicate<BlockPos> predicate, List<BlockPos> blocks, BlockPos pos) {
        if (predicate.test(pos)) blocks.add(pos);
    }

    public synchronized double getCost() {
        return moveCost + heuristicCost;
    }

    public double getHeuristicCost() {
        return heuristicCost;
    }

    public synchronized double getMoveCost() {
        return moveCost;
    }

    public synchronized void updateParent(PathNode parent) {
        this.parent = parent;
        this.moveCost = parent.moveCost + PathFinder.NEW_NODE_COST;
    }

    @Override
    public synchronized boolean equals(Object obj) {
        PathNode other = (PathNode) obj;
        return pos.getX() == other.pos.getX() && pos.getY() == other.pos.getY() && pos.getZ() == other.pos.getZ();
    }

}
