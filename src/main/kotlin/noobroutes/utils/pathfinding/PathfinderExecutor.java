package noobroutes.utils.pathfinding;

import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import noobroutes.features.misc.EWPathfinderModule;
import noobroutes.utils.VecUtilsKt;
import noobroutes.utils.render.Color;
import noobroutes.utils.render.Renderer;

import java.util.List;

public class PathfinderExecutor {


    public static void run(float x, float y, float z, boolean perfect, float yawStep, float pitchStep, float ewCost) {
        //blocks = new ArrayList<>();
        //blocks.addAll(PathFinder.findRaycastBlocks(Minecraft.getMinecraft().thePlayer.getPosition(), PathFinder::isValidPos, 61, Minecraft.getMinecraft().theWorld));
        //if (true) return;
        PathFinder pathFinder = new PathFinder(new GoalXYZ(new BlockPos(x, y, z)), VecUtilsKt.toBlockPos(Minecraft.getMinecraft().thePlayer.getPositionVector().subtract(0, 1, 0), 0), ewCost, perfect, yawStep, pitchStep);

        Thread thread = new Thread(() -> EWPathfinderModule.INSTANCE.setLastPath(pathFinder.calculate()));
        thread.start();
    }

}
