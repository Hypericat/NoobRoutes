package noobroutes.utils.pathfinding;

import net.minecraft.client.Minecraft;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.BlockPos;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import noobroutes.utils.VecUtilsKt;
import noobroutes.utils.render.Color;
import noobroutes.utils.render.Renderer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class PathfinderExecutor {
    private static Path lastPath;

    private static List<BlockPos> blocks;
    public static BlockPos currentBlock;

    public static void test(float x, float y, float z, float nodeCost) {
        //blocks = new ArrayList<>();
        //blocks.addAll(PathFinder.findRaycastBlocks(Minecraft.getMinecraft().thePlayer.getPosition(), PathFinder::isValidPos, 61, Minecraft.getMinecraft().theWorld));
        //if (true) return;
        PathFinder pathFinder = new PathFinder(new GoalXYZ(new BlockPos(x, y, z)), VecUtilsKt.toBlockPos(Minecraft.getMinecraft().thePlayer.getPositionVector().subtract(0, 1, 0), 0), nodeCost);

        Thread thread = new Thread(() -> PathfinderExecutor.lastPath = pathFinder.calculate());
        thread.start();
    }


    @SubscribeEvent
    public void onRender(RenderWorldLastEvent event) {
        //if (blocks == null) return;
        //blocks.forEach(blockPos -> {
        //    AxisAlignedBB aabb = new AxisAlignedBB(blockPos, blockPos.add(1, 1, 1));
        //    Renderer.INSTANCE.drawBox(aabb, Color.GREEN, 3, 1, 0, false, true);
        //});
        //if (true) return;

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
            Renderer.INSTANCE.drawBox(aabb, Color.GREEN, 3, 1, 0, false, true);
        }

        if (currentBlock != null) {
            AxisAlignedBB aabb = new AxisAlignedBB(currentBlock, currentBlock.add(1, 1, 1));
            Renderer.INSTANCE.drawBox(aabb, Color.RED, 3, 1, 0, false, true);
        }
    }
}
