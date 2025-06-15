package noobroutes.features.dungeon.autobloodrush

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.Core
import noobroutes.config.DataManager
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.RoomEnterEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.dungeon.autobloodrush.routes.DoorRoute
import noobroutes.features.dungeon.autobloodrush.routes.Etherwarp
import noobroutes.features.dungeon.autoroute.AutoRouteUtils
import noobroutes.features.dungeon.autoroute.AutoRouteUtils.resetRotation
import noobroutes.features.dungeon.autoroute.AutoRouteUtils.serverSneak
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.ColorSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.*
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.EtherWarpHelper
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayerSq
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoordsOdin
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import noobroutes.utils.skyblock.dungeon.ScanUtils
import noobroutes.utils.skyblock.dungeon.tiles.Room
import noobroutes.utils.skyblock.dungeon.tiles.Rotations
import noobroutes.utils.skyblock.modMessage
import kotlin.math.floor

object AutoBloodRush : Module("Auto Blood Rush", description = "Autoroutes for bloodrushing", category = Category.DUNGEON) {


    private val clipDistance by NumberSetting(name = "Clip Distance", description = "how far u clip", min = 0.0, max = 2, default = 0.5, increment = 0.1)
    val silent by BooleanSetting("Silent", default = true, description = "Server side rotations")
    val editMode by BooleanSetting(
        "Edit Mode",
        description = "Allows you to edit routes"
    )
    private val doorNumberColor by ColorSetting(
        "Door Number Color",
        description = "I wonder what this could possibly mean",
        default = Color.Companion.GREEN
    ).withDependency { editMode }
    val nodeColor by ColorSetting(
        "Node",
        description = "Color of Route Nodes",
        default = Color.Companion.GREEN
    ).withDependency { editMode }




    var routes = mutableMapOf<String, MutableMap<String, MutableList<BloodRushRoute>>>()
    data class Door(val pos: BlockPos, val rotation: Rotations)

    val oneByOneDoors = listOf(
        BlockPos(-1, 69, 15),
        BlockPos(15, 69, -1),
        BlockPos(31, 69, 14),
        BlockPos(15, 69, 31)
    )
    val twoByOneDoors =  listOf(
        BlockPos(-1, 69, 15),
        BlockPos(15, 69, 31),
        BlockPos(47, 69, 31),
        BlockPos(63, 69, 15),
        BlockPos(47, 69, -1),
        BlockPos(15, 69, -1)
    )
    val threeByOneDoors = listOf(
        BlockPos(95, 69, 15),
        BlockPos(79, 69, 31),
        BlockPos(47, 69, 31),
        BlockPos(15, 69, 31),
        BlockPos(-1, 69, 15),
        BlockPos(15, 69, -1),
        BlockPos(47, 69, -1),
        BlockPos(79, 69, -1)
    )



    val fourByOneDoors = listOf(
        BlockPos(-1, 69, 15),
        BlockPos(15, 69, -1),
        BlockPos(47, 69, -1),
        BlockPos(79, 69, -1),
        BlockPos(111, 69, -1),
        BlockPos(127, 69, 15),
        BlockPos(111, 69, 31),
        BlockPos(79, 69, 31),
        BlockPos(47, 69, 31),
        BlockPos(15, 69, 31),

    )
    val lShapedDoors = listOf(
        BlockPos(-1, 69, 47),
        BlockPos(15, 69, 63),
        BlockPos(47, 69, 63),
        BlockPos(63, 69, 47),
        BlockPos(47, 69, 31),
        BlockPos(31, 69, 15),
        BlockPos(15, 69, -1),
        BlockPos(-1, 69, 15)
    )

    val twoByTwoDoors = listOf(
        BlockPos(15, 69, -1),
        BlockPos(-1, 69, 15),
        BlockPos(-1, 69, 47),
        BlockPos(15, 69, 63),
        BlockPos(47, 69, 63),
        BlockPos(63, 69, 47),
        BlockPos(63, 69, 15),
        BlockPos(47, 69, -1)
    )



    var activeRoutes: MutableMap<String, String> = mutableMapOf()
    var currentDoor: Door? = null
    val doors = mutableListOf<Door>()
    var fairyRoom: Room? = null


    fun scanForDoors() {
        val nextRoom = getNextRoom() ?: return
        if (nextRoom.data.cores.size != nextRoom.roomComponents.size) {
            Core.logger.info("room cores and components not equal")
            return
        }
        val nextDoor = findRoomDoors(nextRoom) ?: return
        doors.add(nextDoor)
        if (isBlock(nextDoor.pos, Blocks.stained_hardened_clay)) {
            currentDoor = null
            return
        }
        currentDoor = nextDoor
    }

