package noobroutes.features.dungeon.autoroute


import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.MouseEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.Core
import noobroutes.Core.logger
import noobroutes.config.DataManager
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.PacketReturnEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.dungeon.autoroute.SecretUtils.secretCount
import noobroutes.features.dungeon.autoroute.nodes.*
import noobroutes.features.move.Zpew
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.ColorSetting
import noobroutes.features.settings.impl.DropdownSetting
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.utils.*
import noobroutes.utils.RotationUtils.offset
import noobroutes.utils.Utils.containsOneOf
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.json.JsonUtils.asVec3
import noobroutes.utils.render.Color
import noobroutes.utils.skyblock.*
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayer
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayerSq
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeYaw
import noobroutes.utils.skyblock.dungeon.tiles.Room
import noobroutes.utils.skyblock.dungeon.tiles.RoomData
import noobroutes.utils.skyblock.dungeon.tiles.RoomType
import noobroutes.utils.skyblock.dungeon.tiles.Rotations
import org.lwjgl.input.Keyboard
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.ceil
import kotlin.math.floor


/**
 * Modified from AK47 and MeowClient
 */

object AutoRoute : Module("Autoroute", description = "Ak47 modified", category = Category.DUNGEON) {
    val silent by BooleanSetting("Silent", default = true, description = "Serverside rotations")
    val renderRoutes by BooleanSetting("Render Routes", default = false, description = "Renders nodes")
    val renderIndex by BooleanSetting("Render Index", description = "Render index above node, useful for editing").withDependency { renderRoutes }
    val edgeRoutes by BooleanSetting("Edging", default = false, description = "Edges nodes :)").withDependency { renderRoutes }
    val depth by BooleanSetting("Depth", default = true, description = "Render Rings Through Walls").withDependency { renderRoutes }
    val drawEtherLines by BooleanSetting("Draw Ether Lines", default = true, description = "Draws a line to the ether target").withDependency { renderRoutes }
    val drawAotvLines by BooleanSetting("Draw Aotv Lines", default = true, description = "Draws a line to the Aotv target").withDependency { renderRoutes }
    val drawPearlClipText by BooleanSetting("Draw Pearlclip Distance", default = true, description = "Renders the distance of the Pearlclip above the node").withDependency { renderRoutes }
    val drawPearlCountText by BooleanSetting("Draw Pearl Count", default = true, description = "Renders the pearl count above the Pearl node").withDependency { renderRoutes }
    private val colorSettings by DropdownSetting("Colors").withDependency { renderRoutes }
    val etherwarpColor by ColorSetting("Etherwarp", default = Color.GREEN, description = "Color of Etherwarp nodes").withDependency { renderRoutes && colorSettings }
    val walkColor by ColorSetting("Walk", default = Color.GREEN, description = "Color of Walk nodes").withDependency { renderRoutes && colorSettings }
    val pearlClipColor by ColorSetting("Pearlclip", default = Color.GREEN, description = "Color of Pearlclip nodes").withDependency { renderRoutes && colorSettings }
    val aotvColor by ColorSetting("Aotv", default = Color.GREEN, description = "Color of Aotv nodes").withDependency { renderRoutes && colorSettings }
    val boomColor by ColorSetting("Boom", default = Color.GREEN, description = "Color of Boom nodes").withDependency { renderRoutes && colorSettings }
    val batColor by ColorSetting("Bat", default = Color.GREEN, description = "Color of Bat nodes").withDependency { renderRoutes && colorSettings }
    val pearlColor by ColorSetting("Pearl", default = Color.GREEN, description = "Color of Pearl nodes").withDependency { renderRoutes && colorSettings }
    val useItemColor by ColorSetting("Use Item", default = Color.GREEN, description = "Color of Use Item nodes").withDependency { renderRoutes && colorSettings }
    private var editMode by BooleanSetting("Edit Mode", description = "Prevents nodes from triggering")
    val editModeBind by KeybindSetting("Edit Mode Toggle", Keyboard.KEY_NONE, description = "Toggles Edit Mode").onPress { editMode = !editMode }


    private val roomReplacement
        get() = Room(
            Rotations.NORTH,
            RoomData(LocationUtils.currentArea.name, RoomType.NORMAL, cores = listOf(0, 0), 0, 0, 0),
            roomComponents = mutableSetOf()
        )

