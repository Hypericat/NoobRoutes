package noobroutes.features.routes.nodes.autoroutes


import com.google.gson.JsonObject
import noobroutes.features.routes.nodes.AutorouteNode
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom

interface NodeLoader {
    fun loadNodeInfo(obj: JsonObject): AutorouteNode
    fun generateFromArgs(args: Array<out String>, room: UniqueRoom): AutorouteNode?

}