package noobroutes.features.dungeon.autobloodrush

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.Core
import noobroutes.Core.mc
import noobroutes.config.DataManager
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.RoomEnterEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.dungeon.autobloodrush.routes.DoorRoute
import noobroutes.features.dungeon.autobloodrush.routes.Etherwarp
import noobroutes.features.dungeon.autoroute.AutoRoute
import noobroutes.features.dungeon.autoroute.AutoRouteUtils
import noobroutes.features.dungeon.autoroute.AutoRouteUtils.resetRotation
import noobroutes.features.dungeon.autoroute.AutoRouteUtils.serverSneak
import noobroutes.features.misc.EWPathfinderModule
import noobroutes.features.misc.EWPathfinderModule.getEtherPosFromOrigin
import noobroutes.features.move.DynamicRoute
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.ColorSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.pathfinding.GoalXYZ
import noobroutes.pathfinding.Path
import noobroutes.pathfinding.PathFinder
import noobroutes.pathfinding.PathNode
import noobroutes.utils.*
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.*
import noobroutes.utils.skyblock.EtherWarpHelper.EYE_HEIGHT
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayerSq
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoordsOdin
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import noobroutes.utils.skyblock.dungeon.ScanUtils
import noobroutes.utils.skyblock.dungeon.tiles.Room
import noobroutes.utils.skyblock.dungeon.tiles.RoomState
import noobroutes.utils.skyblock.dungeon.tiles.Rotations
import kotlin.math.floor

object AutoBloodRush : Module("Auto Blood Rush", description = "Autoroutes for bloodrushing", category = Category.DUNGEON) {


    val silent by BooleanSetting("Silent", default = true, description = "Server side rotations")
    private val pathfind by BooleanSetting("Pathfind", default = false, description = "path as u br")
    val further by BooleanSetting("go further", default = false, description = "clip further")
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
    private val placeDoors by BooleanSetting("Place Doors", description = "Places doors at all possible locations in a room").withDependency { editMode }



    var routes = mutableMapOf<String, MutableMap<String, MutableList<BloodRushRoute>>>()
    data class Door(val pos: BlockPos, val rotation: Rotations)

    val oneByOneDoors = listOf(
        BlockPos(-1, 69, 15),
        BlockPos(15, 69, -1),
        BlockPos(31, 69, 15), // added this for testing(its a real coord)
        BlockPos(15, 69, 31)
    )
    val twoByOneDoors =  listOf(
        BlockPos(-1, 69, 15),
        BlockPos(15, 69, -1),
        BlockPos(47, 69, -1),
        BlockPos(63, 69, 15),
        BlockPos(47, 69, 31),
        BlockPos(15, 69, 31),
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
        BlockPos(15, 69, -1),
        BlockPos(31, 69, 15),
        BlockPos(47, 69, 31),
        BlockPos(63, 69, 47),
        BlockPos(47, 69, 63),
        BlockPos(15, 69, 63),
        BlockPos(-1, 69, 47),
        BlockPos(-1, 69, 15)
    )

    val twoByTwoDoors = listOf(
        BlockPos(15, 69, -1),
        BlockPos(47, 69, -1),
        BlockPos(63, 69, 15),
        BlockPos(63, 69, 47),
        BlockPos(47, 69, 63),
        BlockPos(15, 69, 63),
        BlockPos(-1, 69, 47),
        BlockPos(-1, 69, 15),
    )

    val threeByOneSpots = mapOf(
        0 to Pair(BlockPos(93, 68, 14), BlockPos(93, 68, 16)),
        1 to Pair(BlockPos(80, 68, 29), BlockPos(78, 68, 29)),
        2 to Pair(BlockPos(48, 68, 29), BlockPos(46, 68, 29)),
        3 to Pair(BlockPos(16, 68, 29), BlockPos(14, 68, 29)),
        4 to Pair(BlockPos(1, 68, 16), BlockPos(1, 68, 14)),
        5 to Pair(BlockPos(14, 68, 1), BlockPos(16, 68, 1)),
        6 to Pair(BlockPos(46, 68, 1), BlockPos(48, 68, 1)),
        7 to Pair(BlockPos(78, 68, 1), BlockPos(80, 68, 1)),
    )

