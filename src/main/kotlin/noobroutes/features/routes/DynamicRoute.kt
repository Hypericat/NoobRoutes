package noobroutes.features.routes

import net.minecraft.block.BlockColored
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraft.item.EnumDyeColor
import net.minecraft.util.Vec3
import net.minecraft.util.Vec3i
import net.minecraftforge.client.event.MouseEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.utils.routes.RouteUtils.lastRoute
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.ColorSetting
import noobroutes.utils.IBlockStateUtils.setProperty
import noobroutes.utils.Utils.containsOneOf
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.render.Color
import noobroutes.utils.skyblock.EtherWarpHelper
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayer
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayer2D
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayerSq
import noobroutes.utils.skyblock.modMessage
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.floor

object DynamicRoute : Module("Dynamic Route", description = "Dynamic Etherwarp Routes.", category = Category.ROUTES) {
    val silent by BooleanSetting("Silent", default = true, description = "Server side rotations")
    val renderRoutes by BooleanSetting("Render Routes", default = false, description = "Renders nodes")
    val renderIndex by BooleanSetting(
        "Render Index",
        description = "Render index above node, useful for editing"
    ).withDependency { this.renderRoutes }
    val depth by BooleanSetting("Depth", default = true, description = "Render Rings Through Walls").withDependency { renderRoutes }
    val fullBlock by BooleanSetting(
        "Place Full Blocks",
        default = true,
        description = "Make the blocks under the nodes full blocks."
    )

    val dynColor by ColorSetting(
        "Dyn Node Color",
        default = Color.Companion.BLUE,
        description = "Color of dynamic nodes"
    ).withDependency { this.renderRoutes }
    val editMode by BooleanSetting("Edit Mode", description = "Prevents nodes from triggering")
    val extraDebug by BooleanSetting("Warn Missing Item", description = "Get ts out of my face", default = true)


    private var nodes : MutableList<DynamicNode> = mutableListOf();
    private var deletedNodes : MutableList<DynamicNode> = mutableListOf();

    @Synchronized
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

    fun isInNode() : Boolean {
        return nodes.any {
            it.pos.distanceToPlayerSq <= 0.25
        }
    }


    @Synchronized
    private fun inNodes(): MutableList<DynamicNode> {
        val inNodes = mutableListOf<DynamicNode>()
        nodes.forEach { node ->
            val inNode = node.pos.distanceToPlayerSq <= 0.25
            if (inNode && !node.triggered) {
                node.triggered = true
                inNodes.add(node)
            } else if (!inNode && node.triggered) {
                node.reset()
            }
        }
        return inNodes
    }


    @Synchronized
    fun addNode(node: DynamicNode) {
        if (Minecraft.getMinecraft().theWorld != null && fullBlock) {
            node.setPrevState(
                Minecraft.getMinecraft().theWorld.getBlockState(node.getBlockPos().subtract(
                    Vec3i(
                        0,
                        1,
                        0
                    )
                )))

            Minecraft.getMinecraft().theWorld?.setBlockState(
                node.getBlockPos().subtract(Vec3i(0, 1, 0)),
                Blocks.stained_glass.defaultState.setProperty(BlockColored.COLOR.name, EnumDyeColor.BLACK)
            )
        }
        nodes.add(node)
    }

    @Synchronized
    @SubscribeEvent
    fun onUnloadWorldLoad(event: WorldEvent.Unload) {
        nodes.clear()
        deletedNodes.clear()
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.isEnd || mc.thePlayer == null || editMode || PlayerUtils.movementKeysPressed) return

        if (!AutoRoute.canRoute) return

        val node = inNodes().firstOrNull() ?: return

        lastRoute = System.currentTimeMillis()
        node.tick()

        if (node.singleUse) removeNode(node)
    }

    fun clearRoute() {
        while (nodes.isNotEmpty()) {
            removeNode(nodes.first())
        }
    }

    @Synchronized
    fun handleDynamicRouteCommand(args: Array<out String>) {
        val chain = args.containsOneOf("chain", ignoreCase = true)

        when (args[0].lowercase()) {
            "restore" -> {
                modMessage("Restoring node!")
                addNode(deletedNodes.removeLastOrNull() ?: return modMessage("No node to restore!"))
            }


            "delete", "remove", "begone", "eradicate", "flaccid" -> {
                val node = getNode(args) ?: return
                removeNode(node)
                modMessage("Removed ${deletedNodes.last().name}")
            }

            "edit" -> {
                val node = getNode(args) ?: return
                node.chain = chain
            }
            "clear" -> {
                while (nodes.isNotEmpty()) {
                    removeNode(nodes.first())
                }
                modMessage("Removed all dynamic nodes!")
            }

            "add", "create", "erect" -> {
                if (args.size < 2) {
                    modMessage("nodes: etherwarp")
                    return
                }
                val playerCoords = Vec3(
                    floor(PlayerUtils.posX) + 0.5,
                    floor(PlayerUtils.posY),
                    floor(PlayerUtils.posZ) + 0.5
                )
                when (args[1].lowercase()) {
                    "warp", "etherwarp", "etherwarp_target", "etherwarptarget", "ether", "ew" -> {
                        val raytrace = EtherWarpHelper.rayTraceBlock(200, 1f)
                        if (raytrace == null) {
                            modMessage("No Target Found")
                            return
                        }
                        modMessage("Adding node!")
                        addNode(DynamicNode(playerCoords, raytrace))
                    }

                    else -> {
                        modMessage("Usages: Add, Delete, Edit, Load")
                    }
                }
            }
        }
    }

    @Synchronized
    private fun removeNode(node: DynamicNode) {
        deletedNodes.add(node)
        nodes.remove(node)

        if (node.getPrevState() != null && Minecraft.getMinecraft().theWorld != null)
            Minecraft.getMinecraft().theWorld?.setBlockState(node.getBlockPos().subtract(Vec3i(0, 1, 0)), node.getPrevState())
    }


    @Synchronized
    @SubscribeEvent
    fun onKeyInput(event: MouseEvent) {
        if (event.button == 1 && event.buttonstate && System.currentTimeMillis() - lastRoute < 150) {
            val nodes = nodes.filter { node ->
                if (node.chain) (
                        abs(PlayerUtils.posX - node.pos.xCoord) < 0.001 &&
                                abs(PlayerUtils.posZ - node.pos.zCoord) < 0.001 &&
                                PlayerUtils.posY >= node.pos.yCoord - 0.01 && PlayerUtils.posY <= node.pos.yCoord + 0.5
                        ) else node.pos.distanceToPlayer <= 0.5
            }
            if (nodes.isNotEmpty()) event.isCanceled = true
        }
        if (event.button == 0 && event.buttonstate) {
            nodes.forEach { it.reset() }
            if (nodes.firstOrNull { node ->
                    (if (node.chain) (
                            abs(PlayerUtils.posX - node.pos.xCoord) < 0.001 &&
                                    abs(PlayerUtils.posZ - node.pos.zCoord) < 0.001 &&
                                    PlayerUtils.posY >= node.pos.yCoord - 0.01 && PlayerUtils.posY <= node.pos.yCoord + 0.5
                            ) else if (node.name == "Pearl") (
                            node.pos.distanceToPlayer2D <= 0.5
                            )
                    else node.pos.distanceToPlayer <= 0.5)
                } != null) event.isCanceled = true
        }
    }

    @Synchronized
    private fun getNode(args : Array<out String>): DynamicNode? {
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
}