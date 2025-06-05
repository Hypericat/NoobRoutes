package noobroutes.utils

import net.minecraft.util.BlockPos
import net.minecraft.util.MathHelper
import net.minecraft.util.Vec3
import net.minecraft.util.Vec3i
import kotlin.math.sqrt

class MutableVec3(var x: Double, var y: Double, var z: Double) {
    constructor(vec3: Vec3) : this(vec3.xCoord, vec3.yCoord, vec3.zCoord)
    constructor(blockPos: BlockPos) : this(blockPos.x, blockPos.y, blockPos.z)
    constructor(vec3i: Vec3i) : this(vec3i.x, vec3i.y, vec3i.z)
    constructor(x: Number, y: Number, z: Number) : this(
        x.toDouble(), y.toDouble(), z.toDouble()
    )

    val length get() = sqrt(this.x * this.x + this.y * this.y + this.z * this.z)

    fun normalize(): MutableVec3 {
        val d0 =
            MathHelper.sqrt_double(this.x * this.x + this.y * this.y + this.z * this.z)
                .toDouble()
        return if (d0 < 1.0E-4) MutableVec3(0.0, 0.0, 0.0) else MutableVec3(this.x / d0, this.y / d0, this.z / d0)
    }

    fun toBlockPos(): BlockPos {
        return BlockPos(this.x, this.y, this.z)
    }
    fun add(mutableVec3: MutableVec3): MutableVec3 {
        return add(mutableVec3.x, mutableVec3.y, mutableVec3.z)
    }

    fun add(vec3i: Vec3i): MutableVec3 {
        return add(vec3i.x, vec3i.y, vec3i.z)
    }
    fun add(blockPos: BlockPos): MutableVec3 {
        return add(blockPos.x, blockPos.y, blockPos.z)
    }
    fun add(vec3: Vec3): MutableVec3 {
        return add(vec3.xCoord, vec3.yCoord, vec3.zCoord)
    }
    fun add(x: Number, y: Number, z: Number): MutableVec3 {
        this.x += x.toDouble()
        this.y += y.toDouble()
        this.z += z.toDouble()
        return this
    }

    fun scale(scale: Number): MutableVec3 {
        return scale(scale, scale, scale)
    }

    fun scale(x: Number, y: Number, z: Number): MutableVec3 {
        this.x *= x.toDouble()
        this.y *= y.toDouble()
        this.z *= z.toDouble()
        return this
    }





}