    val lShapedSpots = mapOf(
        0 to Pair(BlockPos(14, 68, 1), BlockPos(16, 68, 1)),
        1 to Pair(BlockPos(29, 68, 14), BlockPos(29, 68, 16)),
        2 to Pair(BlockPos(46, 68, 33), BlockPos(48, 68, 33)),
        3 to Pair(BlockPos(61, 68, 46), BlockPos(61, 68, 48)),
        4 to Pair(BlockPos(48, 68, 61), BlockPos(46, 68, 61)),
        5 to Pair(BlockPos(16, 68, 61), BlockPos(14, 68, 61)),
        6 to Pair(BlockPos(1, 68, 48), BlockPos(1, 68, 46)),
        7 to Pair(BlockPos(1, 68, 16), BlockPos(1, 68, 14))
    )

    val fourByOneSpots = mapOf(
        0 to Pair(BlockPos(1, 68, 16), BlockPos(1, 68, 14)),
        1 to Pair(BlockPos(14, 68, 1), BlockPos(16, 68, 1)),
        2 to Pair(BlockPos(46, 68, 1), BlockPos(48, 68, 1)),
        3 to Pair(BlockPos(78, 68, 1), BlockPos(80, 68, 1)),
        4 to Pair(BlockPos(110, 68, 1), BlockPos(112, 68, 1)),
        5 to Pair(BlockPos(125, 68, 14), BlockPos(125, 68, 16)),
        6 to Pair(BlockPos(112, 68, 29), BlockPos(110, 68, 29)),
        7 to Pair(BlockPos(80, 68, 29), BlockPos(78, 68, 29)),
        8 to Pair(BlockPos(48, 68, 29), BlockPos(46, 68, 29)),
        9 to Pair(BlockPos(16, 68, 29), BlockPos(14, 68, 29))
    )

    val oneByOneSpots = mapOf(
        0 to Pair(BlockPos(1, 68, 16), BlockPos(1, 68, 14)),
        1 to Pair(BlockPos(14, 68, 1), BlockPos(16, 68, 1)),
        2 to Pair(BlockPos(29, 68, 14), BlockPos(29, 68, 16)),
        3 to Pair(BlockPos(16, 68, 29), BlockPos(14, 68, 29))
    )

    val twoByOneSpots = mapOf(
        0 to Pair(BlockPos(1, 68, 16), BlockPos(1, 68, 14)),
        1 to Pair(BlockPos(14, 68, 1), BlockPos(16, 68, 1)),
        2 to Pair(BlockPos(46, 68, 1), BlockPos(48, 68, 1)),
        3 to Pair(BlockPos(61, 68, 14), BlockPos(61, 68, 16)),
        4 to Pair(BlockPos(48, 68, 29), BlockPos(46, 68, 29)),
        5 to Pair(BlockPos(16, 68, 29), BlockPos(14, 68, 29))
    )

