package noobroutes.utils.skyblock.dungeonScanning

import net.minecraft.block.BlockSkull
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.network.play.server.S38PacketPlayerListItem
import net.minecraft.tileentity.TileEntitySkull
import net.minecraft.util.BlockPos
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.Core.mc
import noobroutes.events.BossEventDispatcher.inBoss
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.RoomEnterEvent
import noobroutes.utils.*
import noobroutes.utils.skyblock.*
import noobroutes.utils.skyblock.dungeonScanning.tiles.Room
import noobroutes.utils.skyblock.dungeonScanning.tiles.Rotations
import noobroutes.utils.skyblock.dungeonScanning.tiles.UniqueRoom
import kotlin.math.floor
import kotlin.math.roundToLong

object DungeonUtils {

    inline val inDungeons: Boolean
        get() = LocationUtils.currentArea.isArea(Island.Dungeon)

    inline val floorNumber: Int
        get() = LocationUtils.currentDungeon?.floor?.floorNumber ?: 0

    inline val floor: Floor
        get() = LocationUtils.currentDungeon?.floor ?: Floor.E

    inline val puzzles: List<Puzzle>
        get() = LocationUtils.currentDungeon?.puzzles.orEmpty()

    inline val currentRoomName: String
        get() = LocationUtils.currentDungeon?.currentRoom?.name ?: "Unknown"

    inline val dungeonTeammates: ArrayList<DungeonPlayer>
        get() = LocationUtils.currentDungeon?.dungeonTeammates ?: ArrayList()

    inline val dungeonTeammatesNoSelf: ArrayList<DungeonPlayer>
        get() = LocationUtils.currentDungeon?.dungeonTeammatesNoSelf ?: ArrayList()

    inline val leapTeammates: ArrayList<DungeonPlayer>
        get() = LocationUtils.currentDungeon?.leapTeammates ?: ArrayList()

    inline val currentDungeonPlayer: DungeonPlayer
        get() = dungeonTeammates.find { it.name == mc.thePlayer?.name } ?: DungeonPlayer(mc.thePlayer?.name ?: "Unknown", DungeonClass.Unknown, 0, entity = mc.thePlayer)

    inline val currentRoom: UniqueRoom?
        get() = LocationUtils.currentDungeon?.currentRoom


    /**
     * Checks if the current dungeonScanning floor number matches any of the specified options.
     *
     * @param options The floor number options to compare with the current dungeonScanning floor.
     * @return `true` if the current dungeonScanning floor matches any of the specified options, otherwise `false`.
     */
    fun isFloor(vararg options: Int): Boolean {
        return floorNumber in options
    }

    /**
     * Gets the current phase of floor 7 boss.
     *
     * @return The current phase of floor 7 boss, or `null` if the player is not in the boss room.
     */
    fun getF7Phase(): M7Phases {
        if ((!isFloor(7) || !inBoss) && LocationUtils.isOnHypixel) return M7Phases.Unknown

        return when {
            PlayerUtils.posY > 210 -> M7Phases.P1
            PlayerUtils.posY > 155 -> M7Phases.P2
            PlayerUtils.posY > 100 -> M7Phases.P3
            PlayerUtils.posY > 45 -> M7Phases.P4
            else -> M7Phases.P5
        }
    }

    fun getMageCooldownMultiplier(): Double {
        return if (currentDungeonPlayer.clazz != DungeonClass.Mage) 1.0
        else 1 - 0.25 - (floor(currentDungeonPlayer.clazzLvl / 2.0) / 100) * if (dungeonTeammates.count { it.clazz == DungeonClass.Mage } == 1) 2 else 1
    }



