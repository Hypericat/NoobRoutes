package noobroutes.utils

import net.minecraft.entity.Entity
import net.minecraft.util.Vec3

class EntityPosition(
    var x: Double,
    var y: Double,
    var z: Double,
    var yaw: Float,
    var pitch: Float,
) {
    var lastX: Double = x
    var lastY: Double = y
    var lastZ: Double = z
    var prevX: Double = x
    var prevY: Double = y
    var prevZ: Double = z
    var prevYaw: Float = yaw
    var prevPitch: Float = pitch

    constructor(entity: Entity, full: Boolean) : this(entity.posX, entity.posY, entity.posZ, entity.rotationYaw, entity.rotationPitch) {
        if (full) {
            lastX = entity.lastTickPosX
            lastY = entity.lastTickPosY
            lastZ = entity.lastTickPosZ

            prevX = entity.prevPosX
            prevY = entity.prevPosY
            prevZ = entity.prevPosZ

            prevPitch = entity.prevRotationPitch
            prevYaw = entity.prevRotationYaw
        }

    }

    fun copyFromEntity(entity: Entity, full: Boolean = false) {
        x = entity.posX
        y = entity.posY
        z = entity.posZ
        yaw = entity.rotationYaw
        pitch = entity.rotationPitch

        if (full) {
            lastX = entity.lastTickPosX
            lastY = entity.lastTickPosY
            lastZ = entity.lastTickPosZ
            prevX = entity.prevPosX
            prevY = entity.prevPosY
            prevZ = entity.prevPosZ
            prevPitch = entity.prevRotationPitch
            prevYaw = entity.prevRotationYaw
        } else {
            setDefault()
        }
    }

    fun copyToEntity(entity: Entity, full: Boolean = false) {
        entity.setPositionAndRotation(x, y, z, yaw, pitch)
        if (full) {
            entity.lastTickPosX = lastX
            entity.lastTickPosY = lastY
            entity.lastTickPosZ = lastZ
            entity.prevPosX = prevX
            entity.prevPosY = prevY
            entity.prevPosZ = prevZ
            entity.prevRotationPitch = prevPitch
            entity.prevRotationYaw = prevYaw
        } else {
            entity.lastTickPosX = x
            entity.lastTickPosY = y
            entity.lastTickPosZ = z
            entity.prevPosX = x
            entity.prevPosY = y
            entity.prevPosZ = z
            entity.prevRotationPitch = pitch
            entity.prevRotationYaw = yaw
        }
    }

    private fun setDefault() {
        lastX = x
        lastY = y
        lastZ = z
        prevX = x
        prevY = y
        prevZ = z
        prevPitch = pitch
        prevYaw = yaw
    }

    fun add(vec3: Vec3) {
        add(vec3.xCoord, vec3.yCoord, vec3.zCoord)
    }

    fun add(entityPosition: EntityPosition) {
        add(entityPosition.x, entityPosition.y, entityPosition.z)
    }

    fun add(x: Double, y: Double, z: Double){
        this.x += x
        this.y += y
        this.z += z
    }

    fun add(mutableVec3: MutableVec3) {
        this.x += mutableVec3.x
        this.y += mutableVec3.y
        this.z += mutableVec3.z
    }

}