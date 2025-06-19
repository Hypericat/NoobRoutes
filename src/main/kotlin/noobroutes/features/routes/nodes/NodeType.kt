package noobroutes.features.routes.nodes

import noobroutes.features.routes.nodes.autoroutes.Aotv
import noobroutes.features.routes.nodes.autoroutes.Bat
import noobroutes.features.routes.nodes.autoroutes.Boom
import noobroutes.features.routes.nodes.autoroutes.Etherwarp
import noobroutes.features.routes.nodes.autoroutes.Pearl
import noobroutes.features.routes.nodes.autoroutes.PearlClip
import noobroutes.features.routes.nodes.autoroutes.UseItem
import noobroutes.features.routes.nodes.autoroutes.Walk
import kotlin.reflect.KClass

enum class NodeType(val displayName: String, val clazz: KClass<out Node>) {
    ETHERWARP("Etherwarp", Etherwarp::class),
    AOTV("Aotv", Aotv::class),
    BAT("Bat", Bat::class),
    BOOM("Boom", Boom::class),
    PEARL("Pearl", Pearl::class),
    PEARL_CLIP("PearlClip", PearlClip::class),
    USE_ITEM("UseItem", UseItem::class),
    WALK("Walk", Walk::class),
    DYNAMIC("DynamicNode", DynamicNode::class);


    companion object {
        fun getFromName(name: String) : NodeType? {
            return entries.firstOrNull {it.displayName == name}
        }
    }
}