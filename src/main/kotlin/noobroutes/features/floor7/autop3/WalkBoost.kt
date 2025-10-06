package noobroutes.features.floor7.autop3

import com.google.gson.JsonElement
import noobroutes.utils.capitalizeFirst

enum class WalkBoost {
    NONE,
    NORMAL,
    LARGE,
    UNCHANGED;

    fun getIndex(): Int {
        return WalkBoost.entries.indexOf(this)
    }


    companion object {
        fun getOptionsList(): ArrayList<String> {
            val list = arrayListOf<String>()
            for (entry in WalkBoost.entries) {
                list.add(entry.name.lowercase().capitalizeFirst().replace('_', ' '))
            }
            return list
        }
        operator fun get(index: Int): WalkBoost {
            return WalkBoost.entries[index]
        }

        fun JsonElement.asWalkBoost(): WalkBoost {
            return entries.first {it.name == this.asString}
        }
    }
}