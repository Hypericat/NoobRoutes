package noobroutes.features.floor7.autop3

import noobroutes.features.floor7.autop3.rings.BlinkRing
import noobroutes.features.floor7.autop3.rings.HClipRing

enum class RingType(val ringName: String, val clazz: Class<out Ring>, val commandGenerated: Boolean = true, val canSave: Boolean = true) {
    H_CLIP("HClip", HClipRing::class.java),
    BLINK("Blink", BlinkRing::class.java, commandGenerated = false),
    ;


    companion object {
        fun getTypeFromName(name: String): RingType? {
            return entries.firstOrNull {it.name == name}
        }
    }
}