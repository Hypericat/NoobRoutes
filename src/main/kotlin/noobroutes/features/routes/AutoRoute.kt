package noobroutes.features.routes

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos
import net.minecraftforge.client.event.MouseEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.Core
import noobroutes.config.DataManager
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.RoomEnterEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.routes.autobloodrush.AutoBloodRush
import noobroutes.features.routes.nodes.AutorouteNode
import noobroutes.features.routes.nodes.NodeType
import noobroutes.features.routes.nodes.autoroutes.Boom
import noobroutes.features.routes.nodes.autoroutes.Etherwarp
import noobroutes.features.routes.nodes.autoroutes.NodeLoader
import noobroutes.features.routes.nodes.autoroutes.Pearl
import noobroutes.features.routes.nodes.autoroutes.PearlClip
import noobroutes.features.routes.nodes.autoroutes.UseItem
import noobroutes.features.routes.nodes.autoroutes.Walk
import noobroutes.utils.routes.RouteUtils
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.ColorSetting
import noobroutes.features.settings.impl.DropdownSetting
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.utils.Scheduler
import noobroutes.utils.Utils.containsOneOf
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.coerceMax
import noobroutes.utils.equalsOneOf
import noobroutes.utils.floor
import noobroutes.utils.isAir
import noobroutes.utils.render.Color
import noobroutes.utils.round
import noobroutes.utils.routes.SecretUtils
import noobroutes.utils.skyblock.EtherWarpHelper
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayer
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayerSq
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeYaw
import noobroutes.utils.skyblock.dungeon.RoomData
import noobroutes.utils.skyblock.dungeon.tiles.Room
import noobroutes.utils.skyblock.dungeon.tiles.RoomType
import noobroutes.utils.skyblock.dungeon.tiles.Rotations
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import noobroutes.utils.skyblock.modMessage
import org.lwjgl.input.Keyboard
import kotlin.collections.iterator
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.ceil

/**
 * Modified from AK47 and MeowClient
 */
object AutoRoute : Module("Autoroute", description = "Ak47 modified", category = Category.ROUTES) {
    val silent by BooleanSetting("Silent", default = true, description = "Server side rotations")
    val decrease by BooleanSetting("Reduce Pearlclip", default = false, description = "When creating PearlClips it decreases the distance input by 1")
    val renderRoutes by BooleanSetting("Render Routes", default = false, description = "Renders nodes")
    val renderIndex by BooleanSetting("Render Index", description = "Render index above node, useful for editing").withDependency { renderRoutes }
    val edgeRoutes by BooleanSetting("Edging", default = false, description = "Edges nodes :)").withDependency { renderRoutes }
    val depth by BooleanSetting("Depth", default = true, description = "Render Rings Through Walls").withDependency { renderRoutes }
    private val drawSettings by DropdownSetting("Draw").withDependency { renderRoutes }
    val drawEtherLines by BooleanSetting("Draw Ether Lines", default = true, description = "Draws a line to the ether target").withDependency { renderRoutes && drawSettings }
    val drawAotvLines by BooleanSetting("Draw Aotv Lines", default = true, description = "Draws a line to the Aotv target").withDependency { renderRoutes && drawSettings }
    val drawPearlClipText by BooleanSetting("Draw Pearlclip Distance", default = true, description = "Renders the distance of the Pearlclip above the node").withDependency { renderRoutes && drawSettings }
    val drawPearlCountText by BooleanSetting("Draw Pearl Count", default = true, description = "Renders the pearl count above the Pearl node").withDependency { renderRoutes && drawSettings }
    private val colorSettings by DropdownSetting("Colors").withDependency { renderRoutes }
    val etherwarpColor by ColorSetting("Etherwarp", default = Color.Companion.GREEN, description = "Color of Etherwarp nodes").withDependency { renderRoutes && colorSettings }
    val walkColor by ColorSetting("Walk", default = Color.Companion.GREEN, description = "Color of Walk nodes").withDependency { renderRoutes && colorSettings }
    val pearlClipColor by ColorSetting("Pearlclip", default = Color.Companion.GREEN, description = "Color of Pearlclip nodes").withDependency { renderRoutes && colorSettings }
    val aotvColor by ColorSetting("Aotv", default = Color.Companion.GREEN, description = "Color of Aotv nodes").withDependency { renderRoutes && colorSettings }
    val boomColor by ColorSetting("Boom", default = Color.Companion.GREEN, description = "Color of Boom nodes").withDependency { renderRoutes && colorSettings }
    val batColor by ColorSetting("Bat", default = Color.Companion.GREEN, description = "Color of Bat nodes").withDependency { renderRoutes && colorSettings }
    val pearlColor by ColorSetting("Pearl", default = Color.Companion.GREEN, description = "Color of Pearl nodes").withDependency { renderRoutes && colorSettings }
    val useItemColor by ColorSetting("Use Item", default = Color.Companion.GREEN, description = "Color of Use Item nodes").withDependency { renderRoutes && colorSettings }
    private var editMode by BooleanSetting("Edit Mode", description = "Prevents nodes from triggering")
    val editModeBind by KeybindSetting("Edit Mode Toggle", Keyboard.KEY_NONE, description = "Toggles Edit Mode").onPress { editMode = !editMode }
    val placewarp by KeybindSetting("warp", Keyboard.KEY_NONE, description = "Toggles Edit Mode").onPress {
        handleAutoRouteCommand(arrayOf("add", "ew"))
    }

