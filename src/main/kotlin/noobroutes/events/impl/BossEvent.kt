package noobroutes.events.impl

import net.minecraftforge.fml.common.eventhandler.Event
import noobroutes.utils.skyblock.dungeon.Floor

open class BossEvent(val floor: Floor) : Event() {
    class PhaseChange(floor: Floor, val phase: Phase) : BossEvent(floor)
    class TerminalPhaseChange(floor: Floor, val phase: TerminalPhase) : BossEvent(floor)
    class BossStart(floor: Floor) : BossEvent(floor)
    class BossFinish(floor: Floor) : BossEvent(floor)
}


enum class Phase {
    P1,
    P2,
    P3,
    P4,
    P5,
    Unknown
}

enum class TerminalPhase {
    S1,
    S2,
    S3,
    S4,
    GoldorFight,
    Unknown
}