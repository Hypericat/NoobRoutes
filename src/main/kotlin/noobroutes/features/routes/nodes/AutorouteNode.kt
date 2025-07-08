package noobroutes.features.routes.nodes

import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.features.routes.AutoRoute
import noobroutes.utils.Utils.containsOneOf
import noobroutes.utils.json.JsonUtils.addProperty
import noobroutes.utils.json.JsonUtils.asVec3
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import kotlin.math.abs

abstract class AutorouteNode(
    pos: Vec3,
    var awaitSecrets: Int = 0,
    var delay: Long = 0,
    var center: Boolean = false,
    var stop: Boolean = false,
    var chain: Boolean = false,
    var reset: Boolean = false
) : Node(pos) {

    var currentRoom: UniqueRoom?

    var delayTriggered = false
    var secretTriggered = false
    var centerTriggered = false
    var resetTriggered = false

    companion object {
        fun getGeneralNodeArgsFromObj(obj: JsonObject): GeneralNodeArgs {
            val pos = obj.get("position").asVec3
            val awaitSecrets = obj.get("secrets")?.asInt ?: 0
            val delay = obj.get("delay")?.asLong ?: 0L
            val center = obj.has("center")
            val stop = obj.has("stop")
            val chain = obj.has("chain")
            val reset = obj.has("reset")
            return GeneralNodeArgs(pos, awaitSecrets, delay, center, stop, chain, reset)
        }

        fun getGeneralNodeArgs(room: UniqueRoom, args: Array<out String>): GeneralNodeArgs{
            val playerCoords = room.getRelativeCoords(
                kotlin.math.floor(PlayerUtils.posX) + 0.5,
                kotlin.math.floor(PlayerUtils.posY),
                kotlin.math.floor(PlayerUtils.posZ) + 0.5
            )
            val reset = args.containsOneOf("reset", ignoreCase = true)
            val center = args.containsOneOf("center", "align", ignoreCase = true)
            val chain = args.containsOneOf("chain", ignoreCase = true)
            val stop = args.containsOneOf("stop", ignoreCase = true)
            val awaitSecrets =
                args.firstOrNull { it.startsWith("await:", ignoreCase = true) }?.substringAfter("await:")?.toIntOrNull()
                    ?: 0
            val delay =
                args.firstOrNull { it.startsWith("delay:", ignoreCase = true) }?.substringAfter("delay:")?.toLongOrNull()
                    ?: 0L
            return GeneralNodeArgs(playerCoords, awaitSecrets, delay, center, stop, chain, reset)
        }
    }
    data class GeneralNodeArgs(val pos: Vec3, val awaitSecrets: Int, val delay: Long, val center: Boolean, val stop: Boolean, val chain: Boolean, val reset: Boolean)


    init {
        currentRoom = DungeonUtils.currentRoom
    }

    override fun reset() {
        super.reset()
        delayTriggered = false
        secretTriggered = false
        centerTriggered = false
        resetTriggered = false
    }

    fun getAsJsonObject(): JsonObject {
        val obj = JsonObject().apply {
            addProperty("name", getType().displayName)
            addProperty("position", pos)

            if (awaitSecrets > 0) addProperty("secrets", awaitSecrets)
            if (delay > 0) addProperty("delay", delay)
            if (center) addProperty("center", true)
            if (stop) addProperty("stop", true)
            if (chain) addProperty("chain", true)
            if (reset) addProperty("reset", true)
        }
        nodeAddInfo(obj)
        return obj
    }

    override fun render() {
        val room = currentRoom ?: return
        Renderer.drawCylinder(room.getRealCoords(pos.add(Vec3(0.0, 0.03, 0.0))), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, getRenderColor(), getDepth())
    }

    override fun renderIndex(index: Int) {
        val room = currentRoom ?: return
        Renderer.drawStringInWorld(index.toString(), room.getRealCoords(pos).add(Vec3(0.0, 0.3, 0.0)), getRenderColor(), getDepth())
    }

    fun getRealPos(): Vec3? {
        val room = currentRoom ?: return null
        return room.getRealCoords(pos)
    }

    override fun getDepth(): Boolean {
        return AutoRoute.depth;
    }

    override fun isInNode(pos: Vec3): Boolean {
        val room = currentRoom ?: return false;
        val realCoord = room.getRealCoords(this.pos)
        return (
            (
                this.chain
                && abs(pos.xCoord - realCoord.xCoord) < 0.001
                && abs(pos.zCoord - realCoord.zCoord) < 0.001
                && pos.yCoord >= this.pos.yCoord - 0.01
                && pos.yCoord <= this.pos.yCoord + 0.5
            )
            || (!this.chain && realCoord.squareDistanceTo(pos) < 0.25)
        )
    }


    internal abstract val priority: Int
    abstract fun nodeAddInfo(obj: JsonObject)

    abstract fun updateTick()

    open fun setRoom(room: UniqueRoom) {
        this.currentRoom = room;
    }

    override fun getDistanceSq(pos: Vec3): Double {
        val room = currentRoom ?: return -1.0
        return room.getRealCoords(this.pos).squareDistanceTo(pos)
    }

    override fun isSilent() : Boolean {
        return AutoRoute.silent
    }

}