    private val nodeRegistry = mapOf(
        Pair("Etherwarp", Etherwarp::class),
        Pair("Aotv", Aotv::class),
        Pair("Boom", Boom::class),
        Pair("Walk", Walk::class),
        Pair("Bat", Bat::class),
        Pair("PearlClip", PearlClip::class),
        Pair("Pearl", Pearl::class)
    )

    private var lastRoute = 0L
    val routing get() = System.currentTimeMillis() - lastRoute < 200


    val items = listOf(
        "Health Potion VIII Splash Potion",
        "Healing Potion 8 Splash Potion",
        "Healing Potion VIII Splash Potion",
        "Healing VIII Splash Potion",
        "Healing 8 Splash Potion",
        "Decoy",
        "Inflatable Jerry",
        "Spirit Leap",
        "Trap",
        "Training Weights",
        "Defuse Kit",
        "Dungeon Chest Key",
        "Treasure Talisman",
        "Revive Stone",
        "Architect's First Draft"
    )


    private var nodes = mutableMapOf<String, MutableList<Node>>()


    var serverSneak = false

    var pearlSoundRegistered = false
    private var sneakRegistered = false
    private var unsneakRegistered = false

    fun ether() {
        sneakRegistered = true
        PlayerUtils.sneak()
    }

    var aotvTarget: BlockPos? = null
    fun aotv(pos: BlockPos) {
        aotvTarget = pos
        unsneakRegistered = true
        PlayerUtils.forceUnSneak()
    }

    fun resetRotation() {
        rotating = false
        rotatingPitch = null
        rotatingYaw = null
    }


    @SubscribeEvent
    fun unsneak(event: RenderWorldLastEvent) {
        if (!unsneakRegistered) return
        if (mc.thePlayer.isSneaking) PlayerUtils.forceUnSneak()
        if (serverSneak) return
        PlayerUtils.airClick()
        aotvTarget?.let { Zpew.doZeroPingAotv(it) }
        resetRotation()
        unsneakRegistered = false
        PlayerUtils.resyncSneak()
    }

    @SubscribeEvent
    fun sneak(event: RenderWorldLastEvent) {
        if (!sneakRegistered) return
        if (!mc.thePlayer.isSneaking) PlayerUtils.sneak()
        if (!serverSneak) return
        PlayerUtils.airClick()
        resetRotation()
        sneakRegistered = false
        PlayerUtils.resyncSneak()
    }

    @SubscribeEvent
    fun onKeyInput(event: MouseEvent) {
        if (event.button == 1 && event.buttonstate && System.currentTimeMillis() - lastRoute < 150) {
            val room = DungeonUtils.currentRoom ?: roomReplacement
            val nodes = nodes[room.data.name]?.filter { node ->
                val realCoord = room.getRealCoords(node.pos)
                if (node.chain) (
                                abs(PlayerUtils.posX - realCoord.xCoord) < 0.001 &&
                                abs(PlayerUtils.posZ - realCoord.zCoord) < 0.001 &&
                                PlayerUtils.posY >= node.pos.yCoord - 0.01 && PlayerUtils.posY <= node.pos.yCoord + 0.5
                        ) else realCoord.distanceToPlayer <= 0.5
            }
            if (nodes?.isNotEmpty() == true) event.isCanceled = true
        }
        if (event.button == 2 && event.buttonstate) {
            if (secretCount < 0) {
                secretCount = 0;
                event.isCanceled = true
                return
            }
            val room = DungeonUtils.currentRoom ?: roomReplacement
            nodes[room.data.name]?.forEach { it.reset() }
        }
    }

    @SubscribeEvent
    fun onPacketSendReturn(event: PacketReturnEvent.Send) {
        if (event.packet !is C0BPacketEntityAction) return
        serverSneak = when (event.packet.action) {
            C0BPacketEntityAction.Action.START_SNEAKING -> true
            C0BPacketEntityAction.Action.STOP_SNEAKING -> false
            else -> serverSneak
        }
    }




