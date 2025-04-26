package noobroutes.utils.skyblock.dungeon

import noobroutes.Core.mc
import noobroutes.utils.*
import net.minecraft.block.BlockSkull
import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.network.play.server.S38PacketPlayerListItem
import net.minecraft.tileentity.TileEntitySkull
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.event.entity.EntityJoinWorldEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.floor
import kotlin.math.roundToLong

object DungeonUtils {

    inline val inDungeons: Boolean
        get() = _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentArea.isArea(
            _root_ide_package_.noobroutes.utils.skyblock.Island.Dungeon)

    inline val floorNumber: Int
        get() = _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.floor?.floorNumber ?: 0

    inline val floor: Floor
        get() = _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.floor ?: Floor.E

    inline val inBoss: Boolean
        get() = _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.inBoss == true

    inline val secretCount: Int
        get() = _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.dungeonStats?.secretsFound ?: 0

    inline val knownSecrets: Int
        get() = _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.dungeonStats?.knownSecrets ?: 0

    inline val secretPercentage: Float
        get() = _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.dungeonStats?.secretsPercent ?: 0f

    inline val totalSecrets: Int
        get() = if (secretCount == 0 || secretPercentage == 0f) 0 else floor(100 / secretPercentage * secretCount + 0.5).toInt()

    inline val deathCount: Int
        get() = _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.dungeonStats?.deaths ?: 0

    inline val cryptCount: Int
        get() = _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.dungeonStats?.crypts ?: 0

    inline val openRoomCount: Int
        get() = _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.dungeonStats?.openedRooms ?: 0

    inline val completedRoomCount: Int
        get() = _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.dungeonStats?.completedRooms ?: 0

    inline val percentCleared: Int
        get() = _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.dungeonStats?.percentCleared ?: 0

    inline val totalRooms: Int
        get() = if (completedRoomCount == 0 || percentCleared == 0) 0 else floor((completedRoomCount / (percentCleared * 0.01).toFloat()) + 0.4).toInt()

    inline val puzzles: List<Puzzle>
        get() = _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.puzzles.orEmpty()

    inline val puzzleCount: Int
        get() = _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.puzzles?.size ?: 0

    inline val dungeonTime: String
        get() = _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.dungeonStats?.elapsedTime ?: "00m 00s"

    inline val isGhost: Boolean
        get() = _root_ide_package_.noobroutes.utils.skyblock.getItemSlot("Haunt", true) != null

    inline val currentRoomName: String
        get() = _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.currentRoom?.data?.name ?: "Unknown"

    inline val dungeonTeammates: ArrayList<DungeonPlayer>
        get() = _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.dungeonTeammates ?: ArrayList()

    inline val dungeonTeammatesNoSelf: ArrayList<DungeonPlayer>
        get() = _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.dungeonTeammatesNoSelf ?: ArrayList()

    inline val leapTeammates: ArrayList<DungeonPlayer>
        get() = _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.leapTeammates ?: ArrayList()

    inline val currentDungeonPlayer: DungeonPlayer
        get() = dungeonTeammates.find { it.name == mc.thePlayer?.name } ?: DungeonPlayer(mc.thePlayer?.name ?: "Unknown", DungeonClass.Unknown, 0, entity = mc.thePlayer)

    inline val doorOpener: String
        get() = _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.dungeonStats?.doorOpener ?: "Unknown"

    inline val mimicKilled: Boolean
        get() = _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.dungeonStats?.mimicKilled == true

    inline val currentRoom: noobroutes.utils.skyblock.dungeon.tiles.Room?
        get() = _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.currentRoom

    inline val passedRooms: Set<noobroutes.utils.skyblock.dungeon.tiles.Room>
        get() = _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.passedRooms.orEmpty()

    inline val isPaul: Boolean
        get() = _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.paul == true



    inline val bloodDone: Boolean
        get() = _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.dungeonStats?.bloodDone == true


