package noobroutes.features.settings.impl

/**
 * A setting intended to show or hide other settings in the GUI.
 *
 * @author Bonsai
 */
class DropdownSetting (
    name: String,
    override val default: Boolean = false
): noobroutes.features.settings.Setting<Boolean>(name, false, "") {

    override var value: Boolean = default

    var enabled: Boolean by this::value
}