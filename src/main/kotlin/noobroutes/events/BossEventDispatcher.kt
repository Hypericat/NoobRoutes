package noobroutes.events

import net.minecraft.init.Blocks
import net.minecraft.network.play.server.S22PacketMultiBlockChange
import net.minecraft.util.BlockPos
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.events.impl.*
import noobroutes.utils.postAndCatch
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.Floor

object BossEventDispatcher {

    private val necronStartRegex = Regex("\\[BOSS] Necron: (Finally, I heard so much about you.|You went further than any human before).*?")

    var currentTerminalPhase: TerminalPhase = TerminalPhase.Unknown
    var currentBossPhase: Phase = Phase.Unknown
    private var lastInBoss = false
    var inBoss = false
    var inF7Boss = false

    private inline val inBossCheck: Boolean
        get() = LocationUtils.currentDungeon?.inBoss == true

    fun reset(){
        inBoss = false
        inF7Boss = false
        lastInBoss = false
        currentBossPhase = Phase.Unknown
        currentTerminalPhase = TerminalPhase.Unknown
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        reset()
    }
    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        reset()
    }

    @SubscribeEvent
    fun onClientTick(event: ClientTickEvent){
        if (!DungeonUtils.inDungeons) return
        val iB = inBossCheck
        if (lastInBoss != iB) {
            if (iB) {
                inBoss = true
                if (DungeonUtils.floorNumber == 7) inF7Boss = true
                BossEvent.BossStart(DungeonUtils.floor).postAndCatch()
            } else {
                inBoss = false
                inF7Boss = false
                BossEvent.BossFinish(DungeonUtils.floor).postAndCatch()
            }
        }
        lastInBoss = iB
    }


    @SubscribeEvent
    fun onChat(event: ChatPacketEvent) {
        when (event.message) {
            "[BOSS] Maxor: WELL! WELL! WELL! LOOK WHO'S HERE!" -> {
                currentBossPhase = Phase.P1
                BossEvent.PhaseChange(DungeonUtils.floor, Phase.P1).postAndCatch()
            }
            "[BOSS] Storm: Pathetic Maxor, just like expected." -> {
                currentBossPhase = Phase.P2
                BossEvent.PhaseChange(DungeonUtils.floor, Phase.P2).postAndCatch()
            }
            "[BOSS] Goldor: Who dares trespass into my domain?" -> {
                BossEvent.PhaseChange(DungeonUtils.floor, Phase.P3).postAndCatch()
                BossEvent.TerminalPhaseChange(DungeonUtils.floor, TerminalPhase.S1).postAndCatch()
                currentBossPhase = Phase.P3
                currentTerminalPhase = TerminalPhase.S1
            }
            "The Core entrance is opening!" -> {
                BossEvent.TerminalPhaseChange(DungeonUtils.floor, TerminalPhase.GoldorFight).postAndCatch()
                currentTerminalPhase = TerminalPhase.GoldorFight
            }
            "[BOSS] Necron: All this, for nothing..." -> {
                BossEvent.PhaseChange(DungeonUtils.floor, Phase.P5)
                currentBossPhase = Phase.P5
            }
        }
        if (necronStartRegex.matchEntire(event.message)?.groupValues != null) {
            BossEvent.PhaseChange(DungeonUtils.floor, Phase.P4).postAndCatch()
            currentBossPhase = Phase.P4
            currentTerminalPhase = TerminalPhase.Unknown
        }
        if (event.message.contains("Defeated Maxor, Storm, Goldor, and Necron in")) {
            currentBossPhase = Phase.Unknown
            currentTerminalPhase = TerminalPhase.Unknown
            BossEvent.BossFinish(DungeonUtils.floor).postAndCatch()
            inBoss = false
            inF7Boss = false
        }
    }

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Receive) {
        if (event.packet !is S22PacketMultiBlockChange || !inBoss) return
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