    val roomReplacement
        get() = UniqueRoom(0, 0, Room(0, 0, RoomData(LocationUtils.currentArea.name, RoomType.NORMAL, listOf(), 0, 0))).apply { rotation =
            Rotations.NORTH
        }


    var lastRoute = 0L
    val routing get() = System.currentTimeMillis() - lastRoute < 200

    private var nodes = mutableMapOf<String, MutableList<AutorouteNode>>()

    @SubscribeEvent
    fun onKeyInput(event: MouseEvent) {
        if (event.button == 1 && event.buttonstate && System.currentTimeMillis() - lastRoute < 150) {
            val room = DungeonUtils.currentRoom ?: roomReplacement
            val nodes = nodes[room.name]?.filter { node ->
                val realCoord = room.getRealCoords(node.pos)
                if (node.chain) (
                                abs(PlayerUtils.posX - realCoord.xCoord) < 0.001 &&
                                abs(PlayerUtils.posZ - realCoord.zCoord) < 0.001 &&
                                PlayerUtils.posY >= node.pos.yCoord - 0.01 && PlayerUtils.posY <= node.pos.yCoord + 0.5
                        ) else realCoord.distanceToPlayer <= 0.5
            }
            if (nodes?.isNotEmpty() == true) event.isCanceled = true
        }
        if (event.button == 0 && event.buttonstate) {
            if (SecretUtils.secretCount < 0) {
                SecretUtils.secretCount = 0;
                event.isCanceled = true
                return
            }
            if (RouteUtils.pearls > 0) {
                RouteUtils.pearls = 0
                event.isCanceled = true
                return
            }
            val room = DungeonUtils.currentRoom ?: roomReplacement
            nodes[room.name]?.forEach { it.reset() }
            if (nodes[room.name]?.firstOrNull {node ->
                val realCoord = room.getRealCoords(node.pos)
                    (if (node.chain) (
                            abs(PlayerUtils.posX - realCoord.xCoord) < 0.001 &&
                                    abs(PlayerUtils.posZ - realCoord.zCoord) < 0.001 &&
                                    PlayerUtils.posY >= node.pos.yCoord - 0.01 && PlayerUtils.posY <= node.pos.yCoord + 0.5
                    ) else realCoord.distanceToPlayer <= 0.5)
                } != null) event.isCanceled = true
        }
    }






    fun loadFile() {
        nodes.clear()
        val file = DataManager.loadDataFromFileObject("autoroutes")
        try {
            if (file.containsKey("Routes")) {
                nodes = RouteUtils.meowConverter(file)
                return
            }
            for ((key, arr) in file) {
                devMessage(key)
                val roomAutorouteNodes = mutableListOf<AutorouteNode>()
                arr.forEach {
                    val obj = it.asJsonObject
                    val name = obj.get("name")?.asString ?: return@forEach
                    val node = NodeType.getFromName(name)
                    roomAutorouteNodes.add(((node?.clazz ?: return@forEach) as NodeLoader).loadNodeInfo(obj))
                }
                nodes[key] = roomAutorouteNodes
            }
        } catch (e: Exception) {
            modMessage("Error Loading Rings")
            Core.logger.error("error loading rings", e)
        }

    }

