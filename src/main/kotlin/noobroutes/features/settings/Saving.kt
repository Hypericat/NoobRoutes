package com.github.wadey3636.noobroutes.features.settings

import com.google.gson.JsonElement

/**
 * Used for settings that you want to save/load.
 *
 * @see com.github.wadey3636.noobroutes.features.settings.impl.BooleanSetting
 */
internal interface Saving {
    /**
     * Used to update the setting from the json.
     */
    fun read(element: JsonElement?)

    /**
     * Used to create the json.
     */
    fun write(): JsonElement
}