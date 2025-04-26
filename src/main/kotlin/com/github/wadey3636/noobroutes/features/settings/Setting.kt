package com.github.wadey3636.noobroutes.features.settings

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/**
 * Superclass of Settings.
 * @author Aton
 */
abstract class Setting<T> (
    val name: String,
    var hidden: Boolean = false,
    var description: String = "",
) : ReadWriteProperty<com.github.wadey3636.noobroutes.features.Module, T>, PropertyDelegateProvider<com.github.wadey3636.noobroutes.features.Module, ReadWriteProperty<com.github.wadey3636.noobroutes.features.Module, T>> {

    /**
     * Default value of the setting
     */
    abstract val default: T

    /**
     * Value of the setting
     */
    abstract var value: T

    /**
     * Dependency for if it should be shown in the [click gui][com.github.wadey3636.noobroutes.ui.clickgui.elements.ModuleButton].
     */
    protected var visibilityDependency: (() -> Boolean)? = null

    /**
     * Resets the setting to the default value
     */
    open fun reset() {
        value = default
    }

    val shouldBeVisible: Boolean
        get() {
            return (visibilityDependency?.invoke() ?: true) && !hidden
        }

    override operator fun provideDelegate(thisRef: com.github.wadey3636.noobroutes.features.Module, property: KProperty<*>): ReadWriteProperty<com.github.wadey3636.noobroutes.features.Module, T> {
        return thisRef.register(this)
    }

    override operator fun getValue(thisRef: com.github.wadey3636.noobroutes.features.Module, property: KProperty<*>): T {
        return value
    }

    override operator fun setValue(thisRef: com.github.wadey3636.noobroutes.features.Module, property: KProperty<*>, value: T) {
        this.value = value
    }

    companion object {

        val gson: Gson = GsonBuilder().setPrettyPrinting().create()

        fun <K : Setting<T>, T> K.withDependency(dependency: () -> Boolean): K {
            visibilityDependency = dependency
            return this
        }
    }
}