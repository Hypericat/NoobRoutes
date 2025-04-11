package com.github.wadey3636.noobroutes.utils

import com.github.wadey3636.noobroutes.features.puzzle.WaterBoard
import me.defnotstolen.Core.mc
import me.defnotstolen.events.impl.InputEvent
import me.defnotstolen.events.impl.PacketEvent
import me.defnotstolen.utils.Vec2
import me.defnotstolen.utils.skyblock.modMessage
import me.defnotstolen.utils.toBlockPos
import net.minecraft.client.settings.KeyBinding
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard
import scala.reflect.internal.Types
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Utils {

    fun xPart(yaw: Float): Double {
        return -sin(yaw * Math.PI /180)
    }

    fun zPart(yaw: Float): Double {
        return cos(yaw * Math.PI /180)
    }

    val rat = listOf(
        "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡿⣻⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡏⠉⠻⣿⣿⢿⣿⠿⠛⢻⣟⣛⣩⣵⡾⠋⠁⢹⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣷⠀⣶⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡗⠀⠀⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣶⡿⠿⢿⣿⣿⣿⣿⣿⠋⢿⠛⣿⣿⡀⢾⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣷⣦⣭⣙⠻⢿⣿⣶⣦⣴⣿⣿⣿⣿⣿⣤⣴⣶⣿⣿⣧⣘⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣟⣛⣛⣛⣓⡒⠒⠛⠿⠟⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣯⣭⣛⣛⣛⠻⢿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣭⣭⡍⠀⠀⠀⣾⣿⣿⣏⠉⣹⣿⣿⡿⠋⠙⣿⢻⣿⡦⣭⣙⠻⢿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠛⠋⠁⠀⠀⠀⠀⠘⠻⢿⣥⣍⣛⣿⡆⠀⠀⠉⠈⠉⢻⣦⣝⣻⣶⣭⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡟⠉⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠈⠙⠃⠀⠀⠀⠀⠀⠀⠀⣷⣝⣿⣿⣿⣿⣿⣿⡿⠟⠛⠻⣿⣿⣿⣿⣿",
        "⡿⠿⢿⣿⣿⣿⣿⣿⣿⣿⠿⠟⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣿⣿⣿⣿⣿⣿⣿⣏⠀⠀⠀⠀⠘⣿⣿⣿⣿",
        "⢠⣶⡄⠘⣿⣿⣿⣿⡯⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣿⣿⣿⣿⣿⣿⣿⣿⣆⠀⠀⠀⠀⠈⢿⣿⣿",
        "⣿⣿⡇⠀⢸⣿⣿⡟⠁⠀⠀⠀⠀⠀⠀⠀⠀⢠⣤⣤⡄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠐⣿⣿⣿⣿⣿⣿⣿⣿⣿⣧⠀⠀⠀⠀⠀⢻⣿",
        "⣿⣿⠁⠀⣾⣿⠏⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠉⠉⣿⣦⣀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢘⣿⣿⣿⣿⣿⣿⣿⣿⣿⣀⡀⠀⠀⠀⠀⠹",
        "⣿⠇⠀⣸⣿⡧⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⣸⣿⣿⣷⣦⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠈⠻⣿⣿⣿⣿⣿⣿⣿⣿⣿⣷⡄⠀⠀⠀⠀",
        "⡿⠀⢀⣿⣿⠃⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢠⡟⠉⠛⠻⠿⠋⠱⣄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠻⠿⣿⣿⣿⣿⣿⣿⠿⠿⣿⣶⡆⠀⣸",
        "⡇⠀⢸⣿⡏⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⡼⣷⠶⠾⠿⠶⠶⠶⣿⠂⢀⣤⣤⣄⣤⣄⣀⣶⣦⠀⠀⠰⠿⠿⠃⠀⣰⣿⣷⣌⠻⣿⠆⣽",
        "⠁⠀⣿⣿⣀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢱⣿⣿⣿⣿⡆⢸⣿⣿⡀⢸⣿⣿⣿⣿⣿⣿⣿⣅⢀⣀⣀⡀⠀⣠⣾⣿⣿⣿⣿⣿⣶⣾⣿",
        "⡄⠀⢸⣿⣿⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢸⣿⣿⣿⣿⡇⢸⣿⣿⣧⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⡇⠀⠈⣿⣿⣇⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢸⣿⣿⣿⣿⣿⣼⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⡀⠀⠘⣿⣿⡄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠘⠿⠽⣿⣿⣿⣭⡭⠿⠸⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣷⡀⠀⠈⠻⣿⡆⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣿⣷⡀⠀⠀⠀⠉⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣿⣿⣿⣆⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣠⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣿⣿⣿⣿⣷⣄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣰⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣿⣿⣿⣿⣿⣿⣿⣦⣄⣀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢤⣼⣿⠿⢿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣷⣶⣦⣤⣤⣤⣶⣦⣿⣿⣶⣾⣿⣥⣤⣤⣬⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿"
    )

    fun getYawAndPitch(x: Double, y:Double, z:Double): Pair<Float, Float> { //stolen from CGA
        val dx = x - mc.thePlayer.posX
        val dy = y - (mc.thePlayer.posY + mc.thePlayer.eyeHeight)
        val dz = z - mc.thePlayer.posZ

        val horizontalDistance = sqrt(dx * dx + dz * dz )

        val yaw = Math.toDegrees(atan2(-dx, dz))
        val pitch = -Math.toDegrees(atan2(dy, horizontalDistance))

        val normalizedYaw = if (yaw < -180) yaw + 360 else yaw

        return Pair(normalizedYaw.toFloat(), pitch.toFloat())
    }

    var rotateSpot: Pair<Float, Float>? = null

    fun etherwarpToBlock(coords: Vec3) {
        val spot = getIdealSpot(coords)
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, true)
        rotateSpot = getYawAndPitch(spot.xCoord, spot.yCoord, spot.zCoord)
        modMessage("etherwarping")
        if (mc.isSingleplayer)  {
            ClientUtils.clientScheduleTask(2) { mc.thePlayer.setPosition(coords.xCoord + 0.5, coords.yCoord, coords.zCoord + 0.5) }
            WaterBoard.waitingForS08 = false
        }
    }

    @SubscribeEvent
    fun onC03(event: PacketEvent.Send) {
        if (rotateSpot == null || event.packet !is C03PacketPlayer) return
        modMessage("rotating")
        val rotateTo = rotateSpot
        rotateSpot = null
        event.isCanceled = true
        if (event.packet is C04PacketPlayerPosition || event.packet is C06PacketPlayerPosLook) PacketUtils.sendPacket(C06PacketPlayerPosLook(
            event.packet.positionX,
            event.packet.positionY,
            event.packet.positionZ,
            rotateTo!!.first,
            rotateTo!!.second,
            event.packet.isOnGround
        ))
        else {
            PacketUtils.sendPacket(C05PacketPlayerLook(rotateTo!!.first, rotateTo!!.second, event.packet.isOnGround))
        }
        PacketUtils.sendPacket(C08PacketPlayerBlockPlacement(mc.thePlayer.heldItem))
        KeyBinding.setKeyBindState(mc.gameSettings.keyBindSneak.keyCode, false)
    }

    fun getIdealSpot(coords: Vec3): Vec3 {
        val player = mc.thePlayer
        val eyePos = player.positionVector.addVector(0.0, 1.65, 0.0)

        val minX = coords.xCoord
        val minY = coords.yCoord
        val minZ = coords.zCoord
        val maxX = minX + 1.0
        val maxY = minY + 1.0
        val maxZ = minZ + 1.0

        data class Face(val center: Vec3, val normal: Vec3)

        val faces = listOf(
            Face(Vec3((minX + maxX) / 2, minY, (minZ + maxZ) / 2), Vec3(0.0, -1.0, 0.0)),
            Face(Vec3((minX + maxX) / 2, maxY, (minZ + maxZ) / 2), Vec3(0.0, 1.0, 0.0)),
            Face(Vec3((minX + maxX) / 2, (minY + maxY) / 2, minZ), Vec3(0.0, 0.0, -1.0)),
            Face(Vec3((minX + maxX) / 2, (minY + maxY) / 2, maxZ), Vec3(0.0, 0.0, 1.0)),
            Face(Vec3(minX, (minY + maxY) / 2, (minZ + maxZ) / 2), Vec3(-1.0, 0.0, 0.0)),
            Face(Vec3(maxX, (minY + maxY) / 2, (minZ + maxZ) / 2), Vec3(1.0, 0.0, 0.0))
        )

        return faces.maxByOrNull { face ->
            val directionToFace = face.center.subtract(eyePos).normalize()
            directionToFace.dotProduct(face.normal)
        }?.center ?: coords.addVector(0.5, 0.5, 0.5)
    }

}