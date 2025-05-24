package noobroutes.features.dungeon.autoroute


import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraft.util.BlockPos
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.config.DataManager
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.PacketReturnEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.dungeon.autoroute.nodes.Aotv
import noobroutes.features.dungeon.autoroute.nodes.Etherwarp
import noobroutes.features.dungeon.autoroute.nodes.PearlClip
import noobroutes.features.dungeon.autoroute.nodes.Walk
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.ColorSetting
import noobroutes.features.settings.impl.DropdownSetting
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.utils.*
import noobroutes.utils.RotationUtils.offset
import noobroutes.utils.Utils.containsOneOf
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.render.Color
import noobroutes.utils.skyblock.EtherWarpHelper
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayer
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeYaw
import noobroutes.utils.skyblock.dungeon.tiles.Room
import noobroutes.utils.skyblock.modMessage
import org.lwjgl.input.Keyboard
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.floor


/**
 * Modified from AK47 and MeowClient
 */

object AutoRoute : Module("Autoroute", description = "Ak47 modified", category = Category.DUNGEON) {
    val silent by BooleanSetting("Silent", default = true, description = "Serverside rotations")
    val renderRoutes by BooleanSetting("Render Routes", default = false, description = "Renders nodes")
    val edgeRoutes by BooleanSetting("Edging", default = false, description = "Edges nodes :)").withDependency { renderRoutes }
    val depth by BooleanSetting("Depth", default = true, description = "Render Rings Through Walls").withDependency { renderRoutes }
    private val colorSettings by DropdownSetting("Colors").withDependency { renderRoutes }
    val etherwarpColor by ColorSetting("Etherwarp", default = Color.GREEN, description = "Color of etherwarp nodes").withDependency { renderRoutes && colorSettings }
    val walkColor by ColorSetting("Walk", default = Color.GREEN, description = "Color of walk nodes").withDependency { renderRoutes && colorSettings }
    val pearlClipColor by ColorSetting("Pearlclip", default = Color.GREEN, description = "Color of pearlclip nodes").withDependency { renderRoutes && colorSettings }
    val aotvColor by ColorSetting("Aotv", default = Color.GREEN, description = "Color of Aotv nodes").withDependency { renderRoutes && colorSettings }
    val boomColor by ColorSetting("Boom", default = Color.GREEN, description = "Color of Boom nodes").withDependency { renderRoutes && colorSettings }



    var lastRoute = 0L
    val routing get() = System.currentTimeMillis() - lastRoute < 150

    var editMode by BooleanSetting("Edit Mode", description = "Prevents nodes from triggering")
    val editModeBind by KeybindSetting("Edit Mode Toggle", Keyboard.KEY_NONE, description = "Toggles Edit Mode").onPress {
        editMode = !editMode
    }

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


    var nodes = mutableMapOf<String, MutableList<Node>>()



    var serverSneak = false
    var lastEtherwarp = 0L

    var pearlSoundRegistered = false
    var etherRegistered = false
    var sneakRegistered = false


    var clickRegistered = false
    var itemRegistered = false
    var batSpawnRegistered = false
    var batDeathRegistered = false

    fun ether(){
        sneakRegistered = true
        routeSneak()
    }

    @SubscribeEvent
    fun sneak(event: RenderWorldLastEvent){
        if (!sneakRegistered) return
        if (!mc.thePlayer.isSneaking) routeSneak()
        if (!serverSneak) return
        PlayerUtils.airClick()
        rotating = false
        sneakRegistered = false
    }

