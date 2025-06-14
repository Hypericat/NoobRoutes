package noobroutes.utils.pathfinding;

import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import noobroutes.utils.render.Color;
import noobroutes.utils.render.Renderer;

public class PathfinderExecutor {
    private static Path lastPath;

    public static void test(float x, float y, float z) {
        PathFinder pathFinder = new PathFinder(Minecraft.getMinecraft().theWorld, new GoalXYZ(new BlockPos(x, y, z)), Minecraft.getMinecraft().thePlayer.getPosition());

        Thread thread = new Thread(() -> PathfinderExecutor.lastPath = pathFinder.calculate());
        thread.start();
    }


    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        if (lastPath == null) return;

        PathNode last = lastPath.getEndNode();
        while (last != null) {
            BlockPos pos = last.getPos();
            last = last.getParent();
            //AxisAlignedBB aabb = new AxisAlignedBB(
            //        pos.getX() - 0.03, pos.getY(), pos.getZ() - 0.03,
            //        pos.getX() + 0.03, pos.getY() + 0.06, pos.getZ() + 0.03
            //);
            AxisAlignedBB aabb = new AxisAlignedBB(pos, pos.add(1, 1, 1));
            Renderer.INSTANCE.drawBox(aabb, Color.GREEN, 3, 1, 0, true, true);
        }
    }
}
