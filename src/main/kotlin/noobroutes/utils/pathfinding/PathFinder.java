package noobroutes.utils.pathfinding;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.init.Blocks;
import net.minecraft.util.BlockPos;
import net.minecraft.world.World;
import noobroutes.utils.pathfinding.openset.BinaryHeapOpenSet;
import noobroutes.utils.skyblock.ChatUtilsKt;

import java.util.HashMap;

public class PathFinder {
    private final Goal goal;
    private final World world;
    private final BlockPos startPos;
    private final HashMap<Long, PathNode> cache;
    public final static double NEW_NODE_COST = 10d;
    public final static double MIN_IMPROVEMENT = 1d;


    public PathFinder(World world, Goal goal, BlockPos startPos) {
        this.goal = goal;
        this.cache = new HashMap<>();
        this.world = world;
        this.startPos = startPos;
    }

    public Path calculate() {
        BinaryHeapOpenSet nodes = new BinaryHeapOpenSet();
        PathNode startNode = new PathNode(startPos, null, goal);
        startNode.setMoveCost(0d);
        nodes.insert(startNode);

        double bestHeuristic = startNode.getHeuristicCost();
        PathNode bestNode = startNode;

        PathNode recentNode;

        int nodeCount = 0;

        while (!nodes.isEmpty()) {
            PathNode checkNode = nodes.removeLowest();
            recentNode = checkNode;
            nodeCount++;

            if (goal.test(checkNode.getPos())) {
                ChatUtilsKt.devMessage("Found path! " + nodeCount + " attempts!", "§8§l-<§r§aNoob Routes§r§8§l>-§r ", null);
                ChatUtilsKt.devMessage("Found at : " + checkNode.getPos(), "§8§l-<§r§aNoob Routes§r§8§l>-§r ", null);

                Path path = new Path(startPos, startNode, checkNode, goal);

                //if (Minecraft.getMinecraft().isSingleplayer()) drawPath(path); // REMOVE THIS

                return path;
            }

            for (BlockPos pos : checkNode.getNear(this::isValidPos)) {
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
                    if (bestHeuristic - neighborNode.getHeuristicCost() > MIN_IMPROVEMENT) {
                        bestNode = neighborNode;
                    }

                }
            }
        }
        ChatUtilsKt.devMessage("Scanned all (" + nodeCount + ") nodes!", "§8§l-<§r§aNoob Routes§r§8§l>-§r ", null);
        ChatUtilsKt.devMessage("Returning best path!", "§8§l-<§r§aNoob Routes§r§8§l>-§r ", null);
        Path path = new Path(startPos, startNode, bestNode, goal);

        //if (Minecraft.getMinecraft().isSingleplayer()) drawPath(path); // REMOVE THIS

        return path;
    }

    // wrong for now
    public boolean isValidPos(BlockPos blockPos) {
        return getBlockState(blockPos).getBlock() == Blocks.air
                && getBlockState(blockPos.add(0, 1, 0)).getBlock() == Blocks.air
                //&& getBlockState(blockPos.add(0, -1, 0)).getBlock() != Blocks.air
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

    public IBlockState getBlockState(BlockPos blockPos) {
        return this.world.getBlockState(blockPos);
    }

    private static void drawPath(Path path) {
        PathNode node = path.getEndNode();

        while (node != null) {
            BlockPos pos = node.getPos();
            Minecraft.getMinecraft().thePlayer.sendChatMessage("/setblock " + pos.getX() + " " + pos.getY() + " " + pos.getZ() + " redstone_block");
            node = node.getParent();
        }
    }
}
