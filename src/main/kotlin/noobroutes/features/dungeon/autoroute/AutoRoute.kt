package noobroutes.features.dungeon.autoroute


import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.passive.EntityBat
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.server.S0DPacketCollectItem
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.config.DataManager
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.PacketReturnEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.dungeon.autoroute.nodes.Etherwarp
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.utils.Scheduler
import noobroutes.utils.Utils.getEntitiesOfType
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.getBlockAt
import noobroutes.utils.isBlock
import noobroutes.utils.noControlCodes
import noobroutes.utils.positionVector
import noobroutes.utils.skyblock.EtherWarpHelper
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayer
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayerSq
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import noobroutes.utils.skyblock.dungeon.tiles.Room
import noobroutes.utils.skyblock.modMessage
import org.lwjgl.input.Keyboard
import kotlin.math.abs
import kotlin.math.floor


/**
 * Taken from AK47
 */

object AutoRoute : Module("Autoroute", description = "Ak47 modified", category = Category.DUNGEON) {
    val silent by BooleanSetting("Silent", default = true, description = "Serverside rotations")
    val renderRoutes by BooleanSetting("Render Routes", default = false, description = "Renders nodes")
    val edgeRoutes by BooleanSetting("Edging", default = false, description = "Edges nodes :)").withDependency { renderRoutes }
    val depth by BooleanSetting("Depth", default = true, description = "Render Rings Through Walls").withDependency { renderRoutes }
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

    var etherRegistered = false
    var sneakRegistered = false
    var secretsNeeded = 0

    var clickRegistered = false
    var itemRegistered = false
    var batSpawnRegistered = false
    var batDeathRegistered = false

    fun ether(){
        sneakRegistered = true
        PlayerUtils.sneak()
    }

    @SubscribeEvent
    fun sneak(event: RenderWorldLastEvent){
        if (!sneakRegistered) return
        if (mc.thePlayer.isSneaking) PlayerUtils.sneak()
        if (!etherwarp) return
        PlayerUtils.airClick()
        PlayerUtils.unSneak()
        sneakRegistered = false
    }


    //@SubscribeEvent
    fun onKeyInput(event: InputEvent.KeyInputEvent) {
        val key = Keyboard.getEventKey()
        if (key == mc.gameSettings.keyBindSneak.keyCode) PlayerUtils.sneak()
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
    fun batSpawn(event: TickEvent.ClientTickEvent) {
        if (event.isEnd || !batSpawnRegistered) return
        val bats = mc.theWorld.getEntitiesOfType<EntityBat>()
        for (bat in bats) {
            if (bat.positionVector.distanceToPlayerSq > 225) continue
            devMessage("Bat Spawned")
            Scheduler.schedulePreTickTask {
                PlayerUtils.airClick()
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
        if (!renderRoutes || room == null || room.data.name == "Unknown" || nodes[room.data.name] == null || editMode) return
        nodes[room.data.name]?.forEach {
            it.render(room)
        }
    }

    @SubscribeEvent
    fun motionUpdateEvent(event: MotionUpdateEvent.Pre) {
        val room = DungeonUtils.currentRoom
        if (room == null || room.data.name == "Unknown" || nodes[room.data.name] == null || editMode || PlayerUtils.movementKeysPressed) return
        nodes[room.data.name]?.forEach { node ->
            val realCoord = room.getRealCoords(node.pos)
            val inNode = if (node.chain) (
                    abs(PlayerUtils.posX - realCoord.xCoord) < 0.001 &&
                    abs(PlayerUtils.posZ - realCoord.zCoord) < 0.001 &&
                    PlayerUtils.posY >= node.pos.yCoord - 0.01 && PlayerUtils.posY <= node.pos.yCoord + 0.5
            ) else realCoord.distanceToPlayer <= 0.5
            if (inNode && !triggerNodes.contains(node)) {
                node.run(event, room)
                triggerNodes.add(node)
            } else if (!inNode && triggerNodes.contains(node)) {
                triggerNodes.remove(node)
            }
        }
    }

    fun handleAutoRouteCommand(args: Array<out String>) {
        val room = DungeonUtils.currentRoom
        if (room == null || room.data.name == "Unknown") {
            devMessage("Not in a Room")
            return
        }
        if (args.size < 2) {
            modMessage("nodes: etherwarp")
            return
        }

        when (args[0].lowercase()) {
            "add", "create", "erect" -> {
                val playerCoords = room.getRelativeCoords(floor(PlayerUtils.posX) + 0.5, floor(PlayerUtils.posY), floor(PlayerUtils.posZ) + 0.5)
                when (args[1].lowercase()) {
                    "warp", "etherwarp", "etherwarp_target", "etherwarptarget", "ether" -> {
                        val raytrace = EtherWarpHelper.rayTraceBlock(200, 1f, true)
                        if (raytrace == null) {
                            modMessage("No Target Found")
                            return
                        }
                        val target = room.getRelativeCoords(raytrace)
                        addNode(room, Etherwarp(playerCoords, target))
                    }

                }
            }
        }
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



}