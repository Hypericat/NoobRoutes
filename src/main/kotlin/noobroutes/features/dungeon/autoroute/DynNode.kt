package noobroutes.features.dungeon.autoroute

import net.minecraft.block.state.IBlockState
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noobroutes.Core.mc
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.features.dungeon.autoroute.AutoRouteUtils.ether
import noobroutes.features.move.DynamicRoute
import noobroutes.utils.*
import noobroutes.utils.AutoP3Utils.walking
import noobroutes.utils.RotationUtils.offset
import noobroutes.utils.RotationUtils.setAngles
import noobroutes.utils.Utils.xPart
import noobroutes.utils.Utils.zPart
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.skyblock.skyblockID
import kotlin.math.absoluteValue

class DynNode(
    var pos: Vec3 = Vec3(0.0, 0.0, 0.0),
    var target: Vec3 = Vec3(0.0, 0.0, 0.0),
    val name: String = "DynNode",
    var chain: Boolean = false,
    val singleUse: Boolean = false
) {

    private var prevState: IBlockState? = null;


    var triggered = false

    fun reset() {
        triggered = false
    }

    fun setPrevState(state: IBlockState) {
        this.prevState = state
    }

    fun getPrevState() : IBlockState? {
        return this.prevState;
    }

    fun getBlockPos() : BlockPos {
        return this.pos.toBlockPos()
    }


    fun tick() {
        val angles = RotationUtils.getYawAndPitch(target)
        val state = SwapManager.swapFromSBId(DynamicRoute.extraDebug, "ASPECT_OF_THE_VOID")
        if (state == SwapManager.SwapState.UNKNOWN) return
        if (state == SwapManager.SwapState.TOO_FAST) {
            modMessage("Tried to 0 tick swap gg")
            return
        }
        if (!DynamicRoute.silent) setAngles(angles.first, angles.second)
        stopWalk()
        PlayerUtils.sneak()
    }


    fun motion(event: MotionUpdateEvent.Pre) {
        val angles = RotationUtils.getYawAndPitch(target)
        event.yaw = angles.first
        event.pitch = angles.second
        if (!mc.thePlayer.isSneaking || mc.thePlayer.heldItem.skyblockID != "ASPECT_OF_THE_VOID") {
            AutoRouteUtils.setRotation(angles.first + offset, angles.second)
            Scheduler.schedulePreTickTask {
                ether()
            }
            return
        }
        ether()
    }

    fun stopWalk(){
        walking = false
        PlayerUtils.unPressKeys()
    }

    fun drawNode(color: Color) {
        Renderer.drawCylinder(pos.add(Vec3(0.0, 0.03, 0.0)), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, color, depth = DynamicRoute.depth)
    }

    fun drawIndex(index: Int) {
        Renderer.drawStringInWorld(index.toString(), pos.add(Vec3(0.0, 0.3, 0.0)), renderIndexColor(), depth = DynamicRoute.depth)
    }


    fun render() {
        drawNode(DynamicRoute.dynColor)
        if (!AutoRoute.drawEtherLines) return
        val lookVec = RotationUtils.getYawAndPitchOrigin(pos, target)
        if (lookVec.second.absoluteValue != 90f) {
            Renderer.draw3DLine(
                listOf(
                    pos.add(lookVec.first.xPart * 0.6, 0.0, lookVec.first.zPart * 0.6),
                    target,
                ),
                DynamicRoute.dynColor,
                depth = DynamicRoute.depth
            )
        }
    }

    fun renderIndexColor(): Color {
        return DynamicRoute.dynColor
    }

}