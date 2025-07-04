package noobroutes.ui.util

interface ElementValue<T> {
    val elementValueChangeListeners: MutableList<(T) -> Unit>
    var elementValue: T
    fun setValue(value: T) {
        this.elementValue = value
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