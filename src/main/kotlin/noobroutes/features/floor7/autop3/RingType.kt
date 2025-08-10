package noobroutes.features.floor7.autop3

import noobroutes.features.floor7.autop3.rings.*
import kotlin.reflect.KClass

enum class RingType(
    val ringName: String,
    val ringClass: KClass<out Ring>,
    val commandGenerated: Boolean = true,
    val canSave: Boolean = true,
    val aliases: List<String> = listOf()
) {
    BLINK("Blink", BlinkRing::class, commandGenerated = true),
    BLINK_WAYPOINT("Blink_Waypoint", BlinkWaypoint::class, commandGenerated = false, canSave = false),
    BOOM("Boom", BoomRing::class),
    CLAMP("Clamp", ClampRing::class),
    H_CLIP("HClip", HClipRing::class),
    JUMP("Jump", JumpRing::class),
    LAVA_CLIP("LavaClip", LavaClipRing::class, aliases = listOf("lava")),
    MOTION("Motion", MotionRing::class),
    SPEED("Speed", SpedRing::class, commandGenerated = false, aliases = listOf("sped", "tickshift")),
    STOP("Stop", StopRing::class),
    WALK("Walk", WalkRing::class),
    COMMAND("Command", CommandRing::class, aliases = listOf("cmd"));


    companion object {
        fun getTypeFromName(name: String): RingType? {
            return entries.firstOrNull { it.ringName.equals(name, ignoreCase = true) || it.aliases.any { alias -> alias.equals(
                name, ignoreCase = true) }}
        }
    }
}