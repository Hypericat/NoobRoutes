package noobroutes.events.impl

import net.minecraftforge.fml.common.eventhandler.Cancelable
import net.minecraftforge.fml.common.eventhandler.Event

abstract class ClickEvent : Event() {
    @Cancelable
    class Left : ClickEvent()

    @Cancelable
    class Right : ClickEvent()

    @Cancelable
    class Middle: ClickEvent()

    @Cancelable
    class All(val type: ClickType) : ClickEvent()

    enum class ClickType{
        Left,
        Middle,
        Right
    }
}
