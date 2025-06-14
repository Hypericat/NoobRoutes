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

import java.util.*;
import java.util.function.Predicate;

import static java.lang.Math.cos;
import static java.lang.Math.sin;

public class PathFinder {
    private final Goal goal;
    private final World world;
    private final BlockPos startPos;
    private final HashMap<Long, PathNode> cache;
    public final static double NEW_NODE_COST = 2d;
    public final static double MIN_IMPROVEMENT = 1d;


    public PathFinder(World world, Goal goal, BlockPos startPos) {
        this.goal = goal;
        this.cache = new HashMap<>();
        this.world = world;
        this.startPos = startPos;
    }

    public Path calculate() {
        long time = System.currentTimeMillis();

        BinaryHeapOpenSet nodes = new BinaryHeapOpenSet();
        PathNode startNode = new PathNode(startPos, null, goal);
        startNode.setMoveCost(0d);
        nodes.insert(startNode);

        PathNode bestNode = startNode;
        boolean complete = false;

        int nodeCount = 0;

        while (!nodes.isEmpty()) {
            PathNode checkNode = nodes.removeLowest();
            nodeCount++;

            PathfinderExecutor.currentBlock = checkNode.getPos(); // TEMP

            if (goal.test(checkNode.getPos())) {
                ChatUtilsKt.devMessage("Found path! " + nodeCount + " attempts!", "§8§l-<§r§aNoob Routes§r§8§l>-§r ", null);
                ChatUtilsKt.devMessage("Found at : " + checkNode.getPos(), "§8§l-<§r§aNoob Routes§r§8§l>-§r ", null);
                outTime(time);

                if (!complete || checkNode.getMoveCost() < bestNode.getMoveCost()) bestNode = checkNode;
                complete = true;

                //Path path = new Path(startPos, startNode, checkNode, goal);

                ////if (Minecraft.getMinecraft().isSingleplayer()) drawPath(path); // REMOVE THIS

                //return path;
            }

            if (complete && checkNode.getMoveCost() > bestNode.getMoveCost()) continue;

            for (BlockPos pos : findRaycastBlocks(checkNode.getPos(), PathFinder::isValidPos, 61, world)) {
                PathNode neighborNode = getNodeAt(pos, pos.hashCode(), checkNode);
                double newCost = checkNode.getMoveCost() + NEW_NODE_COST;

                if (neighborNode.getMoveCost() - newCost > MIN_IMPROVEMENT) {
                    neighborNode.updateParent(checkNode);
                    // If was in heap move it up
                    if (neighborNode.isOpen()) {
                        nodes.update(neighborNode);
                    } else {
                        // Else add it back
                        nodes.insert(neighborNode);
                    }
                    if (!complete && bestNode.getHeuristicCost() - neighborNode.getHeuristicCost() > MIN_IMPROVEMENT) {
                        if (neighborNode.getMoveCost() < bestNode.getMoveCost())
                            bestNode = neighborNode;
                    }

                }
            }
        }
        ChatUtilsKt.devMessage("Scanned all (" + nodeCount + ") nodes!", "§8§l-<§r§aNoob Routes§r§8§l>-§r ", null);
        ChatUtilsKt.devMessage("Returning best path!", "§8§l-<§r§aNoob Routes§r§8§l>-§r ", null);
        outTime(time);
        Path path = new Path(startPos, startNode, bestNode, goal);

        //if (Minecraft.getMinecraft().isSingleplayer()) drawPath(path); // REMOVE THIS

        return path;
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

    public PathNode getNodeAt(BlockPos pos, long hashcode, PathNode parent) {
        PathNode node = cache.get(hashcode);
        if (node == null) {
            node = new PathNode(pos, parent, goal);
            cache.put(hashcode, node);
        }
        return node;
    }

    public static IBlockState getBlockState(BlockPos blockPos) {
        if (Minecraft.getMinecraft().theWorld == null) return null;
        return Minecraft.getMinecraft().theWorld.getBlockState(blockPos);
    }

    public static List<BlockPos> findRaycastBlocks(BlockPos pos, Predicate<BlockPos> filter, int maxDist, World world) {
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
                Vec3 dir = RotationUtils.INSTANCE.yawAndPitchVector(yaw, pitch);
                Vec3 end = eyePos.addVector(dir.xCoord * maxDist, dir.yCoord * maxDist, dir.zCoord * maxDist);

                MovingObjectPosition res = world.rayTraceBlocks(eyePos, end, false, true, false);
                if (res == null || res.typeOfHit != MovingObjectPosition.MovingObjectType.BLOCK) continue;

                BlockPos rayPos = res.getBlockPos();
                if (cache.add(rayPos) && filter.test(rayPos)) {
                    blockHits.add(rayPos);
                }
            }
        }
        //outTime(time);
        return blockHits;
    }
}
