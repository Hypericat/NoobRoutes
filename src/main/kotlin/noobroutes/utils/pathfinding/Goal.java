package noobroutes.utils.pathfinding;

import net.minecraft.util.BlockPos;

public interface Goal {

    boolean test(int x, int y, int z);

    default boolean test(BlockPos pos) {
        return test(pos.getX(), pos.getY(), pos.getZ());
    }

    double heuristic(int x, int y, int z);

    default double heuristic(BlockPos pos) {
        return heuristic(pos.getX(), pos.getY(), pos.getZ());
    }





}