    /**
     * Gets the new ability cooldown after mage cooldown reductions.
     * @param baseSeconds The base cooldown of the ability in seconds. Eg 10
     * @return The new time
     */
    fun getAbilityCooldown(baseSeconds: Long): Long {
        return (baseSeconds * getMageCooldownMultiplier()).roundToLong()
    }

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Receive) {
        if (inDungeons) LocationUtils.currentDungeon?.onPacket(event)
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onRoomEnter(event: RoomEnterEvent) {
        if (inDungeons) LocationUtils.currentDungeon?.enterDungeonRoom(event.room)
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        LocationUtils.currentDungeon?.onWorldLoad()
    }

    @SubscribeEvent
    fun onEntityJoin(event: EntityJoinWorldEvent) {
        LocationUtils.currentDungeon?.onEntityJoin(event)
    }

    private val puzzleRegex = Regex("^§r (\\w+(?: \\w+)*|\\?\\?\\?): §r§7\\[(§r§c§l✖|§r§a§l✔|§r§6§l✦)§r§7] ?(?:§r§f\\(§r§[a-z](\\w+)§r§f\\))?§r$")

    fun getDungeonPuzzles(list: List<String> = listOf()): List<Puzzle> {
        return list.mapNotNull { text ->
            val (name, status) = puzzleRegex.find(text)?.destructured ?: return@mapNotNull null
            val puzzle = Puzzle.allPuzzles.find { it.name == name }?.copy() ?: return@mapNotNull null

            puzzle.status = when {
                puzzles.find { it.name == puzzle.name }?.status == PuzzleStatus.Completed -> PuzzleStatus.Completed
                status == "§r§c§l✖" -> PuzzleStatus.Failed
                status == "§r§a§l✔" -> PuzzleStatus.Completed
                status == "§r§6§l✦" -> PuzzleStatus.Incomplete
                else -> {
                    modMessage(text.replace("§", "&"))
                    return@mapNotNull null
                }
            }
            puzzle
        }
    }

    private val tablistRegex = Regex("^\\[(\\d+)] (?:\\[\\w+] )*(\\w+) .*?\\((\\w+)(?: (\\w+))*\\)$")
    var customLeapOrder: List<String> = emptyList()

    fun getDungeonTeammates(previousTeammates: ArrayList<DungeonPlayer>, tabList: List<S38PacketPlayerListItem.AddPlayerData>): ArrayList<DungeonPlayer> {
        for (line in tabList) {
            val displayName = line.displayName?.unformattedText?.noControlCodes ?: continue
            val (_, name, clazz, clazzLevel) = tablistRegex.find(displayName)?.destructured ?: continue

            previousTeammates.find { it.name == name }?.let { player -> player.isDead = clazz == "DEAD" } ?:
            previousTeammates.add(DungeonPlayer(name, DungeonClass.entries.find { it.name == clazz } ?: continue, clazzLvl = _root_ide_package_.noobroutes.utils.romanToInt(
                clazzLevel
            ), mc.netHandler?.getPlayerInfo(name)?.locationSkin ?: continue, mc.theWorld?.getPlayerEntityByName(name), false))
        }
        return previousTeammates
    }

    const val WITHER_ESSENCE_ID = "e0f3e929-869e-3dca-9504-54c666ee6f23"
    private const val REDSTONE_KEY = "fed95410-aba1-39df-9b95-1d4f361eb66e"

    /**
     * Determines whether a given block state and position represent a secret location.
     *
     * This function checks if the specified block state and position correspond to a secret location based on certain criteria.
     * It considers blocks such as chests, trapped chests, and levers as well as player skulls with a specific player profile ID.
     *
     * @param state The block state to be evaluated for secrecy.
     * @param pos The position (BlockPos) of the block in the world.
     * @return `true` if the specified block state and position indicate a secret location, otherwise `false`.
     */
    fun isSecret(state: IBlockState, pos: BlockPos): Boolean {
        return when {
            state.block.equalsOneOf(Blocks.chest, Blocks.trapped_chest, Blocks.lever) -> true
            state.block is BlockSkull -> (mc.theWorld?.getTileEntity(pos) as? TileEntitySkull)?.playerProfile?.id.toString().equalsOneOf(WITHER_ESSENCE_ID, REDSTONE_KEY)
            else -> false
        }
    }

    fun UniqueRoom.getRelativeCoords(pos: Vec3): Vec3 {
        val center = this.getCenter()
        val x = pos.xCoord - center.x
        val z = pos.zCoord - center.z
        return when (this.rotation) {
            Rotations.NORTH -> {
                Vec3(x, pos.yCoord, z)
            }
            Rotations.WEST -> {
                Vec3(-z, pos.yCoord, x)
            }
            Rotations.SOUTH -> {
                Vec3(-x, pos.yCoord, -z)
            }
            Rotations.EAST -> {
                Vec3(z, pos.yCoord, -x)
            }
            Rotations.NONE -> {
                devMessage("no rotation??????")
                Vec3(x, pos.yCoord, z)
            }
        }
    }





    const val MAX_SAFE_INTEGER: Long = 9007199254740991L
    const val MIN_SAFE_INTEGER = -9007199254740991L

    fun UniqueRoom.getCenter(): Vec2 {
        var minX = MAX_SAFE_INTEGER.toDouble()
        var maxX = MIN_SAFE_INTEGER.toDouble()
        var minZ = MAX_SAFE_INTEGER.toDouble()
        var maxZ = MIN_SAFE_INTEGER.toDouble()
        for (component in this.roomComponents) {
            val x = component.first.x + 0.5
            val z = component.second.z + 0.5
            minX = if (x < minX) x else minX
            maxX = if (x > maxX) x else maxX
            minZ = if (z < minZ) z else minZ
            maxZ = if (z > maxZ) z else maxZ
        }
        return Vec2((minX + maxX) * 0.5, (minZ + maxZ) * 0.5)
    }


    fun UniqueRoom.getRealCoords(pos: Vec3): Vec3 {
        val center = this.getCenter()
        val rotatedPos = when (this.rotation) {
            Rotations.NORTH -> {
                Vec2(pos.xCoord, pos.zCoord)
            }
            Rotations.WEST -> {
                Vec2(pos.zCoord, -pos.xCoord)
            }
            Rotations.SOUTH -> {
                Vec2(-pos.xCoord, -pos.zCoord)
            }
            Rotations.EAST -> {
                Vec2(-pos.zCoord, pos.xCoord)
            }
            Rotations.NONE -> {
                devMessage("no rotation??????")
                Vec2(pos.xCoord, pos.zCoord)
            }
        }
        return Vec3(rotatedPos.x + center.x, pos.yCoord, rotatedPos.z + center.z)

    }

    fun UniqueRoom.getRelativeCoords(x: Double, y: Double, z: Double) = getRelativeCoords(Vec3(x, y, z))

    fun UniqueRoom.getRealYaw(yaw: Float): Float {
        val realYaw = when (this.rotation) {
            Rotations.NORTH -> yaw
            Rotations.WEST -> yaw - 90
            Rotations.SOUTH -> yaw - 180
            Rotations.EAST -> yaw - 270
            else -> yaw
        }
        return MathHelper.wrapAngleTo180_float(realYaw)
    }
    fun UniqueRoom.getRelativeYaw(yaw: Float): Float {
        val relativeYaw = when (this.rotation) {
            Rotations.NORTH -> yaw
            Rotations.WEST -> yaw + 90
            Rotations.SOUTH -> yaw + 180
            Rotations.EAST -> yaw + 270
            else -> yaw
        }
        return MathHelper.wrapAngleTo180_float(relativeYaw)
    }

    fun UniqueRoom.getRelativeCoords(pos: BlockPos) = getRelativeCoords(Vec3(pos.x.toDouble() + 0.5, pos.y.toDouble(), pos.z.toDouble() + 0.5)).toBlockPos()
    fun UniqueRoom.getRealCoords(pos: BlockPos) = getRealCoords(Vec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())).toBlockPos()
    fun UniqueRoom.getRelativeCoords(x: Int, y: Int, z: Int) = getRelativeCoords(Vec3(x.toDouble(), y.toDouble(), z.toDouble())).toBlockPos()
    fun UniqueRoom.getRealCoords(x: Int, y: Int, z: Int) = getRealCoords(Vec3(x.toDouble(), y.toDouble(), z.toDouble())).toBlockPos()

    val dungeonItemDrops = listOf(
        "Health Potion VIII Splash Potion", "Healing Potion 8 Splash Potion", "Healing Potion VIII Splash Potion", "Healing VIII Splash Potion", "Healing 8 Splash Potion",
        "Decoy", "Inflatable Jerry", "Spirit Leap", "Trap", "Training Weights", "Defuse Kit", "Dungeon Chest Key", "Treasure Talisman", "Revive Stone", "Architect's First Draft"
    )
}