package me.modcore.events.impl

import net.minecraftforge.fml.common.eventhandler.Event

open class MotionUpdateEvent(
    @JvmField var x: Double,
    @JvmField var y: Double,
    @JvmField var z: Double,
    @JvmField var motionX: Double,
    @JvmField var motionY: Double,
    @JvmField var motionZ: Double,
    @JvmField var yaw: Float,
    @JvmField var pitch: Float,
    @JvmField var onGround: Boolean
) : Event() {

    class Pre(
        x: Double,
        y: Double,
        z: Double,
        motionX: Double,
        motionY: Double,
        motionZ: Double,
        yaw: Float,
        pitch: Float,
        onGround: Boolean
    ) : MotionUpdateEvent(x, y, z, motionX, motionY, motionZ, yaw, pitch, onGround)

    class Post(
        x: Double,
        y: Double,
        z: Double,
        motionX: Double,
        motionY: Double,
        motionZ: Double,
        yaw: Float,
        pitch: Float,
        onGround: Boolean
    ) : MotionUpdateEvent(x, y, z, motionX, motionY, motionZ, yaw, pitch, onGround)
}