    fun saveFile() {
        val jsonObj = JsonObject()
        nodes.forEach { route ->
            val nodeArray = JsonArray().apply {
                for (node in route.value) {
                    add(node.getAsJsonObject())
                }
            }
            jsonObj.add(route.key, nodeArray)
        }
        DataManager.saveDataToFile("autoroutes", jsonObj)
    }



    @SubscribeEvent
    fun onRenderWorldLast(event: RenderWorldLastEvent) {
        if (mc.thePlayer == null) return
        val room = DungeonUtils.currentRoom ?: roomReplacement
        if (!renderRoutes || nodes[room.name] == null) return
        nodes[room.name]?.forEach {
            it.render()
        }
        if (renderIndex) {
            nodes[room.name]?.forEachIndexed { index, node ->
                node.renderIndex(index)
            }
        }
    }


    private var nodesToRun = mutableListOf<AutorouteNode>()

    private fun inNodes(room: UniqueRoom): MutableList<AutorouteNode> {
        val inAutorouteNodes = mutableListOf<AutorouteNode>()
        nodes[room.name]?.forEach { node ->
            val realCoord = room.getRealCoords(node.pos)
            val inNode =
                if (node.chain) (
                    abs(PlayerUtils.posX - realCoord.xCoord) < 0.001 &&
                            abs(PlayerUtils.posZ - realCoord.zCoord) < 0.001 &&
                            PlayerUtils.posY >= node.pos.yCoord - 0.01 && PlayerUtils.posY <= node.pos.yCoord + 0.5
                    )
                else realCoord.distanceToPlayer <= 0.5
            if (inNode && !node.triggered) {
                node.triggered = true
                inAutorouteNodes.add(node)
            } else if (!inNode && node.triggered) {
                node.reset()
            }
        }
        return inAutorouteNodes
    }
    var delay = 0L

