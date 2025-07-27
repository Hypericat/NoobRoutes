package noobroutes.features.floor7.autop3

import noobroutes.features.floor7.autop3.rings.*

enum class RingType(val ringName: String, val clazz: Class<out Ring>, val commandGenerated: Boolean = true, val canSave: Boolean = true) {
    BLINK("Blink", BlinkRing::class.java, commandGenerated = false),
    BLINK_WAYPOINT("Blink_Waypoint", BlinkWaypoint::class.java, commandGenerated = false, canSave = false),
    BOOM("Boom", BoomRing::class.java),
    CLAMP("Clamp", ClampRing::class.java),
    H_CLIP("HClip", HClipRing::class.java),
    JUMP("Jump", JumpRing::class.java),
    LAVA_CLIP("LavaClip", LavaClipRing::class.java),
    MOTION("Motion", MotionRing::class.java),
    SPEED("Speed", SpedRing::class.java),
    STOP("Stop", StopRing::class.java),
    WALK("Walk", WalkRing::class.java);


    companion object {
        fun getTypeFromName(name: String): RingType? {
            return entries.firstOrNull {it.name == name}
        }
    }
}