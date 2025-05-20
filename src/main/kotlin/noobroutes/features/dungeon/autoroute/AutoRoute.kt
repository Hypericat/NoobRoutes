package noobroutes.features.dungeon.autoroute


import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.passive.EntityBat
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S0DPacketCollectItem
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.config.DataManager
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.PacketReturnEvent
import noobroutes.features.Category
import noobroutes.features.Module
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
import noobroutes.utils.Utils.getEntitiesOfType
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.render.Color
import noobroutes.utils.skyblock.EtherWarpHelper
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayer
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayerSq
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



    var etherwarp = false
    var lastEtherwarp = 0L

    var pearlSoundRegistered = false
    var etherRegistered = false
    var sneakRegistered = false
    var secretsNeeded = 0

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
        if (!etherwarp) return
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
    fun item(event: PacketEvent.Receive) {
        if (!itemRegistered || event.packet !is S0DPacketCollectItem) return
        val item = (mc.theWorld.getEntityByID(event.packet.collectedItemEntityID) as EntityItem).entityItem.displayName.noControlCodes
        if (!items.contains(item)) return
        secretsNeeded--
        if (secretsNeeded > 0) return
        ether()
        clear()
    }

    @SubscribeEvent
    fun onPacketSendReturn(event: PacketReturnEvent.Send) {
        if (event.packet !is C0BPacketEntityAction) return
        etherwarp = when (event.packet.action) {
            C0BPacketEntityAction.Action.START_SNEAKING -> true
            C0BPacketEntityAction.Action.STOP_SNEAKING -> false
            else -> etherwarp
        }
    }


    @SubscribeEvent
    fun click(event: PacketReturnEvent.Send) {
        if (!clickRegistered || event.packet !is C08PacketPlayerBlockPlacement ||
            event.packet.position == null ||
            !isBlock(event.packet.position, Blocks.chest, Blocks.trapped_chest, Blocks.lever, Blocks.skull)
            ) return
        devMessage("clicked ${getBlockAt(event.packet.position).unlocalizedName}")
        secretsNeeded--
        if (secretsNeeded > 0) return
        Scheduler.schedulePreTickTask(1) {
            ether()
            clear()
        }
    }

    @SubscribeEvent
    fun batDeath(event: PacketEvent.Receive) {
        if (
            !batDeathRegistered ||
            event.packet !is S29PacketSoundEffect ||
            event.packet.soundName != "mob.bat.hurt" ||
            event.packet.positionVector.distanceToPlayerSq > 225
        ) return
        secretsNeeded--
        if (secretsNeeded > 0) return
        ether()
        clear()
    }

    @SubscribeEvent
    fun onBat(event: TickEvent.ClientTickEvent) {
        if (event.isEnd) return

        if (!batSpawnRegistered) return
        val bats = mc.theWorld.getEntitiesOfType<EntityBat>()
        for (bat in bats) {
            if (bat.positionVector.distanceToPlayerSq > 225) continue
            devMessage("Bat Spawned")
            Scheduler.schedulePreTickTask {
                PlayerUtils.airClick()
                rotating = false
                clear()
            }
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
    fun motion(event: MotionUpdateEvent){
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
    fun onTick(e: ClientTickEvent) {
        if (e.isEnd) return
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

        inNodes.sortWith(compareByDescending { it.priority })
        if (inNodes.first().name == "Boom") {
            inNodes.coerceMax(2)
        } else inNodes.coerceMax(1)

        nodesToRun = inNodes


        nodesToRun.first().let { node ->
            if (node.runStatus == Node.RunStatus.NotExecuted) {
                rotating = false
                if (node.awaitSecrets > 0) {
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
        val

        when (args[0].lowercase()) {
            "add", "create", "erect" -> {
                if (args.size < 2) {
                    modMessage("nodes: etherwarp, walk, pearlclip")
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
                        addNode(room, Etherwarp(playerCoords, target))
                    }
                    "walk" -> {
                        addNode(room, Walk(playerCoords, room.getRelativeYaw(mc.thePlayer.rotationYaw.round(14).toFloat())))
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
                        addNode(room, PearlClip(playerCoords, distance))
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

    fun pearlclip(){

    }



}