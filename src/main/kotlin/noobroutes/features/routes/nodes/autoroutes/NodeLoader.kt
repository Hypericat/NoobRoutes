package noobroutes.features.routes.nodes.autoroutes


import com.google.gson.JsonObject
import net.minecraft.util.Vec3
import noobroutes.features.routes.nodes.AutorouteNode
import noobroutes.utils.json.JsonUtils.asVec3
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom

interface NodeLoader {
    fun loadNodeInfo(obj: JsonObject): AutorouteNode
    fun generateFromArgs(args: Array<out String>, room: UniqueRoom): AutorouteNode?

    fun JsonObject.getCoords(): Vec3 {
        return this.get("position").asVec3
    }

    fun getCoords(room: UniqueRoom): Vec3{
        return room.getRelativeCoords(
            kotlin.math.floor(PlayerUtils.posX) + 0.5,
            kotlin.math.floor(PlayerUtils.posY),
            kotlin.math.floor(PlayerUtils.posZ) + 0.5
        )
    }
}