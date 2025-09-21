package noobroutes.utils



import net.minecraft.network.Packet
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.Core.mc
import noobroutes.events.impl.PacketEvent
import noobroutes.utils.clock.Executor

object PacketUtils {

    init {
        Executor(10000, "Clear Packet Cache") {
            for (cancelledPacket in packetCancelList) {
                if (System.currentTimeMillis() - cancelledPacket.time < 10000) continue
                packetCancelList.remove(cancelledPacket)
            }
        }
    }

    private data class CanceledPacket(val packet: Packet<*>, val time: Long)

    private var packetCancelList = mutableListOf<CanceledPacket>()



    @SubscribeEvent(priority = EventPriority.HIGHEST)
    fun onPacket(event: PacketEvent.Send) {
        for (cancelledPacket in packetCancelList) {
            if (event.packet != cancelledPacket.packet) continue
            event.isCanceled = true
            packetCancelList.remove(cancelledPacket)

        }
    }

    fun cancelNettyPacket(packet: Packet<*>) {
        packetCancelList.add(CanceledPacket(packet, System.currentTimeMillis()))
    }

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
}