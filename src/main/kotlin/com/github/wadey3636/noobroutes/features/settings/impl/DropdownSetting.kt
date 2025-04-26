package com.github.wadey3636.noobroutes.features.settings.impl

/**
 * A setting intended to show or hide other settings in the GUI.
 *
 * @author Bonsai
 */
class DropdownSetting (
    name: String,
    override val default: Boolean = false
): com.github.wadey3636.noobroutes.features.settings.Setting<Boolean>(name, false, "") {

    override var value: Boolean = default

    var enabled: Boolean by this::value
}