    val canRoute get() = (System.currentTimeMillis() - delay >= 0) && PlayerUtils.canSendC08 && RouteUtils.pearls < 1

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.isEnd || mc.thePlayer == null) return
        val room = DungeonUtils.currentRoom ?: roomReplacement
        if (PlayerUtils.movementKeysPressed) {
            nodesToRun.clear()
            return
        }
        if (!canRoute || nodes[room.name] == null || editMode) return

        val inNodes = inNodes(room)
        if (inNodes.isNotEmpty()) {
            inNodes.sortWith(compareByDescending { it.priority })
            if (inNodes.firstOrNull()?.getType()?.displayName == "Boom") {
                inNodes.coerceMax(2)
            } else inNodes.coerceMax(1)
            nodesToRun = inNodes
        }


        if (nodesToRun.isEmpty()) {
            if (SecretUtils.secretCount < 0) SecretUtils.secretCount = 0
            return
        }

        if (DynamicRoute.enabled && !DynamicRoute.editMode && DynamicRoute.isInNode()) return
        if (AutoBloodRush.waiting || AutoBloodRush.routeTo != null) return

        nodesToRun.firstOrNull()?.let { node ->
            RouteUtils.lastRoute = System.currentTimeMillis()
            lastRoute = System.currentTimeMillis()
            if (node.reset && !node.resetTriggered) {
                SecretUtils.secretCount = 0
                node.resetTriggered = true
            }
            val realCoord = room.getRealCoords(node.pos)
            if ((!if (node.chain) (
                abs(PlayerUtils.posX - realCoord.xCoord) < 0.001 &&
                        abs(PlayerUtils.posZ - realCoord.zCoord) < 0.001 &&
                        PlayerUtils.posY >= node.pos.yCoord - 0.01 && PlayerUtils.posY <= node.pos.yCoord + 0.5
                ) else realCoord.distanceToPlayer <= 0.5)) {
                nodesToRun.remove(node)
                return
            }

            if (!node.secretTriggered) {
                if (SecretUtils.secretCount >= 0) SecretUtils.secretCount - node.awaitSecrets else SecretUtils.secretCount = -node.awaitSecrets
                node.secretTriggered = true
            }
            if (SecretUtils.secretCount < 0) {
                node.updateTick()
                return
            }
            if (node.delay != 0L && !node.delayTriggered) {
                delay = System.currentTimeMillis() + node.delay
                node.delayTriggered = true
                return
            }

            if (node.stop) PlayerUtils.stopVelocity()
            if (node.center && !node.centerTriggered) {
                center()
                node.centerTriggered = true
                return //makes it wait a tick before executing the rest
            }
            node.secretTriggered = false
            SecretUtils.secretCount = 0
            node.run()
            nodesToRun.remove(node)
        }
    }




    fun center(){
        if (Core.mc.thePlayer.posZ < 0 || Core.mc.thePlayer.posZ > 0) Core.mc.thePlayer.setPosition(
            calcFloorPos(Core.mc.thePlayer.posX, 5.0),
            Core.mc.thePlayer.posY,
            calcFloorPos(Core.mc.thePlayer.posZ, 5.0)
        )
    }

    private fun calcFloorPos(c: Double, v: Double): Double{
        return if (c < 0) {
            ceil(c) - v / 10
        } else {
            return kotlin.math.floor(c) + v / 10
        }
    }


    private var deletedAutorouteNodes = mutableListOf<AutorouteNode>()

    fun handleAutoRouteCommand(args: Array<out String>) {
        val room = DungeonUtils.currentRoom ?: roomReplacement
        val reset = args.containsOneOf("reset", ignoreCase = true)
        val center = args.containsOneOf("center", "align", ignoreCase = true)
        val chain = args.containsOneOf("chain", ignoreCase = true)
        val maybeSecret = args.containsOneOf("maybe", "maybesecret", ignoreCase = true)
        val stop = args.containsOneOf("stop", ignoreCase = true)
        val awaitSecrets =
            args.firstOrNull { it.startsWith("await:", ignoreCase = true) }?.substringAfter("await:")?.toIntOrNull()
                ?: 0
        val delay =
            args.firstOrNull { it.startsWith("delay:", ignoreCase = true) }?.substringAfter("delay:")?.toLongOrNull()
                ?: 0L

        when (args[0].lowercase()) {
            "load" -> {
                loadFile()
            }
            "restore" -> {
                addNode(room, deletedAutorouteNodes.removeFirstOrNull() ?: return modMessage("no node to restore"))
                saveFile()
            }


            "delete", "remove", "begone", "eradicate", "flaccid" -> {
                val node = getNode(room, args) ?: return
                deletedAutorouteNodes.add(node)
                nodes[room.name]?.remove(node)
                saveFile()
                modMessage("Removed ${deletedAutorouteNodes.last().getType().displayName}")
            }

            "edit" -> {
                val node = getNode(room, args) ?: return
                node.awaitSecrets = awaitSecrets
                node.reset = reset
                node.center = center
                node.delay = delay
                node.stop = stop
                node.chain = chain
                saveFile()
            }
            "clear" -> {
                nodes[room.name] = mutableListOf()
                saveFile()
            }

            "add", "create", "erect" -> {
                if (args.size < 2) {
                    modMessage("nodes: etherwarp, walk, pearlclip, aotv")
                    return
                }
                when (args[1].lowercase()) {
                    "warp", "etherwarp", "etherwarp_target", "etherwarptarget", "ether", "ew" -> {
                        addNode(room, (Etherwarp::class as NodeLoader).generateFromArgs(args, room) ?: return)
                    }
                    "boom", "tnt" -> {

                        val block = mc.objectMouseOver.blockPos
                        if (isAir(block)) {
                            modMessage("must look at a block")
                            return
                        }
                        addNode(room, (Boom::class as NodeLoader).generateFromArgs(args, room) ?: return)
                    }

                    "walk" -> {
                        addNode(room, (Walk::class as NodeLoader).generateFromArgs(args, room) ?: return)
                    }

                    "pearlclip" -> {
                        if (args.size < 3) {
                            modMessage("Need Distance")
                        }
                        val distance = args[2].toIntOrNull()?.absoluteValue
                        if (distance == null) {
                            modMessage("Provide a Number thanks")
                            return
                        }
                        addNode(room, (PearlClip::class as NodeLoader).generateFromArgs(args, room) ?: return)
                    }

                    "pearl" -> {
                        if (args.size < 3) {
                            modMessage("Need Amount")
                        }
                        val count = args[2].toIntOrNull()?.absoluteValue
                        if (count == null) {
                            modMessage("Provide a Number thanks")
                            return
                        }
                        addNode(room, (Pearl::class as NodeLoader).generateFromArgs(args, room) ?: return)

                    }
                    "useitem" -> {
                        if (args.size < 3) {
                            modMessage("Need Item Name")
                        }
                        addNode(room, (UseItem::class as NodeLoader).generateFromArgs(args, room) ?: return)
                    }


                    "aotv", "teleport", "hype", "bat" -> {
                        modMessage("recording Aotv do not move!")
                        val timeClicked = System.currentTimeMillis()
                        val yaw = room.getRelativeYaw(mc.thePlayer.rotationYaw)
                        val pitch = mc.thePlayer.rotationPitch
                        PlayerUtils.airClick()
                        Scheduler.scheduleLowS08Task {
                            if (timeClicked + 5000 < System.currentTimeMillis()) {
                                modMessage("recording timed out")
                                return@scheduleLowS08Task
                            }
                            val event = (it as? PacketEvent.Receive) ?: return@scheduleLowS08Task
                            val s08 = event.packet as S08PacketPlayerPosLook
                            val flag = s08.func_179834_f()
                            if (
                                flag.contains(S08PacketPlayerPosLook.EnumFlags.X) ||
                                flag.contains(S08PacketPlayerPosLook.EnumFlags.Y) ||
                                flag.contains(S08PacketPlayerPosLook.EnumFlags.Z) ||
                                event.isCanceled ||
                                s08.y - s08.y.floor() != 0.0
                            ) {
                                modMessage("Invalid Packet")
                                return@scheduleLowS08Task
                            }
                            val target = room.getRelativeCoords(BlockPos(s08.x, s08.y, s08.z))
                            Scheduler.schedulePreTickTask {
                                if (args[1].lowercase().equalsOneOf("aotv", "teleport")) {
                                    addNode(
                                        room,
                                        Aotv(
                                            playerCoords,
                                            target,
                                            yaw,
                                            pitch,
                                            awaitSecrets,
                                            maybeSecret,
                                            delay,
                                            center,
                                            stop,
                                            chain,
                                            reset
                                        )
                                    )
                                }
                                else if (args[1].lowercase().equalsOneOf("bat", "hype")) {
                                    addNode(
                                        room,
                                        Bat(
                                            playerCoords,
                                            target,
                                            yaw,
                                            pitch,
                                            awaitSecrets,
                                            maybeSecret,
                                            delay,
                                            center,
                                            stop,
                                            chain,
                                            reset
                                        )
                                    )
                                }

                            }


                        }
                    }

                    else -> {
                        modMessage("Usages: Add, Delete, Edit, Load")
                    }

                }


            }
        }
    }

    private fun getNode(room: UniqueRoom, args: Array<out String>): AutorouteNode? {
        val nodeList = nodes[room.name]
        if (args.size > 1) {
            val index = args[1].toIntOrNull()?.absoluteValue
            if (index == null) {
                modMessage("Provide a number for index")
                return null
            }
            if (nodeList.isNullOrEmpty() || index !in nodeList.indices) {
                modMessage("No node with index: $index")
                return null
            }
            return nodeList[index]
        }
        if (nodeList.isNullOrEmpty()) return null

        val node = nodeList.minByOrNull {
            room.getRealCoords(it.pos).distanceToPlayerSq
        }!!
        return node
    }

    fun addNode(room: UniqueRoom, autorouteNode: AutorouteNode) {
        modMessage("adding node")
        if (nodes[room.name] == null) {
            nodes[room.name] = mutableListOf()
        }
        nodes[room.name]?.add(autorouteNode)
        saveFile()
    }


}