    val twoByTwoSpots = mapOf(
        0 to Pair(BlockPos(14, 68, 1), BlockPos(16, 68, 1)),
        1 to Pair(BlockPos(46, 68, 1), BlockPos(48, 68, 1)),
        2 to Pair(BlockPos(61, 68, 14), BlockPos(61, 68, 16)),
        3 to Pair(BlockPos(61, 68, 46), BlockPos(61, 68, 48)),
        4 to Pair(BlockPos(48, 68, 61), BlockPos(46, 68, 61)),
        5 to Pair(BlockPos(16, 68, 61), BlockPos(14, 68, 61)),
        6 to Pair(BlockPos(1, 68, 48), BlockPos(1, 68, 46)),
        7 to Pair(BlockPos(1, 68, 16), BlockPos(1, 68, 14))
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

    val room1x1Names = hashSetOf(
        "Arrow Trap",
        "Banners",
        "Basement",
        "Blue Skulls",
        "Cage",
        "Cell",
        "Duncan",
        "Entrance",
        "Golden Oasis",
        "Jumping Skulls",
        "Leaves",
        "Locked Away",
        "Mirror",
        "Multicolored",
        "Mural",
        "Mushroom",
        "Prison Cell",
        "Silver Sword",
        "Sloth",
        "Steps",
        "Andesite",
        "Beams",
        "Big Red Flag",
        "Cages",
        "Chains",
        "Cobble Wall Pillar",
        "Dip",
        "Dome",
        "Drop",
        "End",
        "Granite",
        "Overgrown Chains",
        "Painting",
        "Perch",
        "Quad Lava",
        "Scaffolding",
        "Slabs",
        "Small Stairs",
        "Water",
        "Black Flag",
        "Double Diamond",
        "Dueces",
        "Knight",
        "Long Hall",
        "Lots Of Floors",
        "Overgrown",
        "Red Green",
        "Redstone Key",
        "Sarcophagus",
        "Spikes",
        "Temple",
        "Logs",
        "Raccoon"
    )

    val room1x2Names = hashSetOf(
        "Gold",
        "Skull",
        "Archway",
        "Grass Ruin",
        "Redstone Warrior",
        "Balcony",
        "Grand Library",
        "Mage",
        "Crypt",
        "Doors",
        "Pedestal",
        "Purple Flags",
        "Bridges",
        "Pressure Plates"
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

    val room1x4Names = hashSetOf(
        "Quartz Knight",
        "Pipes",
        "Pit",
        "Hallway",
        "Waterfall",
        "Mossy"
    )

    val room2x2Names = hashSetOf(
        "Stairs",
        "Buttons",
        "Museum",
        "Atlas",
        "Supertall",
        "Flags",
        "Cathedral",
        "Rails",
        "Mines"
    )

    val roomLShapedNames = hashSetOf(
        "Dino Dig Site",
        "Withermancer",
        "Chambers",
        "Market",
        "Lava Ravine",
        "Melon",
        "Well",
        "Layers",
        "Spider"
    )


    fun getRoomDoors(room: Room): List<BlockPos> {
        return when {
            room1x1Names.contains(room.data.name) -> oneByOneDoors
            room1x2Names.contains(room.data.name) -> twoByOneDoors
            room1x3Names.contains(room.data.name) -> threeByOneDoors
            roomLShapedNames.contains(room.data.name) -> lShapedDoors
            room1x4Names.contains(room.data.name) -> fourByOneDoors
            room2x2Names.contains(room.data.name) -> twoByTwoDoors
            else -> listOf()
        }
    }

    fun getDoorSpots(room: Room): Map<Int, Pair<BlockPos, BlockPos>> {
        return when {
            room1x1Names.contains(room.data.name) -> oneByOneSpots
            room1x2Names.contains(room.data.name) -> twoByOneSpots
            room1x3Names.contains(room.data.name) -> threeByOneSpots
            roomLShapedNames.contains(room.data.name) -> lShapedSpots
            room1x4Names.contains(room.data.name) -> fourByOneSpots
            room2x2Names.contains(room.data.name) -> twoByTwoSpots
            else -> mapOf()
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
        if (possibleDoors.indexOf(currentDoor?.pos) == -1) devMessage(currentDoor!!.pos, "spamspamspamspamspamspamspamspamspamspamspamspamspamspamspamspamspamspamspamspamspamspamspamspamspamspam")
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

    fun getOtherDoor(room: Room): Door? {
        val possibleDoors = getRoomDoors(room).map { room.getRealCoordsOdin(it) }
        val doors = possibleDoors.filter {

            getSkull(it.add(-2, 1, -2))?.skullTexture in skullIds
        }
        val doorPos = doors.firstOrNull {it != getClosestDoorToPlayer(room)?.pos} ?: return null
        val closestComponent = room.roomComponents.minByOrNull { it.blockPos.distanceSq(doorPos) }!!
        val rotation = when {
            doorPos.x < closestComponent.x -> Rotations.EAST
            doorPos.x > closestComponent.x -> Rotations.WEST
            doorPos.z > closestComponent.z -> Rotations.NORTH
            doorPos.z < closestComponent.z -> Rotations.SOUTH
            else -> Rotations.NORTH
        }
        return Door(doorPos, rotation)
    }

    fun getDoorIndex(room: Room, door: Door): Int? {
        val doorList = when {
            room.data.name == "Entrance" -> oneByOneDoors
            room.data.cores.size == 1 -> oneByOneDoors
            room.data.cores.size == 2 -> twoByOneDoors
            room1x3Names.contains(room.data.name) -> threeByOneDoors
            room.data.cores.size == 3 -> lShapedDoors
            room1x4Names.contains(room.data.name) -> fourByOneDoors
            room.data.cores.size == 4 -> twoByTwoDoors
            else -> return null
        }

        return doorList.indexOfFirst { relativePos ->
            val realPos = room.getRealCoordsOdin(relativePos)
            realPos == door.pos
        }.takeIf { it != -1 }
    }

    fun getClosestDoorToPlayer(room: Room): Door? {
        val doorPositions = if (room.data.name == "Entrance") {
            oneByOneDoors
        } else {
            getRoomDoors(room)
        }.map { room.getRealCoordsOdin(it) }
        val closestPos = doorPositions.minByOrNull { it.distanceToPlayerSq } ?: return null
        val closestComponent = room.roomComponents.minByOrNull { it.blockPos.distanceSq(closestPos) } ?: return null
        val rotation = when {
            closestPos.x < closestComponent.x -> Rotations.EAST
            closestPos.x > closestComponent.x -> Rotations.WEST
            closestPos.z > closestComponent.z -> Rotations.NORTH
            closestPos.z < closestComponent.z -> Rotations.SOUTH
            else -> Rotations.NORTH
        }
        return Door(closestPos, rotation)
    }

    var started = false

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        /*started = false
        activeRoutes.clear()
        currentDoor = null
        doors.clear()*/
        started = false
        activeRoutes.clear()
        currentDoor = null
        doors.clear()
        fairyRoom = null
        routeTo = null
        waiting = false
        routeName = ""
        direction = Rotations.NONE
        thrown = 0L
        isSolving = false
        routesToGenerate.clear()
        autoBrUnsneakRegistered = false
        customRoom = null
    }

    private var routeTo : BlockPos? = null
    private var waiting = false
    private var bloodNext = false
    private var customRoom: Room? = null

    @SubscribeEvent
    fun setRoom(event: RoomEnterEvent) {
        customRoom = event.room
        devMessage(customRoom!!.data.name)
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (mc.thePlayer == null) return
        if (event.isEnd && currentDoor != null) {
            scanForDoors()
            if (currentDoor == null) devMessage(doors)
        }
        if (!editMode && pathfind) {
            val room = customRoom ?: return
            if (routeTo == mc.thePlayer.positionVector.subtract(0.0,1.0,0.0).toBlockPos()) {
                if (waiting) return
                val doorNode = DoorRoute(routeTo!!.toVec3(0.5, 1.0, 0.5))
                doorNode.runTick(room)
                Scheduler.schedulePreMotionUpdateTask {
                    doorNode.runMotion(room, it as MotionUpdateEvent.Pre)
                }
                waiting = true
                if (bloodNext) DynamicRoute.clearRoute()
                routeTo = null
                return
            }
            val door = getOtherDoor(room) ?: return
            val index = getDoorIndex(room, door) ?: return
            val spot = getDoorSpots(room)[index]?.second ?: return
            val realSpot = room.getRealCoordsOdin(spot)
            val closestDoor = getClosestDoorToPlayer(room) ?: return
            val closestIndex = getDoorIndex(room, closestDoor)
            val closestSpot = getDoorSpots(room)[closestIndex]?.first ?: return
            val closestRealSpot = room.getRealCoordsOdin(closestSpot)
            if (closestRealSpot == mc.thePlayer.positionVector.subtract(0.0,1.0,0.0).toBlockPos() && routeTo == null) {
                EWPathfinderModule.execute(realSpot.x.toFloat(), realSpot.y.toFloat(), realSpot.z.toFloat())
                if (isBlock(door.pos, Blocks.stained_hardened_clay)) bloodNext = true
                routeTo = realSpot
                waiting = false
            }
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
        if (room.data.name == "Entrance") customRoom = room
        if (editMode) {
            val doorPositions = if (room.data.name == "Entrance") oneByOneDoors.map { room.getRealCoordsOdin(it) } else getRoomDoors(room).map { room.getRealCoordsOdin(it) }
            doorPositions.forEachIndexed { index, pos ->
                Renderer.drawStringInWorld(index.toString(), pos.toVec3().add(0.5, 2.0, 0.5), doorNumberColor, scale = 0.1f)
            }
            val doorSpots = if (room.data.name == "Entrance") oneByOneSpots.map { it.key to Pair(room.getRealCoordsOdin(it.value.first), room.getRealCoordsOdin(it.value.second)) } else
                getDoorSpots(room).map { it.key to Pair(room.getRealCoordsOdin(it.value.first), room.getRealCoordsOdin(it.value.second)) }

            doorSpots.forEach {
                Renderer.drawBlock(it.second.first, Color.RED)
                Renderer.drawBlock(it.second.second, Color.BLUE)
            }


        }
        if (routeName == "" && editMode) return
        val key = if (editMode) routeName else activeRoutes[room.data.name] ?: return
        val nodeList = routes.getOrPut(room.data.name) {mutableMapOf()}.getOrPut(key) {mutableListOf()}.toList()
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
                val routeList = routes.getOrPut(currentRoom.data.name) {mutableMapOf()}.getOrPut(routeName) {mutableListOf()}
                if (!routeList.any { it.name == "Door" }) {
                    val spot = getDoorSpots(currentRoom)[doorNumbers[1]!!] ?: return
                    val real = currentRoom.getRealCoordsOdin(spot.second).toVec3(0.5, 1.0, 0.5)
                    routeList.add(DoorRoute(currentRoom.getRelativeCoords(real)))
                }


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
            "generate" -> {
                val room = DungeonUtils.currentRoom ?: return modMessage("not in room")
                generateRoutes(room)
            }
            "placedoor" -> {
                val room = DungeonUtils.currentRoom ?: return modMessage("not in room")
                val door = getRoomDoors(room).map { room.getRealCoordsOdin(it) }.minByOrNull { it.distanceToPlayerSq } ?: return
                val closestComponent = room.roomComponents.minByOrNull { it.blockPos.distanceSq(door) }!!
                val rotation = when {
                    door.x < closestComponent.x -> Rotations.EAST
                    door.x > closestComponent.x -> Rotations.WEST
                    door.z > closestComponent.z -> Rotations.NORTH
                    door.z < closestComponent.z -> Rotations.SOUTH
                    else -> Rotations.NORTH
                }
                placeWitherDoor(door, rotation)
            }
            "stop" -> {
                routesToGenerate.clear()
            }
        }
    }

    var direction: Rotations = Rotations.NONE
    var thrown = 0L

    data class ExpectedS08(val x: Double, val z: Double, val dir: Int)
    lateinit var clipS08: ExpectedS08



    val blockList = mapOf(
        "minecraft:cobblestone_wall" to listOf(
            BlockPos(2, 0, 2),
            BlockPos(2, 2, 2),
            BlockPos(2, 0, -2),
            BlockPos(2, 2, -2),
            BlockPos(-2, 0, 2),
            BlockPos(-2, 2, 2),
            BlockPos(-2, 0, -2),
            BlockPos(-2, 2, -2)
        ),
        "minecraft:iron_bars" to listOf(
            BlockPos(2, 1, 2),
            BlockPos(2, 1, -2),
            BlockPos(-2, 1, 2),
            BlockPos(-2, 1, -2)
        ),
        "minecraft:stone_brick_stairs" to listOf(
            BlockPos(2, 3, 2),
            BlockPos(2, 3, 1),
            BlockPos(2, 3, -1),
            BlockPos(2, 3, -2),
            BlockPos(-2, 3, 2),
            BlockPos(-2, 3, 1),
            BlockPos(-2, 3, -1),
            BlockPos(-2, 3, -2),
        ),
        "minecraft:stonebrick" to listOf(
            BlockPos(2, 4, 2),
            BlockPos(2, 4, 1),
            BlockPos(2, 4, 0),
            BlockPos(2, 4, -1),
            BlockPos(2, 4, -2),
            BlockPos(1, 0, 2),
            BlockPos(1, 1, 2),
            BlockPos(1, 2, 2),
            BlockPos(1, 3, 2),
            BlockPos(1, 4, 2),
            BlockPos(1, 4, 1),
            BlockPos(1, 4, 0),
            BlockPos(1, 4, -1),
            BlockPos(1, 0, -2),
            BlockPos(1, 1, -2),
            BlockPos(1, 2, -2),
            BlockPos(1, 3, -2),
            BlockPos(1, 4, -2),
            BlockPos(0, 0, 2),
            BlockPos(0, 1, 2),
            BlockPos(0, 2, 2),
            BlockPos(0, 3, 2),
            BlockPos(0, 4, 2),
            BlockPos(0, 4, 1),
            BlockPos(0, 4, 0),
            BlockPos(0, 4, -1),
            BlockPos(0, 0, -2),
            BlockPos(0, 1, -2),
            BlockPos(0, 2, -2),
            BlockPos(0, 3, -2),
            BlockPos(0, 4, -2),
            BlockPos(-1, 0, 2),
            BlockPos(-1, 1, 2),
            BlockPos(-1, 2, 2),
            BlockPos(-1, 3, 2),
            BlockPos(-1, 4, 2),
            BlockPos(-1, 4, 1),
            BlockPos(-1, 4, 0),
            BlockPos(-1, 4, -1),
            BlockPos(-1, 0, -2),
            BlockPos(-1, 1, -2),
            BlockPos(-1, 2, -2),
            BlockPos(-1, 3, -2),
            BlockPos(-1, 4, -2),
            BlockPos(-2, 4, 2),
            BlockPos(-2, 4, 1),
            BlockPos(-2, 4, 0),
            BlockPos(-2, 4, -1),
            BlockPos(-2, 4, -2),
        ),
        "minecraft:stone_slab" to listOf(
            BlockPos(2, 3, 0),
            BlockPos(-2, 3, 0)
        )
    )


    fun placeWitherDoor(pos: BlockPos, rotations: Rotations){
        val rotation = when (rotations) {
            Rotations.WEST -> Rotations.NORTH
            Rotations.EAST -> Rotations.NORTH
            Rotations.NORTH -> Rotations.EAST
            Rotations.SOUTH -> Rotations.EAST
            Rotations.NONE -> return devMessage("error with door rotation")
        }

        
        val list = blockList.toList()
        val placeList = mutableListOf<Pair<String, List<BlockPos>>>()
        list.forEach { (key, value) ->
            placeList.add(Pair(key, value.map { it.toVec3().rotateAroundNorth(rotation).toBlockPos().add(pos) }.toList()))
        }

        val air = Vec3(-2.0, 0.0, -1.0).rotateAroundNorth(rotation).toBlockPos().add(pos)
        val air1 = Vec3(2.0, 3.0, 1.0).rotateAroundNorth(rotation).toBlockPos().add(pos)
        sendChatMessage("/fill ${air.x} ${air.y} ${air.z} ${air1.x} ${air1.y} ${air1.z} air")
        for (blockType in placeList) {
            blockType.second.forEach {
                sendChatMessage("/setblock ${it.x} ${it.y} ${it.z} ${blockType.first}")
            }
        }

    }

    @SubscribeEvent
    fun onRoomEnter(event: RoomEnterEvent){
        if (LocationUtils.isSinglePlayer && editMode && placeDoors && event.room != null) {
            val doors = getRoomDoors(event.room)
            doors.map { event.room.getRealCoordsOdin(it) }.forEach {door ->
                val closestComponent = event.room.roomComponents.minByOrNull { it.blockPos.distanceSq(door) }!!
                val rotation = when {
                    door.x < closestComponent.x -> Rotations.EAST
                    door.x > closestComponent.x -> Rotations.WEST
                    door.z > closestComponent.z -> Rotations.NORTH
                    door.z < closestComponent.z -> Rotations.SOUTH
                    else -> Rotations.NORTH
                }
                placeWitherDoor(door, rotation)
            }
        }
    }




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
        if (!further) {
            Scheduler.scheduleC03Task(0, true) {
                PacketUtils.sendPacket(C06PacketPlayerPosLook(clipS08.x + 2.8 * dx, 70.0, clipS08.z + 2.8 * dz, event.packet.yaw , 90f, true))
                mc.thePlayer.setPosition(clipS08.x + 3.8 * dx + 0.8 * dz, 70.0, clipS08.z + 3.8 * dz - 0.8 * dx)
                Scheduler.schedulePreTickTask {
                    AutoRouteUtils.etherwarp(event.packet.yaw, 90f, silent)
                }
                thrown = 0
            }
        } else {
            mc.thePlayer.setPosition(clipS08.x + 3.8 * dx + 0.8 * dz, 70.0, clipS08.z + 3.8 * dz - 0.8 * dx)
            Scheduler.schedulePreTickTask(1) { //the delay is important wadey. if u remove the delay, ill remove ur heads(bottom one first)
                AutoRouteUtils.etherwarp(event.packet.yaw, 90f, silent)
            }
            thrown = 0
        }

    }

    @Synchronized
    fun onSolve(path: Path, routeName: String){
        val currentRoom = DungeonUtils.currentRoom ?: AutoRoute.roomReplacement
        var lastNode: PathNode? = null
        var node: PathNode? = path.endNode
        while (node != null) {
            if (lastNode != null) {
                val nodeVec3 = Vec3(node.pos.x.toDouble() + 0.5, node.pos.y.toDouble() + 1, node.pos.z + 0.5)
                val targetVec3 : Vec3? = getEtherPosFromOrigin(nodeVec3.add(0.0, EYE_HEIGHT, 0.0), lastNode.yaw, lastNode.pitch)

                if (targetVec3 == null) {
                    System.err.println("Invalid YAW / PITCH : " + lastNode.yaw + " : " + lastNode.pitch)
                    lastNode = node
                    node = node.parent
                    continue
                }

                routes.getOrPut(currentRoom.data.name) {mutableMapOf()}.getOrPut(routeName) {mutableListOf()}.add(
                    Etherwarp(currentRoom.getRelativeCoords(nodeVec3), currentRoom.getRelativeCoords(targetVec3))
                )
            }
            lastNode = node
            node = node.parent
        }
        isSolving = false

    }

    @Volatile
    var isSolving = false

    //pathFindRoute(room.getRealCoordsOdin(doorSpots[door]!!.first), room.getRealCoordsOdin(doorSpots[door1]!!.second))

    fun pathFindRoute(start: BlockPos, end: BlockPos, routeName: String){
        if (isSolving) return
        isSolving = true
        val pathFinder = PathFinder(GoalXYZ(end), start, EWPathfinderModule.ewCost.toDouble(), EWPathfinderModule.perfectPathing,
            EWPathfinderModule.yawStep, EWPathfinderModule.pitchStep, EWPathfinderModule.heuristicThreshold)
        val thread = Thread { onSolve(pathFinder.calculate(), routeName) }
        thread.start()
    }

    val routesToGenerate = mutableListOf<Triple<String, BlockPos, BlockPos>>()
    @SubscribeEvent
    fun pathGeneratorTick(event: TickEvent.ClientTickEvent){
        if (event.isEnd || isSolving) return
        val currentRoom = DungeonUtils.currentRoom ?: return
        val route = routesToGenerate.removeFirstOrNull() ?: return
        routeName = route.first
        val routeList = routes.getOrPut(currentRoom.data.name) {mutableMapOf()}.getOrPut(routeName) {mutableListOf()}
        val routeNumbers = route.first.split(">").map { it.toIntOrNull() ?: return }
        if (!routeList.any { it.name == "Door" }) {
            val spot = getDoorSpots(currentRoom)[routeNumbers[1]] ?: return
            val real = currentRoom.getRealCoordsOdin(spot.second).toVec3(0.5, 1.0, 0.5)
            routeList.add(DoorRoute(currentRoom.getRelativeCoords(real)))
        }

        pathFindRoute(route.second, route.third, route.first)
    }

    fun generateRoutes(room: Room){
        val maxDoors = getRoomDoors(room).size - 1
        val doorSpots = getDoorSpots(room)
        for (door in 0..maxDoors) {
            for (door1 in 0..maxDoors) {
                if (door == door1) continue
                val origin = room.getRealCoordsOdin(doorSpots[door]!!.first)
                if (!isAir(origin.add(0, 1, 0))) continue
                val target = room.getRealCoordsOdin(doorSpots[door1]!!.second)
                if (!isAir(target.add(0, 1, 0))) continue
                
                routeName = "$door>$door1"
                devMessage(routeName)
                routesToGenerate.add(Triple("$door>$door1", origin, target))
            }
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