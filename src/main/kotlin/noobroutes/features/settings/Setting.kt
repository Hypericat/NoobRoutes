package noobroutes.features.settings

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlin.properties.PropertyDelegateProvider
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty
import kotlin.reflect.full.hasAnnotation

/**
 * Superclass of Settings.
 * @author Aton
 */
abstract class Setting<T> (
    val name: String,
    var hidden: Boolean = false,
    var description: String = "",
) : ReadWriteProperty<noobroutes.features.Module, T>, PropertyDelegateProvider<noobroutes.features.Module, ReadWriteProperty<noobroutes.features.Module, T>> {


    var devOnly: Boolean = false
        private set


    /**
     * Default value of the setting
     */
    abstract val default: T

    /**
     * Value of the setting
     */
    abstract var value: T

    /**
     * Dependency for if it should be shown in the [click gui][noobroutes.ui.clickgui.elements.ModuleButton].
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

    override operator fun provideDelegate(thisRef: noobroutes.features.Module, property: KProperty<*>): ReadWriteProperty<noobroutes.features.Module, T> {
        devOnly = property.hasAnnotation<DevOnly>()
        return thisRef.register(this)
    }

    override operator fun getValue(thisRef: noobroutes.features.Module, property: KProperty<*>): T {
        return value
    }

    override operator fun setValue(thisRef: noobroutes.features.Module, property: KProperty<*>, value: T) {
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