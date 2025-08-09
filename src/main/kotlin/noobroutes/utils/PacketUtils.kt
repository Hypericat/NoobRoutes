package noobroutes.utils



import net.minecraft.network.Packet
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import noobroutes.Core.mc


object PacketUtils {

    fun sendPacket(packet: Packet<*>?) {
        mc.netHandler.networkManager.sendPacket(packet)
    }

    var lastResponse: C03PacketPlayer.C06PacketPlayerPosLook = C03PacketPlayer.C06PacketPlayerPosLook()

    fun handleC06ResponsePacket(packet: Packet<*>) {
        lastResponse = packet as C03PacketPlayer.C06PacketPlayerPosLook;
    }


    fun C03PacketPlayer.C06PacketPlayerPosLook.matches(s08: S08PacketPlayerPosLook): Boolean {
        return s08.x == this.positionX && s08.y == this.positionY && s08.z == this.positionZ && angleMatches(s08.yaw, this.yaw) && angleMatches(s08.pitch, this.pitch)
    }

    fun C03PacketPlayer.C06PacketPlayerPosLook.isResponseToLastS08(): Boolean {
         return lastResponse === this
    }

    fun C03PacketPlayer.C06PacketPlayerPosLook.generateString(): String{
        return "x: ${this.positionX}, y: ${this.positionY}, z: ${this.positionZ}, yaw: ${this.yaw}, pitch: ${this.pitch}"
    }

    fun S08PacketPlayerPosLook.generateString(): String {
        return "x: ${this.x}, y: ${this.y}, z: ${this.z}, yaw: ${this.yaw}, pitch: ${this.pitch}"
    }

    private fun angleMatches(s08Angle: Float, n2: Float, tolerance: Double = 1e-4): Boolean {
        return kotlin.math.abs(s08Angle - n2) < tolerance || s08Angle == 0f
    }

    fun C03PacketPlayer.C06PacketPlayerPosLook.matches(c06: C03PacketPlayer.C06PacketPlayerPosLook): Boolean{
        return this.positionX == c06.positionX
    }
}