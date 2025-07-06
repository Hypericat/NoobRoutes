package noobroutes.features.routes.nodes

import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.util.Vec3
import noobroutes.features.routes.DynamicRoute
import noobroutes.utils.RotationUtils
import noobroutes.utils.SwapManager
import noobroutes.utils.Utils.xPart
import noobroutes.utils.Utils.zPart
import noobroutes.utils.add
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.routes.RouteUtils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.modMessage
import kotlin.math.absoluteValue

class DynamicNode(
    pos: Vec3,
    var target: Vec3,
    val singleUse: Boolean = false
) : Node(
    pos
){


    private var prevState: IBlockState? = null;

    override fun run() {
        val angles = RotationUtils.getYawAndPitch(target, true)
        var state = SwapManager.swapFromSBId(DynamicRoute.extraDebug, "ASPECT_OF_THE_VOID")
        if (state == SwapManager.SwapState.UNKNOWN && Minecraft.getMinecraft().isSingleplayer) state = SwapManager.swapFromId(
            Item.getIdFromItem(Items.diamond_shovel))
        if (state == SwapManager.SwapState.UNKNOWN) return
        if (state == SwapManager.SwapState.TOO_FAST) {
            modMessage("Tried to 0 tick swap gg")
            return
        }
        RouteUtils.lastRoute = System.currentTimeMillis()
        RouteUtils.setRotation(angles.first,angles.second, isSilent())
        stopWalk()
        PlayerUtils.unSneak()
        RouteUtils.ether()
    }

    fun setPrevState(state: IBlockState) {
        this.prevState = state
    }

    fun getPrevState() : IBlockState? {
        return this.prevState;
    }

    override fun isSilent() : Boolean {
        return DynamicRoute.silent
    }

    override fun getType(): NodeType {
        return NodeType.DYNAMIC
    }

    override fun getDepth(): Boolean {
        return DynamicRoute.depth
    }

    override fun getDistanceSq(pos: Vec3): Double {
        return this.pos.squareDistanceTo(pos)
    }

    override fun getRenderColor(): Color {
        return DynamicRoute.dynColor
    }

    override fun render() {
        super.render()
        val lookVec = RotationUtils.getYawAndPitchOrigin(pos, target, true)
        if (lookVec.second.absoluteValue != 90f) {
            Renderer.draw3DLine(
                listOf(
                    pos.add(lookVec.first.xPart * 0.6, 0.0, lookVec.first.zPart * 0.6),
                    target,
                ),
                this.getRenderColor(),
                depth = this.getDepth()
            )
        }
    }
}