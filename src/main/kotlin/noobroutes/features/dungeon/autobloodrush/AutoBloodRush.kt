package noobroutes.features.dungeon.autobloodrush

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.Core
import noobroutes.config.DataManager
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.events.impl.RoomEnterEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.dungeon.autobloodrush.routes.Etherwarp
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.ColorSetting
import noobroutes.utils.Scheduler
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.Vec2i
import noobroutes.utils.add
import noobroutes.utils.isAir
import noobroutes.utils.isBlock
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.EtherWarpHelper
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoordsOdin
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import noobroutes.utils.skyblock.dungeon.ScanUtils
import noobroutes.utils.skyblock.dungeon.tiles.Room
import noobroutes.utils.skyblock.dungeon.tiles.Rotations
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.toVec3
import kotlin.math.floor

object AutoBloodRush : Module("Auto Blood Rush", description = "Autoroutes for bloodrushing", category = Category.DUNGEON) {

    private val renderDoorNumbers by BooleanSetting(
        "Render Door Numbers",
        description = "Renders wither door numbers, essential for creating routes. Do not use while in normal gameplay, because this is somewhat performance intensive."
    )
    private val doorNumberColor by ColorSetting(
        "Door Number Color",
        description = "I wonder what this could possibly mean",
        default = Color.Companion.GREEN
    ).withDependency { renderDoorNumbers }

    var routes = mutableMapOf<String, MutableMap<String, MutableList<BloodRushRoute>>>()
    data class Door(val pos: BlockPos, val rotation: Rotations)

    val oneByOneDoors = listOf(
        BlockPos(-1, 69, 15),
        BlockPos(15, 69, -1),
        BlockPos(15, 69, 31),
        BlockPos(31, 69, 14)
    )
    val twoByOneDoors =  listOf(
        BlockPos(-1, 69, 15),
        BlockPos(15, 69, 31),
        BlockPos(47, 69, 31),
        BlockPos(63, 69, 15),
        BlockPos(47, 69, -1),
        BlockPos(15, 69, -1)
    )
    val fourByOneDoors = listOf(
        BlockPos(-1, 69, 15),
        BlockPos(15, 69, -1),
        BlockPos(47, 69, -1),
        BlockPos(79, 69, -1),
        BlockPos(111, 69, -1),
        BlockPos(127, 69, 15),
        BlockPos(111, 69, 31),
        BlockPos(15, 69, 31),
        BlockPos(79, 69, 31),
        BlockPos(47, 69, 31),
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



    var activeRoutes: MutableList<BloodRushRoute> = mutableListOf()
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

    fun getRoomDoors(room: Room): List<BlockPos> {
        return when (room.data.cores.size) {
            1 -> oneByOneDoors
            2 -> twoByOneDoors
            3 -> lShapedDoors
            4 -> {
                if (room1x4Names.contains(room.data.name)) fourByOneDoors else twoByTwoDoors
            }
            else -> listOf()
        }
    }

    fun findRoomDoors(room: Room): Door? {
        val possibleDoors = getRoomDoors(room).map { room.getRealCoordsOdin(it) }
        val doors = possibleDoors.filter {
            !isAir(it)
        }
        val doorPos = doors.firstOrNull {it != currentDoor?.pos} ?: return null
        val roomRouteName = "${possibleDoors.indexOf(currentDoor?.pos)}>${possibleDoors.indexOf(doorPos)}"

        val routes = routes.getOrPut(room.data.name) {mutableMapOf()}.getOrPut(roomRouteName) {mutableListOf()}.toList()
        routes.forEach { it.convertToReal(room) }
        activeRoutes.addAll(routes)
        devMessage(roomRouteName)
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
        if (event.isEnd || activeRoutes.isEmpty() || !PlayerUtils.canSendC08) return
        val node = activeRoutes.firstOrNull { it.inNode() } ?: return
        node.runTick()
        Scheduler.schedulePreMotionUpdateTask {
            node.runMotion(it as MotionUpdateEvent.Pre)
        }
    }


    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent){
        if (!renderDoorNumbers) return
        val room = DungeonUtils.currentRoom ?: return

        val doorPositions = if (room.data.name == "Entrance") oneByOneDoors.map { room.getRealCoordsOdin(it) } else getRoomDoors(room).map { room.getRealCoordsOdin(it) }
        doorPositions.forEachIndexed { index, pos ->
            Renderer.drawStringInWorld(index.toString(), pos.toVec3().add(0.5, 2.0, 0.5), doorNumberColor, scale = 0.1f)
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

    //var routes = mutableMapOf<String, MutableMap<String, MutableList<BloodRushRoute>>>()
    fun saveToFile(){
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
        saveToFile()
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
            "clearactive" -> {
                activeRoutes.clear()
            }
            "add" -> {
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
                    return
                }


            }
        }
    }



}