    fun loadFile() {
        nodes.clear()
        val file = DataManager.loadDataFromFileObject("autoroutes")
        try {
            for ((key, arr) in file) {
                devMessage(key)
                val roomNodes = mutableListOf<Node>()
                arr.forEach {
                    val obj = it.asJsonObject
                    val type = obj.get("name")?.asString ?: return@forEach
                    val node = nodeRegistry[type] ?: return@forEach
                    val instance = node.java.getDeclaredConstructor()?.newInstance() ?: return@forEach
                    instance.loadNodeInfo(obj)
                    instance.pos = obj?.get("position")?.asVec3 ?: Vec3(0.0, 0.0, 0.0)
                    instance.center = obj.has("center")
                    instance.chain = obj.has("chain")
                    instance.chain = obj.has("reset")
                    instance.maybeSecret = obj.has("maybeSecret")
                    instance.stop = obj.has("stop")
                    instance.awaitSecrets = obj.get("secrets")?.asInt ?: 0
                    instance.delay = obj.get("delay")?.asLong ?: 0L
                    roomNodes.add(instance)
                }
                nodes[key] = roomNodes
            }
        } catch (e: Exception) {
            modMessage("Error Loading Rings")
            logger.error("error loading rings", e)
        }

    }

    private fun saveFile() {
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
        if (!renderRoutes || nodes[room.data.name] == null) return
        nodes[room.data.name]?.forEach {
            it.render(room)
        }
        if (renderIndex) {
            nodes[room.data.name]?.forEachIndexed { index, node ->
                node.drawIndex(index, room)
            }
        }
    }

    var rotating = false
    var rotatingYaw: Float? = null
    var rotatingPitch: Float? = null
    private var nodesToRun = mutableListOf<Node>()

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun motion(event: MotionUpdateEvent.Pre) {
        if (PlayerUtils.movementKeysPressed) {
            resetRotation()
            nodesToRun.clear()
            val room = DungeonUtils.currentRoom ?: roomReplacement
            nodes[room.data.name]?.forEach { it.reset() }
        }
        if (rotating) {
            rotatingYaw?.let {
                event.yaw = it + offset
            }
            rotatingPitch?.let {
                event.pitch = it + offset
            }
        }
    }

    private fun inNodes(room: Room): MutableList<Node> {
        val inNodes = mutableListOf<Node>()
        nodes[room.data.name]?.forEach { node ->
            val realCoord = room.getRealCoords(node.pos)
            val inNode = if (node.chain) (
                    abs(PlayerUtils.posX - realCoord.xCoord) < 0.001 &&
                            abs(PlayerUtils.posZ - realCoord.zCoord) < 0.001 &&
                            PlayerUtils.posY >= node.pos.yCoord - 0.01 && PlayerUtils.posY <= node.pos.yCoord + 0.5
                    ) else realCoord.distanceToPlayer <= 0.5
            if (inNode && !node.triggered) {
                node.triggered = true
                inNodes.add(node)
            } else if (!inNode && node.triggered) {
                node.reset()
            }
        }
        return inNodes
    }
    var delay = 0L

    private val canRoute get() = (System.currentTimeMillis() - delay >= 0) && PlayerUtils.canSendC08

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onTick(event: ClientTickEvent) {
        if (event.isEnd || mc.thePlayer == null || !canRoute) return
        val room = DungeonUtils.currentRoom ?: roomReplacement

        if (nodes[room.data.name] == null || editMode || PlayerUtils.movementKeysPressed) {
            resetRotation()
            nodesToRun.clear()
            return
        }

        val inNodes = inNodes(room)
        if (inNodes.isNotEmpty()) {
            inNodes.sortWith(compareByDescending { it.priority })
            if (inNodes.firstOrNull()?.name == "Boom") {
                inNodes.coerceMax(2)
            } else inNodes.coerceMax(1)
            nodesToRun = inNodes
        }
        if (nodesToRun.isEmpty()) {
            if (secretCount < 0) secretCount = 0
            return
        }
        nodesToRun.firstOrNull()?.let { node ->

            lastRoute = System.currentTimeMillis()
            if (node.reset && !node.resetTriggered) {
                secretCount = 0
                node.resetTriggered = true
            }
            val realCoord = room.getRealCoords(node.pos)
            if (!if (node.chain) (
                abs(PlayerUtils.posX - realCoord.xCoord) < 0.001 &&
                        abs(PlayerUtils.posZ - realCoord.zCoord) < 0.001 &&
                        PlayerUtils.posY >= node.pos.yCoord - 0.01 && PlayerUtils.posY <= node.pos.yCoord + 0.5
                ) else realCoord.distanceToPlayer <= 0.5) {
                nodesToRun.remove(node)
                resetRotation()
                return
            }

            if (!node.secretTriggered) {
                if (secretCount >= 0) secretCount -= node.awaitSecrets else secretCount = -node.awaitSecrets
                node.secretTriggered = true
            }
            if (secretCount < 0) {
                Scheduler.schedulePreMovementUpdateTask {
                    node.awaitMotion((it as MotionUpdateEvent.Pre), room)
                }
                node.awaitTick(room)
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
            resetRotation()
            secretCount = 0
            node.tick(room)
            //devMessage("runTick: ${System.currentTimeMillis()}")
            Scheduler.schedulePreMovementUpdateTask {
                node.motion((it as MotionUpdateEvent.Pre), room)
                //devMessage("motionUpdate: ${System.currentTimeMillis()}")
            }
            if (node.delete) nodesToRun.remove(node)
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
            return floor(c) + v / 10
        }
    }


    private var deletedNodes = mutableListOf<Node>()

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
                addNode(room, deletedNodes.removeFirstOrNull() ?: return modMessage("no node to restore"))
            }


