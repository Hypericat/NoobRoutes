package noobroutes.pathfinding;

import com.mojang.realmsclient.util.Pair;
import net.minecraft.util.BlockPos;
import net.minecraft.util.Vec3;
import noobroutes.features.misc.EWPathfinderModule;
import noobroutes.utils.VecUtilsKt;
import noobroutes.pathfinding.openset.BinaryHeapOpenSet;
import noobroutes.utils.skyblock.ChatUtilsKt;
import noobroutes.utils.skyblock.EtherWarpHelper;

import java.util.*;

import static java.lang.Math.cos;

public class PathFinder {
    private final Goal goal;
    private final BlockPos startPos;
    private final HashMap<Long, PathNode> cache;
    public static double NEW_NODE_COST = 100d;
    public final static double MIN_IMPROVEMENT = 1d;
    public final static double THREAD_COUNT = 12;

    public static final float EYE_HEIGHT = 1.6200000047683716f;

    private final float yawStep;
    private final float pitchStep;
    private final float heuristicThreshold;
    private final boolean perfect;

    private PathNode bestNode;
    private CachedPath bestCachedPath;
    private boolean complete;
    private BinaryHeapOpenSet nodes;
    private HashSet<Integer> processing;


    public PathFinder(Goal goal, BlockPos startPos, double nodeCost, boolean perfect, float yawStep, float pitchStep, float heuristicThreshold) {
        this.goal = goal;
        this.cache = new HashMap<>();
        this.startPos = startPos;
        this.perfect = perfect;
        this.yawStep = yawStep;
        this.pitchStep = pitchStep;
        this.heuristicThreshold = heuristicThreshold;

        NEW_NODE_COST = nodeCost;
    }

    public Path calculate() {
        long time = System.currentTimeMillis();

        nodes = new BinaryHeapOpenSet();
        processing = new HashSet<>();
        PathNode startNode = new PathNode(startPos, null, goal);

        startNode.setYaw(Float.MAX_VALUE); // Important for recognition of new nodes, removing may lead to worse performance
        startNode.setPitch(Float.MAX_VALUE); // Important for recognition of new nodes, removing may lead to worse performance

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
                bestCachedPath = new CachedPath(node);
            }
            return;
        }
        if (goal.test(node.getPos())) {
            bestNode = node;
            bestCachedPath = new CachedPath(node);
            complete = true;
            return;
        }
        if (bestNode.getHeuristicCost() - node.getHeuristicCost() > MIN_IMPROVEMENT) {
            if (bestNode.getMoveCost() < getBestNodeMoveCost())
                this.bestNode = node;
        }

    }

    private synchronized double getBestHeuristicByIndex(int index) {
        PathNode node = bestCachedPath.getByIndex(index);
        return node == null ? Double.MAX_VALUE : node.getHeuristicCost();
    }

    public void checkNode(PathNode checkNode) {
        if (checkNode == null) return;
        EWPathfinderModule.INSTANCE.setBestHeuristic(checkNode.getHeuristicCost());
        EWPathfinderModule.INSTANCE.setCurrentBlock(checkNode.getPos());

        if (goal.test(checkNode.getPos())) {
            ChatUtilsKt.devMessage("Found valid route length " + checkNode.getIndex(), "§8§l-<§r§aNoob Routes§r§8§l>-§r ", null);

            if (!isComplete() || checkNode.getMoveCost() < getBestNodeMoveCost()) setBestNode(checkNode);
        }


        if (isComplete() && checkNode.getMoveCost() >= getBestNodeMoveCost()) {
            finishNode(checkNode);
            return;
        }

        if (!perfect && isComplete() && checkNode.getHeuristicCost() >= getBestHeuristicByIndex(checkNode.getIndex()) * heuristicThreshold) {
            finishNode(checkNode);
            return;
        }


        double newCost = checkNode.getMoveCost() + NEW_NODE_COST;

        for (Pair<PathNode, Float[]> pair : findRaycastBlocks(checkNode)) {
            PathNode neighborNode = pair.first();

                if (!neighborNode.hasBeenScanned() || neighborNode.getMoveCost() - newCost > MIN_IMPROVEMENT) {
                    neighborNode.updateParent(checkNode);
                    neighborNode.setYaw(pair.second()[0]);
                    neighborNode.setPitch(pair.second()[1]);
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

    public List<Pair<PathNode, Float[]>> findRaycastBlocks(PathNode parent) {
        //long time = System.currentTimeMillis();

        List<Pair<PathNode, Float[]>> blockHits = new ArrayList<>();
        HashSet<BlockPos> cache = new HashSet<>();

        Vec3 eyePos = VecUtilsKt.toCenteredVec3(parent.getPos()).addVector(0.0, 1 + EYE_HEIGHT, 0.0d);

        for (float pitch = -90f; pitch <= 90f; pitch += pitchStep) {
            float pitchRadians = (float) Math.toRadians(pitch);
            float yawStepAtThisPitch = yawStep / Math.max(0.01f, (float) cos(pitchRadians)); // avoid div/0

            for (float yaw = 0f; yaw < 360f; yaw += yawStepAtThisPitch) {
                EtherWarpHelper.EtherPos etherPos = EtherWarpHelper.INSTANCE.getEtherPosFromOrigin(eyePos, yaw, pitch, 61, false);
                if (etherPos.getPos() != null && cache.add(etherPos.getPos()) && etherPos.getSucceeded()) {
                    etherPos.getVec();
                    PathNode node = getNodeAt(etherPos.getPos(), PathNode.hashCode(etherPos.getPos()), parent);
                    blockHits.add(Pair.of(node, new Float[]{yaw, pitch}));
                }
            }
        }
        //outTime(time);
        return blockHits;
    }
}
