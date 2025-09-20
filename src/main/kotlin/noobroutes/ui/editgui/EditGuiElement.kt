package noobroutes.ui.editgui

interface EditGuiElement {
    var priority: Int
    val isDoubleWidth: Boolean
    val height: Float
    //The Height of each element should be its size, plus the gap 10f
}