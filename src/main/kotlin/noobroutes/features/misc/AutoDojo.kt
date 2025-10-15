package noobroutes.features.misc

import net.minecraft.block.state.IBlockState
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S22PacketMultiBlockChange
import net.minecraft.network.play.server.S23PacketBlockChange
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.events.impl.ChatPacketEvent
import noobroutes.events.impl.MovePlayerEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.WorldChangeEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.DevOnly
import noobroutes.mixin.accessors.LastReportedAccessor
import noobroutes.utils.PacketUtils.send
import noobroutes.utils.Scheduler
import noobroutes.utils.Utils.isClose
import noobroutes.utils.Vec2
import noobroutes.utils.noControlCodes
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.toBlockPos
import noobroutes.utils.toVec3
import kotlin.math.abs

@DevOnly
object AutoDojo: Module(
    name = "Auto Dojo",
    description = "Does Dojo",
    category = Category.MISC
) {

    private var doingShit = false
    private var missingBlink = listOf<C03PacketPlayer.C04PacketPlayerPosition>()

    private var currentBlock: BlockPos? = null

    var started = false
        private set

    private var skipNextS08 = false

    private var cancelled = 0

    private val oneWalkCoords = listOf(
        0.0 to 0.0,
        0.2806167374854849 to 0.0,
        0.5612334749709773 to 0.0,
        0.8418502124564604 to 0.0,
        1.0 to 0.0
    )

    private val twoWalkCoords = listOf(
        0.0 to 0.0,
        0.2806167374854849 to 0.0,
        0.5612334749709773 to 0.0,
        0.8418502124564604 to 0.0,
        1.1224669499419495 to 0.0,
        1.4030836874274386 to 0.0,
        1.6837004249129277 to 0.0,
        1.964317162398417 to 0.0,
        2.0 to 0.0
    )

    private val lowJumpCoords = listOf(
        0.0 to 0.0,
        0.2806167374854849 to 0.0,
        0.7612456908014249 to 0.41999998688698,
        1.0491491282326548 to 0.80999998688696,
        1.3360045247997 to 1.1137999927997,
        1.623700910251805 to 1.2031240028629,
        1.9104233958699348 to 1.0122615329027,
        2.1968208637415847 to 0.74681630717535,
        2.4829225654554046 to 0.4082799793737,
        2.768755119957855 to 0.0,
        3.0 to 0.0
    )

    private val smallJumpCoords = listOf(
        0.0 to 0.0,
        0.48061669924423 to 0.41999998688698,
        0.76851344595154 to 0.7531999805212,
        1.05597949144512 to 1.0013359791121,
        1.34305359882293 to 1.1661092609382,
        1.62977104250511 to 1.2491870787446,
        1.9161639222149 to 1.2491870787446,
        2.20226144870132 to 1.17078707721873,
        2.48809020374672 to 1.0155550727021,
        2.77367437677374 to 0.78502770378915,
        3.0 to 0.4807108763316,
        3.0 to 0.10408037809296,
        3.0 to 0.0
    )

    private val jumpCoords = listOf(
        0.0 to 0.0,
        0.2806167374854849 to 0.0,
        0.7612334367297149 to 0.41999998688698,
        1.049130183436725 to 0.7531999805212,
        1.336596228931605 to 1.0013359791121,
        1.623670336309415 to 1.1661092609382,
        1.910387780991595 to 1.2491870787446,
        2.196780660701385 to 1.2491870787446,
        2.482878187187805 to 1.17078707721873,
        2.768706942233215 to 1.0155550727021,
        3.054291115260225 to 0.78502770378915,
        3.339652718644105 to 0.4807108763316,
        3.624811783646885 to 0.10408037809296,
        3.909786538717565 to 0.0,
        4.0 to 0.0,
    )

    @SubscribeEvent
    fun onS08(event: PacketEvent.Receive) {
        if (event.packet !is S08PacketPlayerPosLook || skipNextS08) return
        reset()
    }

    private fun reset() {
        started = false
        doingShit = false
        currentBlock = null
        missingBlink = listOf()
        cancelled = 0
        skipNextS08 = false
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldChangeEvent) { reset() }

    @SubscribeEvent
    fun onChat(event: PacketEvent.Receive) {
        if (event.packet is S02PacketChat && event.packet.chatComponent.unformattedText.contains("Test of Swiftness")) {
            started = true
            skipNextS08 = true
            Scheduler.schedulePreTickTask(10) { skipNextS08 = false } //sometimes s08 arrives first, sometimes chat
        }
    }

    @SubscribeEvent
    fun onC03(event: PacketEvent.Send) {
        if (!started || mc.thePlayer == null || event.packet !is C03PacketPlayer) return

        if (event.packet.isMoving) {
            cancelled = 0
            return
        }

        event.isCanceled = true
        cancelled++

        if (missingBlink.isEmpty() || cancelled < missingBlink.size) return

        Scheduler.schedulePreTickTask {
            for (packet in missingBlink) packet.send()

            val lastPacket = missingBlink.last()

            mc.thePlayer.setPosition(lastPacket.positionX, lastPacket.positionY, lastPacket.positionZ)

            val accessor = mc.thePlayer as LastReportedAccessor
            accessor.setLastReportedPosX(lastPacket.positionX)
            accessor.setLastReportedPosY(lastPacket.positionY)
            accessor.setLastReportedPosZ(lastPacket.positionZ)

            doingShit = false
            currentBlock = null
            missingBlink = listOf()
        }
    }

    private fun goToBlock(pos: BlockPos) {
        if (!started ||
            !isClose(abs(mc.thePlayer.posX % 1), 0.5) || !isClose(abs(mc.thePlayer.posZ % 1), 0.5) || mc.thePlayer.posY % 1 != 0.0 ||
            mc.thePlayer.posY != pos.y + 1.0
        ) return modMessage("stand in the center of the block (dont move after the tp)")

        doingShit = true

        val goalPos = pos.toVec3(0.5, 1.0, 0.5)
        val offsetVec = goalPos.subtract(mc.thePlayer.positionVector).toVec2()

        val coordList = when (offsetVec.sqLength()) {
            1.0 -> oneWalkCoords
            4.0 -> twoWalkCoords
            9.0 -> smallJumpCoords
            16.0 -> jumpCoords
            else -> return devMessage("smth went wrong. vec: $offsetVec")
        }

        val dirVec = offsetVec.toVec3().normalize()

        if (coordList.size in (cancelled + 1)..9) { //cancelled < coordList.size && coordList.size <= 9
            Scheduler.schedulePreTickTask(coordList.size - cancelled - 1) { goToBlock(pos) }
            return
        }

        val packetList = coordList.map { C03PacketPlayer.C04PacketPlayerPosition(
            it.first * dirVec.xCoord + mc.thePlayer.posX,
            it.second + mc.thePlayer.posY,
            it.first * dirVec.zCoord + mc.thePlayer.posZ,
            it.second == 0.0)
        }

        val sendList = packetList.take(cancelled)
        val others = packetList.drop(cancelled)

        val oldCancelled = cancelled

        for (packet in sendList) packet.send()

        val lastPacket = sendList.last()

        mc.thePlayer.setPosition(lastPacket.positionX, lastPacket.positionY, lastPacket.positionZ)

        val accessor = mc.thePlayer as LastReportedAccessor
        accessor.setLastReportedPosX(lastPacket.positionX)
        accessor.setLastReportedPosY(lastPacket.positionY)
        accessor.setLastReportedPosZ(lastPacket.positionZ)

        if (oldCancelled < coordList.size) {
            missingBlink = others
            return
        }

        doingShit = false
        currentBlock = null
    }

    @SubscribeEvent
    fun onMoveEntity(event: MovePlayerEvent) {
        if (!started) return
        event.isCanceled = true
    }

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Receive) {
        if (!started) return
        if (event.packet is S23PacketBlockChange && event.packet.blockState.isGoodWool(event.packet.blockPosition)) selectTarget(event.packet.blockPosition)
        else if (event.packet is S22PacketMultiBlockChange) {
            val wool =  event.packet.changedBlocks.find { it.blockState.isGoodWool(it.pos) } ?: return
            selectTarget(wool.pos)
        }
    }

    private fun selectTarget(pos: BlockPos) {
        Scheduler.scheduleLowestPreTickTask {
            if (pos.distanceSq(mc.thePlayer.positionVector.toBlockPos()) > 100 || pos.y + 1.0 != mc.thePlayer.posY || pos == mc.thePlayer.positionVector.toBlockPos().down()) return@scheduleLowestPreTickTask
            if (!doingShit && currentBlock == null) goToBlock(pos)
        }
    }

    private fun Vec3.toVec2(): Vec2 = Vec2(this.xCoord, this.zCoord)
    private fun Vec2.toVec3(): Vec3 = Vec3(this.x, 0.0, this.z)
    private fun Vec2.sqLength(): Double = this.x * this.x + this.z * this.z

    private fun IBlockState.isLimeWool(): Boolean {
        return  this.block == Blocks.wool && this.block.getMetaFromState(this) == 5
    }

    private fun IBlockState.isOrangeWool(): Boolean {
        return  this.block == Blocks.wool && this.block.getMetaFromState(this) == 1
    }

    private fun IBlockState.isGoodWool(pos: BlockPos): Boolean {
        return (this.isLimeWool() || this.isOrangeWool() && !mc.theWorld.getBlockState(pos).isLimeWool()) && pos != mc.thePlayer.positionVector.toBlockPos().down()
    }
}