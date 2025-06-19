package noobroutes.features.routes.nodes

import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import noobroutes.utils.AutoP3Utils.walking
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.toBlockPos

abstract class Node(
    protected var pos: Vec3 = Vec3(0.0, 0.0, 0.0),
    ) {
    protected var triggered = false

    open fun render() {
        Renderer.drawCylinder(pos.add(Vec3(0.0, 0.03, 0.0)), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, getRenderColor(), getDepth())
    }

    open fun renderIndex(index: Int) {
        Renderer.drawStringInWorld(index.toString(), pos.add(Vec3(0.0, 0.3, 0.0)), getRenderColor(), getDepth())
    }

    abstract fun run()
    abstract fun getType() : NodeType
    abstract fun isSilent() : Boolean

    open fun getRenderColor(): Color {
        return Color.GREEN
    }

    open fun getDepth(): Boolean {
        return false;
    }

    protected fun stopWalk(){
        walking = false
        PlayerUtils.unPressKeys()
    }

    open fun reset(){
        triggered = false
    }

    fun getBlockPos() : BlockPos {
        return this.pos.toBlockPos()
    }

    abstract fun isInNode(pos: Vec3) : Boolean;





}