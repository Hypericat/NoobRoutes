package noobroutes.pathfinding;

import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import noobroutes.features.misc.EWPathfinderModule;
import noobroutes.utils.VecUtilsKt;

public class PathfinderExecutor {
    public static void run(BlockPos target, boolean perfect, float yawStep, float pitchStep, float ewCost, float heuristicThreshold, boolean singleUse) {
        run(target, perfect, yawStep, pitchStep, ewCost, heuristicThreshold, singleUse, null);
    }

    public static void run(BlockPos target, boolean perfect, float yawStep, float pitchStep, float ewCost, float heuristicThreshold, boolean singleUse, Runnable runnable) {
        PathFinder pathFinder = new PathFinder(new GoalXYZ(target), VecUtilsKt.toBlockPos(Minecraft.getMinecraft().thePlayer.getPositionVector().subtract(0, 1, 0), 0), ewCost, perfect, yawStep, pitchStep, heuristicThreshold);

        Thread thread = new Thread(() -> {
            EWPathfinderModule.INSTANCE.onSolve(pathFinder.calculate(), singleUse);
            if (runnable != null)
                runnable.run();
        });
        thread.start();
    }
}
