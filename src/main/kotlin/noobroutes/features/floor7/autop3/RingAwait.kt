package noobroutes.features.floor7.autop3

import noobroutes.events.impl.Phase
import noobroutes.events.impl.TerminalPhase
import noobroutes.utils.capitalizeFirst

enum class RingAwait {
    LEFT,
    TERM,
    LEAP,
    COMPLETE_DEV,
    COMPLETE_LEVER,
    P5,
    P4,
    P3,
    P2,
    P1,
    S1,
    S2,
    S3,
    S4,
    GOLDOR_FIGHT;

    fun getIndex(): Int {
        return RingAwait.entries.indexOf(this)
    }

    fun matchesBossPhase(phase: Phase): Boolean {
        return when (this) {
            P1 -> phase == Phase.P1
            P2 -> phase == Phase.P2
            P3 -> phase == Phase.P3
            P4 -> phase == Phase.P4
            P5 -> phase == Phase.P5
            else -> false
        }
    }

    fun matchesTerminalPhase(phase: TerminalPhase): Boolean {
        return when (this) {
            S1 -> phase == TerminalPhase.S1
            S2 -> phase == TerminalPhase.S2
            S3 -> phase == TerminalPhase.S3
            S4 -> phase == TerminalPhase.S4
            GOLDOR_FIGHT -> phase == TerminalPhase.GoldorFight
            else -> false
        }
    }

    companion object {


        operator fun get(index: Int): RingAwait {
            return RingAwait.entries[index]
        }

        fun getFromNameSafe(name: String): RingAwait? {
            return entries.firstOrNull {it.name.lowercase() == name.lowercase()}
        }

        fun getFromName(name: String): RingAwait {
            return entries.first { it.name == name }
        }

        fun getOptionsList(): ArrayList<String> {
            val list = arrayListOf<String>()
            for (entry in entries) {
                list.add(entry.name.lowercase().capitalizeFirst().replace('_', ' '))
            }
            return list
        }


    }
}