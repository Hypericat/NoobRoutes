package noobroutes.features.move

import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.Core
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.dungeon.autoroute.AutoRoute
import noobroutes.features.dungeon.autoroute.AutoRouteUtils.resetRotation
import noobroutes.features.dungeon.autoroute.DynNode
import noobroutes.features.dungeon.autoroute.Node
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.ColorSetting
import noobroutes.utils.Scheduler
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.render.Color
import noobroutes.utils.skyblock.*
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayerSq
import java.util.LinkedList
import kotlin.math.abs
import kotlin.math.absoluteValue

object DynamicRoute : Module("Dynamic Route", description = "Dynamic Etherwarp Routes.", category = Category.MOVE) {
    val silent by BooleanSetting("Silent", default = true, description = "Server side rotations")
    val renderRoutes by BooleanSetting("Render Routes", default = false, description = "Renders nodes")
    val renderIndex by BooleanSetting("Render Index", description = "Render index above node, useful for editing").withDependency { this.renderRoutes }
    val depth by BooleanSetting("Depth", default = true, description = "Render Rings Through Walls").withDependency { renderRoutes }
    val dynColor by ColorSetting("Dyn Node Color", default = Color.BLUE, description = "Color of dynamic nodes").withDependency { this.renderRoutes }
    private var editMode by BooleanSetting("Edit Mode", description = "Prevents nodes from triggering")


    private var lastRoute = 0L
    val routing get() = System.currentTimeMillis() - lastRoute < 200

    private var nodes : MutableList<DynNode> = LinkedList();
    private var deletedNodes = mutableListOf<Node>()


    @SubscribeEvent
    fun onRenderWorldLast(event: RenderWorldLastEvent) {
        if (mc.thePlayer == null) return
        if (!renderRoutes || nodes.isEmpty()) return
        nodes.forEach {
            it.render()
        }
        if (renderIndex) {
            nodes.forEachIndexed { index, node ->
                node.drawIndex(index)
            }
        }
    }


    private fun inNodes(): MutableList<DynNode> {
        val inNodes = mutableListOf<DynNode>()
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


    private fun getNode(args: Array<out String>): DynNode? {
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


    fun addNode(node: DynNode) {
        modMessage("Adding node!")
        nodes.add(node)
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onTick(event: ClientTickEvent) {
        if (event.isEnd || mc.thePlayer == null || editMode) return
        if (!AutoRoute.canRoute) return

        val node = inNodes().firstOrNull() ?: return

        this.lastRoute = System.currentTimeMillis()

        resetRotation()
        node.tick()
        //devMessage("runTick: ${System.currentTimeMillis()}")
        Scheduler.schedulePreMotionUpdateTask {
            node.motion((it as MotionUpdateEvent.Pre))
        //devMessage("motionUpdate: ${System.currentTimeMillis()}")
        }
    }

}