    /**
     * Checks if the current dungeon floor number matches any of the specified options.
     *
     * @param options The floor number options to compare with the current dungeon floor.
     * @return `true` if the current dungeon floor matches any of the specified options, otherwise `false`.
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
        if ((!isFloor(7) || !inBoss) && _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.isOnHypixel) return M7Phases.Unknown

        return when {
            _root_ide_package_.noobroutes.utils.skyblock.PlayerUtils.posY > 210 -> M7Phases.P1
            _root_ide_package_.noobroutes.utils.skyblock.PlayerUtils.posY > 155 -> M7Phases.P2
            _root_ide_package_.noobroutes.utils.skyblock.PlayerUtils.posY > 100 -> M7Phases.P3
            _root_ide_package_.noobroutes.utils.skyblock.PlayerUtils.posY > 45 -> M7Phases.P4
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
    fun onPacket(event: noobroutes.events.impl.PacketEvent.Receive) {
        if (inDungeons) _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.onPacket(event)
    }

    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onRoomEnter(event: noobroutes.events.impl.RoomEnterEvent) {
        if (inDungeons) _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.enterDungeonRoom(event)
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.onWorldLoad()
    }

    @SubscribeEvent
    fun onEntityJoin(event: EntityJoinWorldEvent) {
        _root_ide_package_.noobroutes.utils.skyblock.LocationUtils.currentDungeon?.onEntityJoin(event)
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
                    _root_ide_package_.noobroutes.utils.skyblock.modMessage(text.replace("§", "&"))
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

    fun noobroutes.utils.skyblock.dungeon.tiles.Room.getRelativeCoords(pos: Vec3) = pos.subtractVec(x = clayPos.x, z = clayPos.z).rotateToNorth(rotation)
    /*
    (15, 58, 9)    (-138, 99, -136)
fun Vec3.rotateAroundNorth(rotation: Rotations): Vec3 =
    when (rotation) {
        Rotations.NORTH -> Vec3(-this.xCoord, this.yCoord, -this.zCoord)
        Rotations.WEST ->  Vec3(-this.zCoord, this.yCoord, this.xCoord)
        Rotations.SOUTH -> Vec3(this.xCoord, this.yCoord, this.zCoord)
        Rotations.EAST ->  Vec3(this.zCoord, this.yCoord, -this.xCoord)
        else -> this
    }
     */


    fun noobroutes.utils.skyblock.dungeon.tiles.Room.getRealCoords(pos: Vec3) = pos.rotateAroundNorth(rotation).addVec(x = clayPos.x, z = clayPos.z)
    fun noobroutes.utils.skyblock.dungeon.tiles.Room.getRelativeCoords(pos: BlockPos) = getRelativeCoords(Vec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())).toBlockPos()
    fun noobroutes.utils.skyblock.dungeon.tiles.Room.getRealCoords(pos: BlockPos) = getRealCoords(Vec3(pos.x.toDouble(), pos.y.toDouble(), pos.z.toDouble())).toBlockPos()
    fun noobroutes.utils.skyblock.dungeon.tiles.Room.getRelativeCoords(x: Int, y: Int, z: Int) = getRelativeCoords(Vec3(x.toDouble(), y.toDouble(), z.toDouble())).toBlockPos()
    fun noobroutes.utils.skyblock.dungeon.tiles.Room.getRealCoords(x: Int, y: Int, z: Int) = getRealCoords(Vec3(x.toDouble(), y.toDouble(), z.toDouble())).toBlockPos()

    val dungeonItemDrops = listOf(
        "Health Potion VIII Splash Potion", "Healing Potion 8 Splash Potion", "Healing Potion VIII Splash Potion", "Healing VIII Splash Potion", "Healing 8 Splash Potion",
        "Decoy", "Inflatable Jerry", "Spirit Leap", "Trap", "Training Weights", "Defuse Kit", "Dungeon Chest Key", "Treasure Talisman", "Revive Stone", "Architect's First Draft"
    )
}