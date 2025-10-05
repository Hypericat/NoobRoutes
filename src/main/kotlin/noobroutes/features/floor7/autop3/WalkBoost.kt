package noobroutes.features.floor7.autop3

import com.google.gson.JsonElement

enum class WalkBoost {
    NONE,
    NORMAL,
    LARGE,
    UNCHANGED;
    companion object {


        fun JsonElement.asWalkBoost(): WalkBoost {
            return entries.first {it.name == this.asString}
        }
    }
}