    fun getNextRoom(): Room? {
        val corePos = currentDoor?.let {
            when (it.rotation) {
                Rotations.SOUTH -> {
                    Vec2i(it.pos.x, it.pos.z - 16)
                }

                Rotations.NORTH -> {
                    Vec2i(it.pos.x, it.pos.z + 16)
                }

                Rotations.WEST -> {
                    Vec2i(it.pos.x + 16, it.pos.z)
                }

                Rotations.EAST -> {
                    Vec2i(it.pos.x - 16, it.pos.z)
                }

                Rotations.NONE -> {
                    devMessage("no rotation")
                    Vec2i(it.pos.x, it.pos.z + 16)
                }
            }
        } ?: return null

        val cached = ScanUtils.passedRooms.firstOrNull { room ->
            room.roomComponents.any {
                it.x == corePos.x && it.z == corePos.z
            }
        }
        if (cached != null) return cached
        val scannedRoom = ScanUtils.scanRoom(Vec2i(corePos.x, corePos.z)) ?: return null
        return scannedRoom
    }

    val room1x4Names = hashSetOf(
        "Quartz Knight",
        "Pipes",
        "Pit",
        "Hallway",
        "Waterfall",
        "Mossy"
    )

    val room1x3Names = hashSetOf(
        "Slime",
        "Red Blue",
        "Diagonal",
        "Gravel",
        "Deathmite",
        "Catwalk",
        "Wizard"
    )

    fun getRoomDoors(room: Room): List<BlockPos> {
        return when (room.data.cores.size) {
            1 -> oneByOneDoors
            2 -> twoByOneDoors
            3 -> {
                if (room1x3Names.contains(room.data.name)) threeByOneDoors else lShapedDoors
            }
            4 -> {
                if (room1x4Names.contains(room.data.name)) fourByOneDoors else twoByTwoDoors
            }
            else -> listOf()
        }
    }



    private val skullIds = listOf(
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvM2JjYmJmOTRkNjAzNzQzYTFlNzE0NzAyNmUxYzEyNDBiZDk4ZmU4N2NjNGVmMDRkY2FiNTFhMzFjMzA5MTRmZCJ9fX0=",
        "eyJ0ZXh0dXJlcyI6eyJTS0lOIjp7InVybCI6Imh0dHA6Ly90ZXh0dXJlcy5taW5lY3JhZnQubmV0L3RleHR1cmUvOWQ5ZDgwYjc5NDQyY2YxYTNhZmVhYTIzN2JkNmFkYWFhY2FiMGMyODgzMGZiMzZiNTcwNGNmNGQ5ZjU5MzdjNCJ9fX0="
    )



    fun findRoomDoors(room: Room): Door? {
        val possibleDoors = getRoomDoors(room).map { room.getRealCoordsOdin(it) }
        val doors = possibleDoors.filter {

            getSkull(it.add(-2, 1, -2))?.skullTexture in skullIds
        }
        val doorPos = doors.firstOrNull {it != currentDoor?.pos} ?: return null
        val roomRouteName = "${possibleDoors.indexOf(currentDoor?.pos)}>${possibleDoors.indexOf(doorPos)}"
        activeRoutes[room.data.name] = roomRouteName
        devMessage(Pair(room.data.name, roomRouteName))
        val closestComponent = room.roomComponents.minByOrNull { it.blockPos.distanceSq(doorPos) }!!
        val rotation = when {
            doorPos.x < closestComponent.x -> Rotations.EAST
            doorPos.x > closestComponent.x -> Rotations.WEST
            doorPos.z > closestComponent.z -> Rotations.NORTH
            doorPos.z < closestComponent.z -> Rotations.SOUTH
            else -> Rotations.NORTH
        }
        ScanUtils.addCachedRoom(room)
        return Door(doorPos, rotation)
    }

    var started = false

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        started = false
        activeRoutes.clear()
        currentDoor = null
        doors.clear()
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (mc.thePlayer == null) return
        if (event.isEnd && currentDoor != null) {
            scanForDoors()
            if (currentDoor == null) devMessage(doors)
        }
        if (event.isEnd || (activeRoutes.isEmpty() && !editMode) || !PlayerUtils.canSendC08) return
        val room = DungeonUtils.currentRoom ?: return
        if (!activeRoutes.any { it.key == room.data.name } && !editMode) return
        val key = if (editMode) routeName else activeRoutes[room.data.name]!!

