package noobroutes.pathfinding;

import net.minecraft.client.Minecraft;
import net.minecraft.util.BlockPos;
import noobroutes.features.misc.EWPathfinderModule;
import noobroutes.utils.VecUtilsKt;

public class PathfinderExecutor {


    public static void run(BlockPos target, boolean perfect, float yawStep, float pitchStep, float ewCost, float heuristicThreshold, boolean singleUse) {
        //blocks = new ArrayList<>();
        //blocks.addAll(PathFinder.findRaycastBlocks(Minecraft.getMinecraft().thePlayer.getPosition(), PathFinder::isValidPos, 61, Minecraft.getMinecraft().theWorld));
        //if (true) return;
        PathFinder pathFinder = new PathFinder(new GoalXYZ(target), VecUtilsKt.toBlockPos(Minecraft.getMinecraft().thePlayer.getPositionVector().subtract(0, 1, 0), 0), ewCost, perfect, yawStep, pitchStep, heuristicThreshold);

        Thread thread = new Thread(() -> EWPathfinderModule.INSTANCE.onSolve(pathFinder.calculate(), singleUse));
        thread.start();
    }


}