    var sneakDuration = 0
    @SubscribeEvent
    fun onKeyInput(event: InputEvent.KeyInputEvent) {
        val key = Keyboard.getEventKey()
        if (key == mc.gameSettings.keyBindSneak.keyCode && sneakDuration > 0) PlayerUtils.sneak()
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


    fun clear() {
        batDeathRegistered = false
        clickRegistered = false
        itemRegistered = false
        batSpawnRegistered = false
    }

    fun loadFile() {

    }
    fun saveFile(){
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



    val triggerNodes = mutableListOf<Node>()

    @SubscribeEvent
    fun onRenderWorldLast(event: RenderWorldLastEvent){
        val room = DungeonUtils.currentRoom
        if (!renderRoutes || room == null || room.data.name == "Unknown" || nodes[room.data.name] == null) return
        nodes[room.data.name]?.forEach {
            it.render(room)
        }
    }

    var rotating = false
    var rotatingYaw: Float? = null
    var rotatingPitch: Float? = null
    private var nodesToRun = mutableListOf<Node>()

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun motion(event: MotionUpdateEvent.Pre){
        if (PlayerUtils.movementKeysPressed) {
            rotating = false
            rotatingYaw = null
            rotatingPitch = null
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

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onTick(event: ClientTickEvent) {
        if (event.isEnd) return
        if (sneakDuration > 0) {
            devMessage("current:$sneakDuration, after:${sneakDuration - 1}")
            sneakDuration--
            if (sneakDuration == 0) PlayerUtils.unSneak()
        }
        val room = DungeonUtils.currentRoom
        if (room == null || room.data.name == "Unknown" || nodes[room.data.name] == null || editMode || PlayerUtils.movementKeysPressed) {
            rotating = false
            return
        }
        val inNodes = mutableListOf<Node>()
        nodes[room.data.name]?.forEach { node ->
            val realCoord = room.getRealCoords(node.pos)
            val inNode = if (node.chain) (
                    abs(PlayerUtils.posX - realCoord.xCoord) < 0.001 &&
                    abs(PlayerUtils.posZ - realCoord.zCoord) < 0.001 &&
                    PlayerUtils.posY >= node.pos.yCoord - 0.01 && PlayerUtils.posY <= node.pos.yCoord + 0.5
            ) else realCoord.distanceToPlayer <= 0.5
            if (inNode && !triggerNodes.contains(node)) {
                triggerNodes.add(node)
                inNodes.add(node)
            } else if (!inNode && triggerNodes.contains(node)) {
                triggerNodes.remove(node)
            }
        }
        if (inNodes.isNotEmpty()) {
            inNodes.sortWith(compareByDescending { it.priority })
            if (inNodes.firstOrNull()?.name == "Boom") {
                inNodes.coerceMax(2)
            } else inNodes.coerceMax(1)

            nodesToRun = inNodes
        }

        nodesToRun.removeFirstOrNull()?.let { node ->
            lastRoute = System.currentTimeMillis()
            if (node.runStatus == Node.RunStatus.NotExecuted) {
                rotating = false
                SecretUtils.secretCount -= node.awaitSecrets
                if (SecretUtils.secretCount < 0) {
                    Scheduler.schedulePreMovementUpdateTask {
                        node.awaitMotion((it as MotionUpdateEvent.Pre), room)
                    }
                    node.awaitTick(room)
                    return
                }
                node.tick(room)
                Scheduler.schedulePreMovementUpdateTask {
                    node.motion((it as MotionUpdateEvent.Pre), room)
                }

            }
            if (node.runStatus == Node.RunStatus.Complete) nodesToRun.removeFirst()
        }
    }

    fun handleAutoRouteCommand(args: Array<out String>) {
        val room = DungeonUtils.currentRoom
        if (room == null || room.data.name == "Unknown") {
            devMessage("Not in a Room")
            return
        }
        val center = args.containsOneOf("center", "align", ignoreCase = true)
        val chain = args.containsOneOf("chain", ignoreCase = true)
        val maybeSecret = args.containsOneOf("maybe", "maybesecret", ignoreCase = true)
        val stop = args.containsOneOf("stop", ignoreCase = true)
        val awaitSecrets = args.firstOrNull {it.startsWith("await:", ignoreCase = true) }?.substringAfter("await:")?.toIntOrNull() ?: 0

        when (args[0].lowercase()) {
            "add", "create", "erect" -> {
                if (args.size < 2) {
                    modMessage("nodes: etherwarp, walk, pearlclip, aotv")
                    return
                }
                val playerCoords = room.getRelativeCoords(floor(PlayerUtils.posX) + 0.5, floor(PlayerUtils.posY), floor(PlayerUtils.posZ) + 0.5)
                when (args[1].lowercase()) {
                    "warp", "etherwarp", "etherwarp_target", "etherwarptarget", "ether", "ew" -> {
                        val raytrace = EtherWarpHelper.rayTraceBlock(200, 1f, true)
                        if (raytrace == null) {
                            modMessage("No Target Found")
                            return
                        }
                        val target = room.getRelativeCoords(raytrace)
                        addNode(room, Etherwarp(playerCoords, target, awaitSecrets, maybeSecret, 0, center, stop, chain))
                    }
                    "walk" -> {
                        addNode(room, Walk(playerCoords, room.getRelativeYaw(mc.thePlayer.rotationYaw.round(14).toFloat()), awaitSecrets, maybeSecret, 0, center, stop, chain))
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
                        addNode(room, PearlClip(playerCoords, distance, awaitSecrets, maybeSecret, 0, center, stop, chain))
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
                                    addNode(room, Aotv(playerCoords, target, yaw, pitch, awaitSecrets, maybeSecret, 0, center, stop, chain))
                                }
                            }


                        }
                    }



                }



            }
        }
    }


    var clipDistance = 0
    var clipRegistered = false
    @SubscribeEvent
    fun onPacket(event: PacketEvent.Receive) {
        if (clipRegistered && event.packet is S08PacketPlayerPosLook) {
            mc.thePlayer.setPosition(mc.thePlayer.posX.floor() + 0.5, mc.thePlayer.posY.floor() - clipDistance, mc.thePlayer.posZ.floor() + 0.5)
            pearlSoundRegistered = false
            clipRegistered = false
        }

        if (!pearlSoundRegistered || event.packet !is S29PacketSoundEffect) return
        if (event.packet.soundName != "random.bow" || event.packet.volume != 0.5f) return
        clipRegistered = true


    }

    fun addNode(room: Room, node: Node) {
        modMessage("adding node")
        if (nodes[room.data.name] == null) {
            nodes[room.data.name] = mutableListOf()
        }
        nodes[room.data.name]?.add(node)
        //devMessage(nodes[room.data.name])
        saveFile()
    }

    fun routeSneak(){
        PlayerUtils.sneak()
        sneakDuration = 3
    }






}