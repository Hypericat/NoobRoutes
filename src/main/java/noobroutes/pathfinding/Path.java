package noobroutes.pathfinding;

import com.mojang.realmsclient.util.Pair;
import net.minecraft.util.BlockPos;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Path {
    private final BlockPos start;
    private final PathNode startNode;
    private final PathNode endNode;
    private final Goal goal;

    public Path(BlockPos start, PathNode startNode, PathNode endNode, Goal goal) {
        this.start = start;
        this.startNode = startNode;
        this.endNode = endNode;
        this.goal = goal;
    }

    public BlockPos getStart() {
        return start;
    }

    public PathNode getStartNode() {
        return startNode;
    }

    public PathNode getEndNode() {
        return endNode;
    }

    public Goal getGoal() {
        return goal;
    }

}
