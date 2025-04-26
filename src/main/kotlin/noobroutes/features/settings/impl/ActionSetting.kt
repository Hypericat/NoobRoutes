package com.github.wadey3636.noobroutes.features.settings.impl

/**
 * Setting that gets ran when clicked.
 *
 * @author Aton
 */
class ActionSetting(
    name: String,
    hidden: Boolean = false,
    description: String,
    override val default: () -> Unit = {}
) : com.github.wadey3636.noobroutes.features.settings.Setting<() -> Unit>(name, hidden, description) {

    override var value: () -> Unit = default

    var action: () -> Unit by this::value
}