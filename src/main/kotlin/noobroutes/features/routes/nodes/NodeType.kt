package noobroutes.features.routes.nodes

import noobroutes.features.routes.nodes.autoroutes.*

enum class NodeType(val displayName: String, val loader: NodeLoader) {
    ETHERWARP("Etherwarp", Etherwarp.Companion),
    AOTV("Aotv", Aotv.Companion),
    BAT("Bat", Bat.Companion),
    BOOM("Boom", Boom.Companion),
    PEARL("Pearl", Pearl.Companion),
    PEARL_CLIP("PearlClip", PearlClip.Companion),
    USE_ITEM("UseItem", UseItem.Companion),
    WALK("Walk", Walk.Companion),
    DYNAMIC("DynamicNode", DynamicNode.Companion);
    companion object {
        fun getFromName(name: String) : NodeType? {
            return entries.firstOrNull {it.displayName == name}
        }
    }
}
