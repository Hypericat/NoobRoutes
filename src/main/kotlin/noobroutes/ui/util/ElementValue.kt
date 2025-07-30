package noobroutes.ui.util

import noobroutes.Core.logger

interface ElementValue<T> {
    val elementValueChangeListeners: MutableList<(T) -> Unit>
    var elementValue: T
    fun setValue(value: T) {
        this.elementValue = value
        for (listener in elementValueChangeListeners) {
            listener.invoke(elementValue)
        }
    }
    fun invokeValueChangeListeners() {
        for (listener in elementValueChangeListeners) {
            listener.invoke(elementValue)
        }
    }

    fun getValue(): T {
        return elementValue
    }
    fun addValueChangeListener(listener: (T) -> Unit){
        elementValueChangeListeners.add(listener)
    }

}