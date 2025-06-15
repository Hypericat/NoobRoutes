package noobroutes.features.move

import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.dungeon.autoroute.AutoRoute
import noobroutes.features.dungeon.autoroute.AutoRouteUtils.resetRotation
import noobroutes.features.dungeon.autoroute.Node
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.utils.skyblock.*
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayerSq
import java.util.LinkedList
import kotlin.math.abs
import kotlin.math.absoluteValue

object DynamicRoute : Module("Dynamic Route", description = "Dynamic Etherwarp Routes.", category = Category.MOVE) {
    val silent by BooleanSetting("Silent", default = true, description = "Server side rotations")
    val renderRoutes by BooleanSetting("Render Routes", default = false, description = "Renders nodes")
    val depth by BooleanSetting("Depth", default = true, description = "Render Rings Through Walls").withDependency { renderRoutes }

    private var lastRoute = 0L
    val routing get() = System.currentTimeMillis() - lastRoute < 200

    private var nodes : MutableList<Node> = LinkedList();


    private fun inNodes(): MutableList<Node> {
        val inNodes = mutableListOf<Node>()
        nodes.forEach { node ->
            val inNode =
                if (node.chain) (
                        abs(PlayerUtils.posX - node.pos.xCoord) < 0.001 &&
                                abs(PlayerUtils.posZ - node.pos.zCoord) < 0.001 &&
                                PlayerUtils.posY >= node.pos.yCoord - 0.01 && PlayerUtils.posY <= node.pos.yCoord + 0.5
                        )
                else node.pos.distanceToPlayerSq <= 0.25
            if (inNode && !node.triggered) {
                node.triggered = true
                inNodes.add(node)
            } else if (!inNode && node.triggered) {
                node.reset()
            }
        }
        return inNodes
    }


    private fun getNode(args: Array<out String>): Node? {
        if (args.size > 1) {
            val index = args[1].toIntOrNull()?.absoluteValue
            if (index == null) {
                modMessage("Provide a number for index")
                return null
            }
            if (nodes.isEmpty() || index !in nodes.indices) {
                modMessage("No node with index: $index")
                return null
            }
            return nodes[index]
        }
        if (nodes.isEmpty()) return null

        val node = nodes.minByOrNull {
            it.pos.distanceToPlayerSq
        }!!
        return node
    }


    fun addNode(node: Node) {
        modMessage("Adding node!")
        nodes.add(node)
    }

}