            "delete", "remove", "begone", "eradicate", "flaccid" -> {
                val node = getNode(room, args) ?: return
                deletedNodes.add(node)
                nodes[room.data.name]?.remove(node)
                saveFile()
                modMessage("Removed ${deletedNodes.last().name}")
            }

            "edit" -> {
                val node = getNode(room, args) ?: return
                node.awaitSecrets = awaitSecrets
                node.reset = reset
                node.center = center
                node.delay = delay
                node.stop = stop
                node.chain = chain
            }

            "add", "create", "erect" -> {
                if (args.size < 2) {
                    modMessage("nodes: etherwarp, walk, pearlclip, aotv")
                    return
                }
                val playerCoords = room.getRelativeCoords(
                    floor(PlayerUtils.posX) + 0.5,
                    floor(PlayerUtils.posY),
                    floor(PlayerUtils.posZ) + 0.5
                )
                when (args[1].lowercase()) {
                    "warp", "etherwarp", "etherwarp_target", "etherwarptarget", "ether", "ew" -> {
                        val raytrace = EtherWarpHelper.rayTraceBlock(200, 1f, true)
                        if (raytrace == null) {
                            modMessage("No Target Found")
                            return
                        }
                        val target = room.getRelativeCoords(raytrace)
                        addNode(
                            room,
                            Etherwarp(playerCoords, target, awaitSecrets, maybeSecret, delay, center, stop, chain, reset)
                        )
                    }
                    "boom", "tnt" -> {
                        val block = mc.objectMouseOver.blockPos
                        val target = room.getRelativeCoords(block)
                        if (isAir(block)) {
                            modMessage("must look at a block")
                            return
                        }
                        addNode(room, Boom(playerCoords, target, awaitSecrets, maybeSecret, delay, center, stop, chain, reset))
                    }

                    "walk" -> {
                        addNode(
                            room,
                            Walk(
                                playerCoords,
                                room.getRelativeYaw(mc.thePlayer.rotationYaw.round(14).toFloat()),
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

                    "pearlclip" -> {
                        if (args.size < 3) {
                            modMessage("Need Distance")
                        }
                        val distance = args[2].toIntOrNull()?.absoluteValue
                        if (distance == null) {
                            modMessage("Provide a Number thanks")
                            return
                        }
                        addNode(
                            room,
                            PearlClip(playerCoords, distance, awaitSecrets, maybeSecret, delay, center, stop, chain, reset)
                        )
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

    private fun getNode(room: Room, args: Array<out String>): Node? {
        val nodeList = nodes[room.data.name]
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

    var clipDistance = 0
    private var clipRegistered = false

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Receive) {
        if (clipRegistered && event.packet is S08PacketPlayerPosLook) {
            event.isCanceled = true
            PacketUtils.sendPacket(C06PacketPlayerPosLook(event.packet.x, event.packet.y, event.packet.z, event.packet.yaw, event.packet.pitch, false))
            mc.thePlayer.setPosition(
                mc.thePlayer.posX.floor() + 0.5,
                mc.thePlayer.posY.floor() - clipDistance,
                mc.thePlayer.posZ.floor() + 0.5
            )
            pearlSoundRegistered = false
            clipRegistered = false
        }

        if (!pearlSoundRegistered || event.packet !is S29PacketSoundEffect) return
        if (event.packet.soundName != "random.bow" || event.packet.volume != 0.5f) return
        clipRegistered = true
    }

    private fun addNode(room: Room, node: Node) {
        modMessage("adding node")
        if (nodes[room.data.name] == null) {
            nodes[room.data.name] = mutableListOf()
        }
        nodes[room.data.name]?.add(node)
        //devMessage(nodes[room.data.name])
        saveFile()
    }


}