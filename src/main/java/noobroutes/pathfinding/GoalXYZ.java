package noobroutes.pathfinding;

import net.minecraft.util.BlockPos;

public class GoalXYZ implements Goal {
    private final BlockPos endPos;

    public GoalXYZ(BlockPos endPos) {
        this.endPos = endPos;
    }

    @Override
    public boolean test(int x, int y, int z) {
        return x == endPos.getX() && y == endPos.getY() && z == endPos.getZ();
    }

    @Override
    public double heuristic(int x, int y, int z) {
        int xDif = x - endPos.getX();
        int yDif = y - endPos.getY();
        int zDif = z - endPos.getZ();

        return xDif * xDif + yDif * yDif + zDif * zDif;
    }
}
