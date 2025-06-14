package noobroutes.utils.pathfinding;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.util.Vec3i;
import net.minecraft.world.World;
import noobroutes.utils.RotationUtils;
import noobroutes.utils.VecUtilsKt;
import noobroutes.utils.pathfinding.openset.BinaryHeapOpenSet;
import noobroutes.utils.skyblock.ChatUtilsKt;
import noobroutes.utils.skyblock.EtherWarpHelper;
import org.lwjgl.Sys;

import java.util.*;
import java.util.function.Predicate;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class PathFinder {
    private final Goal goal;
    private final BlockPos startPos;
    private final HashMap<Long, PathNode> cache;
    public static double NEW_NODE_COST = 100d;
    public final static double MIN_IMPROVEMENT = 1d;
    public final static double THREAD_COUNT = 12;

    private PathNode bestNode;
    private boolean complete;
    private BinaryHeapOpenSet nodes;
    private HashSet<Integer> processing;


    public PathFinder(Goal goal, BlockPos startPos, double nodeCost) {
        this.goal = goal;
        this.cache = new HashMap<>();
        this.startPos = startPos;

        NEW_NODE_COST = nodeCost;
    }

    public Path calculate() {
        long time = System.currentTimeMillis();

        nodes = new BinaryHeapOpenSet();
        processing = new HashSet<>();
        PathNode startNode = new PathNode(startPos, null, goal);
        startNode.setMoveCost(0d);
        nodes.insert(startNode);

        bestNode = startNode;
        complete = false;
        for (int i = 0; i < THREAD_COUNT - 1; i++ ) {
            Thread thread = new Thread(this::run);
            thread.start();
        }
        run();


        ChatUtilsKt.devMessage("Scanned all nodes!", "§8§l-<§r§aNoob Routes§r§8§l>-§r ", null);
        ChatUtilsKt.devMessage("Returning best path!", "§8§l-<§r§aNoob Routes§r§8§l>-§r ", null);
        outTime(time);

        return new Path(startPos, startNode, bestNode, goal);
    }

    private void run() {
        while (!isDone()) {
            checkNode(getLowest());
        }
    }


    private synchronized void updateNodes(PathNode node) {
        nodes.update(node);
    }

    private synchronized void insertNodes(PathNode node) {
        nodes.insert(node);
    }

    private synchronized PathNode getLowest() {
        if (nodes.isEmpty()) return null;
        PathNode lowest = nodes.removeLowest();
        processing.add(lowest.hashCode());
        return lowest;
    }

    private synchronized boolean isDone() {
        return nodes.isEmpty() && processing.isEmpty();
    }

    private synchronized void setComplete(boolean complete) {
        this.complete = complete;
    }

    private synchronized void finishNode(PathNode node) {
        if (!processing.contains(node.hashCode())) {
            System.err.println("Found node not in processing!");
        }
        processing.remove(node.hashCode());
    }

    private synchronized boolean isComplete() {
        return this.complete;
    }

    private synchronized double getBestNodeMoveCost() {
        return bestNode.getMoveCost();
    }

    private synchronized double getBestNodeHeuristic() {
        return this.bestNode.getHeuristicCost();
    }

    private synchronized void setBestNode(PathNode node) {
        if (complete) {
            if (node.getMoveCost() < bestNode.getMoveCost()) {
                bestNode = node;
            }
            return;
        }
        if (goal.test(node.getPos())) {
            bestNode = node;
            complete = true;
            return;
        }
        if (bestNode.getHeuristicCost() - node.getHeuristicCost() > MIN_IMPROVEMENT) {
            if (bestNode.getMoveCost() < getBestNodeMoveCost())
                this.bestNode = node;
        }

    }

    public void checkNode(PathNode checkNode) {
        if (checkNode == null) return;
        PathfinderExecutor.currentBlock = checkNode.getPos(); // TEMP

        if (goal.test(checkNode.getPos())) {
            ChatUtilsKt.devMessage("Found valid route length " + checkNode.getMoveCost() / NEW_NODE_COST, "§8§l-<§r§aNoob Routes§r§8§l>-§r ", null);

            if (!isComplete() || checkNode.getMoveCost() < getBestNodeMoveCost()) setBestNode(checkNode);
        }



        if (isComplete() && checkNode.getMoveCost() >= getBestNodeMoveCost()) {
            finishNode(checkNode);
            return;
        }
        for (BlockPos pos : findRaycastBlocks(checkNode.getPos())) {
            PathNode neighborNode = getNodeAt(pos, PathNode.hashCode(pos), checkNode);
            double newCost = checkNode.getMoveCost() + NEW_NODE_COST;

            if (neighborNode.getMoveCost() - newCost > MIN_IMPROVEMENT) {
                neighborNode.updateParent(checkNode);
                // If was in heap move it up
                if (neighborNode.isOpen()) {
                    updateNodes(neighborNode);
                } else {
                    // Else add it back
                    insertNodes(neighborNode);
                }
                if (!isComplete() && getBestNodeHeuristic() - neighborNode.getHeuristicCost() > MIN_IMPROVEMENT) {
                    if (neighborNode.getMoveCost() < getBestNodeMoveCost())
                        setBestNode(neighborNode);
                }

            }
        }
        finishNode(checkNode);
    }

    public static void outTime(long startTime) {
        ChatUtilsKt.devMessage("Took " + (System.currentTimeMillis() - startTime) + "ms", "§8§l-<§r§aNoob Routes§r§8§l>-§r ", null);
    }

    // wrong for now
    public static boolean isValidPos(BlockPos blockPos) {
        return getBlockState(blockPos).getBlock() != Blocks.air
                && getBlockState(blockPos.add(0, 1, 0)).getBlock() == Blocks.air
                && getBlockState(blockPos.add(0, 2, 0)).getBlock() == Blocks.air
        ;
    }

    public synchronized PathNode getNodeAt(BlockPos pos, long hashcode, PathNode parent) {
        PathNode node = cache.get(hashcode);
        if (node == null) {
            node = new PathNode(pos, parent, goal);
            cache.put(hashcode, node);
        }

        if (!node.getPos().equals(pos)) {
            System.err.println("HASHCODE ERROR!");
            System.err.println("Hash1 : " + hashcode + ", Hash2 " + node.hashCode() + " pos 1 : " + pos + " pos 2 : " + node.getPos());
        }

        return node;
    }

    public static IBlockState getBlockState(BlockPos blockPos) {
        if (Minecraft.getMinecraft().theWorld == null) return null;
        return Minecraft.getMinecraft().theWorld.getBlockState(blockPos);
    }

    public static List<BlockPos> findRaycastBlocks(BlockPos pos) {
        //long time = System.currentTimeMillis();

        List<BlockPos> blockHits = new ArrayList<>();
        HashSet<BlockPos> cache = new HashSet<>();

        Vec3 eyePos = VecUtilsKt.toCenteredVec3(pos).addVector(0.0, 2.539999957084656d, 0.0d);
        float stepPitch = 2f;
        float stepYaw = 2f;

        for (float pitch = -90f; pitch <= 90f; pitch += stepPitch) {
            float pitchRadians = (float) Math.toRadians(pitch);
            float yawStepAtThisPitch = stepYaw / Math.max(0.01f, (float) cos(pitchRadians)); // avoid div/0

            for (float yaw = 0f; yaw < 360f; yaw += yawStepAtThisPitch) {
                EtherWarpHelper.EtherPos etherPos = EtherWarpHelper.INSTANCE.getEtherPosFromOrigin(eyePos, yaw, pitch, 61, false);
                if (cache.add(etherPos.getPos()) && etherPos.getSucceeded()) {
                    blockHits.add(etherPos.getPos());
                }
            }
        }
        //outTime(time);
        return blockHits;
    }
}
