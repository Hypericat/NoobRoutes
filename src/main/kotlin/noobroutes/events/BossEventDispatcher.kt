package noobroutes.events

import net.minecraft.block.Block
import net.minecraft.block.BlockAir
import net.minecraft.init.Blocks
import net.minecraft.network.play.server.S22PacketMultiBlockChange
import net.minecraft.network.play.server.S23PacketBlockChange
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.events.impl.*
import noobroutes.utils.isAir
import noobroutes.utils.postAndCatch
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.Floor

object BossEventDispatcher {

    var currentTerminalPhase: TerminalPhase = TerminalPhase.Unknown
    private var lastInBoss = false

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent){
        if (!DungeonUtils.inDungeons) return
        val inBoss = DungeonUtils.inBoss
        if (lastInBoss != inBoss) {
            if (inBoss) {
                BossEvent.BossStart(DungeonUtils.floor).postAndCatch()
            } else {
                BossEvent.BossFinish(DungeonUtils.floor).postAndCatch()
            }
        }
    }


    @SubscribeEvent
    fun onChat(event: ChatPacketEvent) {
        when (event.message) {
            "[BOSS] Storm: Pathetic Maxor, just like expected." -> {
                BossEvent.PhaseChange(DungeonUtils.floor, Phase.P2).postAndCatch()
            }
        }

    }

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Receive) {
        if (event.packet !is S22PacketMultiBlockChange || !DungeonUtils.inBoss) return
        //leaving some space open just incase we need a block change for a different floor
        if (DungeonUtils.floor != Floor.F7 && DungeonUtils.floor != Floor.M7) return
        event.packet.changedBlocks.forEach {
            if (it.blockState.block != Blocks.air) return@forEach
            when (it.pos) {
                BlockPos(101, 118, 123) -> {
                    currentTerminalPhase = TerminalPhase.S2
                    BossEvent.TerminalPhaseChange(DungeonUtils.floor, TerminalPhase.S2).postAndCatch()
                }
                BlockPos(17, 118, 132) -> {
                    currentTerminalPhase = TerminalPhase.S3
                    BossEvent.TerminalPhaseChange(DungeonUtils.floor, TerminalPhase.S3).postAndCatch()
                }
                BlockPos(17, 118, 132) -> {
                    currentTerminalPhase = TerminalPhase.S4
                    BossEvent.TerminalPhaseChange(DungeonUtils.floor, TerminalPhase.S4).postAndCatch()
                }
            }
        }


    }


}