        val node = routes.getOrPut(room.data.name) {mutableMapOf()}.getOrPut(key) {mutableListOf()}.firstOrNull { it.inNode(room) } ?: return
        node.runTick(room)
        Scheduler.schedulePreMotionUpdateTask {
            node.runMotion(room, it as MotionUpdateEvent.Pre)
        }
    }


    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent){

        val room = DungeonUtils.currentRoom ?: return
        if (editMode) {
            val doorPositions = if (room.data.name == "Entrance") oneByOneDoors.map { room.getRealCoordsOdin(it) } else getRoomDoors(room).map { room.getRealCoordsOdin(it) }
            doorPositions.forEachIndexed { index, pos ->
                Renderer.drawStringInWorld(index.toString(), pos.toVec3().add(0.5, 2.0, 0.5), doorNumberColor, scale = 0.1f)
            }
        }
        if (routeName == "" && editMode) return
        val key = if (editMode) routeName else activeRoutes[room.data.name] ?: return
        val nodeList = routes.getOrPut(room.data.name) {mutableMapOf()}.getOrPut(key) {mutableListOf()}
        if (editMode) nodeList.forEachIndexed {
            index, node ->
            node.renderIndex(room, index)
            node.render(room)
        } else {
            nodeList.forEach { node ->
                node.render(room)
            }
        }
    }

    @SubscribeEvent
    fun onEnterDungeon(event: RoomEnterEvent) {
        routeName = ""
        if (event.room?.data?.name != "Entrance" || started) return
        started = true
        val door = Door(event.room.getRealCoordsOdin(15, 69, -1), event.room.rotation)
        currentDoor = door
        doors.add(door)
        scanForDoors()
    }

    fun loadFile() {
        val file = DataManager.loadDataFromFileObject("autobloodroutes")
        routes.clear()

        for ((roomName, routeList) in file) {
            val roomRoutes = mutableMapOf<String, MutableList<BloodRushRoute>>()

            for (entryElement in routeList) {
                val entryObj = entryElement.asJsonObject

                for ((routeName, routeArray) in entryObj.entrySet()) {
                    val routeItems = mutableListOf<BloodRushRoute>()
                    if (routeArray.isJsonArray) {
                        for (routeElement in routeArray.asJsonArray) {
                            val routeObj = routeElement.asJsonObject
                            if (routeObj.get("name").asString == "Etherwarp") {
                                routeItems.add(Etherwarp.loadFromJsonObject(routeObj))
                            } else {
                                routeItems.add(DoorRoute.loadFromJsonObject(routeObj))
                            }
                        }
                    }
                    roomRoutes[routeName] = routeItems
                }
            }

            routes[roomName] = roomRoutes
        }
    }

    fun saveFile(){
        val jsonObj = JsonObject()
        routes.forEach { (roomName, roomRoutes) ->
            val arr = JsonArray()
            roomRoutes.forEach { (key, value) ->
                val routeObj = JsonObject()
                val routeArray = JsonArray()
                value.forEach { route ->
                    routeArray.add(route.getAsJsonObject())
                }
                routeObj.add(key, routeArray)
                arr.add(routeObj)
            }

            jsonObj.add(roomName, arr)
        }
        DataManager.saveDataToFile("autobloodroutes", jsonObj)
    }


    val routeRegex = Regex("\\d+>\\d+")
    var routeName = ""
    fun handleBloodRushCommand(args: Array<out String>) {

        when (args[0].lowercase()) {
            "set" -> {
                val currentRoom = DungeonUtils.currentRoom ?: return modMessage("not in room")
                if (args.size < 2) return modMessage("need route name (doorNumber)>(doorNumber)")

                val doorNumbers = routeRegex.find(args[1])?.value?.split(">")?.map { it.toIntOrNull() } ?: return modMessage(
                    "Invalid Input"
                )
                if (doorNumbers.any {it == null}) return modMessage("Invalid Input")
                val maxDoorCount = getRoomDoors(currentRoom).size
                if (doorNumbers.any { it!! > maxDoorCount - 1}) {
                    modMessage("Invalid Door, valid options: (0-${maxDoorCount - 1})")
                }
                modMessage("set to ${doorNumbers[0]}>${doorNumbers[1]}")
                routeName = "${doorNumbers[0]}>${doorNumbers[1]}"
            }
            "add" -> {
                if (!editMode) return modMessage("Edit Mode Required")
                val currentRoom = DungeonUtils.currentRoom ?: return modMessage("not in room")
                if (routeName == "") return modMessage("no route set")
                if (args.size < 2) {
                    modMessage("ew, door")
                    return
                }
                val playerCoords = currentRoom.getRelativeCoords(
                    floor(PlayerUtils.posX) + 0.5,
                    floor(PlayerUtils.posY),
                    floor(PlayerUtils.posZ) + 0.5
                )
                if (args[1].lowercase() == "ew") {
                    val raytrace = EtherWarpHelper.rayTraceBlock(200, 1f)
                    if (raytrace == null) {
                        modMessage("No Target Found")
                        return
                    }
                    val target = currentRoom.getRelativeCoords(raytrace)
                    routes.getOrPut(currentRoom.data.name) {mutableMapOf()}.getOrPut(routeName) {mutableListOf()}.add(
                        Etherwarp(playerCoords, target)
                    )
                    saveFile()
                    return
                }
                if (args[1].lowercase() == "door") {
                    routes.getOrPut(currentRoom.data.name) {mutableMapOf()}.getOrPut(routeName) {mutableListOf()}.add(
                        DoorRoute(playerCoords)
                    )
                    saveFile()
                    return
                }
            }
            "clear" -> {
                if (!editMode) return modMessage("Edit Mode Required")
                val currentRoom = DungeonUtils.currentRoom ?: return modMessage("Not in room")
                if (routeName == "") return modMessage("No route set")
                routes.getOrPut(currentRoom.data.name) {mutableMapOf()}.getOrPut(routeName) {mutableListOf()}.clear()
                saveFile()
            }

            "delete" -> {
                if (!editMode) return modMessage("Edit Mode Required")
                val currentRoom = DungeonUtils.currentRoom ?: return modMessage("Not in room")
                if (routeName == "") return modMessage("No route set")
                val nodeList = routes.getOrPut(currentRoom.data.name) {mutableMapOf()}.getOrPut(routeName) {mutableListOf()}
                if (nodeList.isEmpty()) return modMessage("No Nodes")
                if (args.size > 1) {
                    val index = args[1].toIntOrNull() ?: return modMessage("Invalid Index")
                    if (nodeList.size - 1 < index) return modMessage("Invalid Index")
                    nodeList.removeAt(index)
                    saveFile()
                    return
                }

                val node = nodeList.minByOrNull {
                    currentRoom.getRealCoords(it.pos).distanceToPlayerSq
                }!!
                nodeList.remove(node)
                saveFile()
            }
            "load" -> {
                loadFile()
            }
        }
    }

    var direction: Rotations = Rotations.NONE
    var thrown = 0L

    data class ExpectedS08(val x: Double, val z: Double, val dir: Int)
    lateinit var clipS08: ExpectedS08



    @SubscribeEvent
    fun clipForwardish(event: PacketEvent.Receive) {
        if (event.packet !is S08PacketPlayerPosLook || System.currentTimeMillis() - thrown > 1000) return
        if (event.packet.x != clipS08.x || event.packet.z != clipS08.z) return
        event.isCanceled = true
        PacketUtils.sendPacket(C06PacketPlayerPosLook(event.packet.x, event.packet.y, event.packet.z, event.packet.yaw, event.packet.pitch, false))
        val dx = if (clipS08.dir == 0) 1 else if (clipS08.dir == 1) -1 else 0
        val dz = if (clipS08.dir == 2) 1 else if (clipS08.dir == 3) -1 else 0
        if (mc.theWorld.getBlockState(BlockPos(event.packet.x, event.packet.y - 1, event.packet.z)).block == Blocks.cobblestone_wall) {
            Scheduler.scheduleC03Task(0, true) { PacketUtils.sendPacket(C06PacketPlayerPosLook(clipS08.x + 1.4 * dx, 70.0, clipS08.z + 1.4 * dz, event.packet.yaw, 90f, true)) }
            Scheduler.scheduleC03Task(1, true) {
                PacketUtils.sendPacket(C06PacketPlayerPosLook(clipS08.x + 2.8 * dx, 70.0, clipS08.z + 2.8 * dz,event.packet.yaw, 90f, true))
                mc.thePlayer.setPosition(clipS08.x + 3.8 * dx + 0.8 * dz, 70.0, clipS08.z + 3.8 * dz - 0.8 * dx)
                Scheduler.schedulePreTickTask {
                    AutoRouteUtils.etherwarp(event.packet.yaw, 90f, silent)
                }
                thrown = 0
            }
            return
        }
        Scheduler.scheduleC03Task(0, true) {
            PacketUtils.sendPacket(C06PacketPlayerPosLook(clipS08.x + 2.8 * dx, 70.0, clipS08.z + 2.8 * dz, event.packet.yaw , 90f, true))
            mc.thePlayer.setPosition(clipS08.x + 3.8 * dx + 0.8 * dz, 70.0, clipS08.z + 3.8 * dz - 0.8 * dx)
            Scheduler.schedulePreTickTask {
                AutoRouteUtils.etherwarp(event.packet.yaw, 90f, silent)
            }
            thrown = 0
        }
    }


    var autoBrUnsneakRegistered = false
    @SubscribeEvent
    fun unsneak(event: RenderWorldLastEvent) {
        if (!autoBrUnsneakRegistered) return
        if (Core.mc.thePlayer.isSneaking) PlayerUtils.unSneak()
        if (serverSneak) return
        PlayerUtils.airClick()
        resetRotation()
        autoBrUnsneakRegistered = false
    }
}