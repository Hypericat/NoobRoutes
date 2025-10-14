package noobroutes.ui.editgui

import net.minecraft.client.renderer.GlStateManager
import noobroutes.ui.ColorPalette
import noobroutes.ui.ColorPalette.TEXT_OFFSET
import noobroutes.ui.editgui.elements.EditGuiCheckBox
import noobroutes.ui.editgui.elements.EditGuiDescription
import noobroutes.ui.editgui.elements.EditGuiPageOpener
import noobroutes.ui.editgui.elements.EditGuiSelector
import noobroutes.ui.editgui.elements.EditGuiSliderElement
import noobroutes.ui.editgui.elements.EditGuiSwitchElement
import noobroutes.ui.util.MouseUtils
import noobroutes.ui.util.UiElement
import noobroutes.ui.util.animations.impl.CubicBezierAnimation
import noobroutes.utils.render.Color
import noobroutes.utils.render.drawArrow
import noobroutes.utils.render.roundedRectangle
import noobroutes.utils.render.text
import noobroutes.utils.skyblock.modMessage
import java.util.Stack
import kotlin.jvm.Throws

class EditGuiBase() : UiElement(0f, 0f) {
    var height = 0f
    var name = ""
    var onOpen: () -> Unit = {}
    var onClose: () -> Unit = {}
    val pageStack = Stack<EditGuiPage>()
    lateinit var root: EditGuiPage


    class EditGuiBaseBuilder() {
        private val pages = mutableListOf<EditGuiPage>()
        private val pageStack = Stack<EditGuiPage>()
        private val root = EditGuiPage("Root")
        private var lastAddedElement: EditGuiElement? = null


        init {
            pageStack.push(root)
            pages.add(root)
        }

        private fun addElement(element: EditGuiElement){
            pageStack.peek().addElement(element as UiElement)
            lastAddedElement = element
        }

        fun popPage(){
            pages.add(pageStack.pop())
        }

        fun pushPage(name: String) {
            val page = EditGuiPage(name)
            addElement(EditGuiPageOpener(page))
            pageStack.push(page)
        }

        fun addCheckBox(name: String, options: ArrayList<String>, getter: () -> Array<Boolean>, setter: (Array<Boolean>) -> Unit, priority: Int? = null) {
            val element = EditGuiCheckBox(name, options, getter, setter)
            priority?.let {
                element.priority = it
            }
            addElement(element)
        }

        fun addSlider(name: String, min: Double, max: Double, increment: Double, roundTo: Int, getter: () -> Double, setter: (Double) -> Unit, priority: Int? = null) {
            val element = EditGuiSliderElement(name, min, max, increment, roundTo, getter, setter)
            priority?.let {
                element.priority = it
            }
            addElement(element)
        }
        @Throws(IllegalStateException::class)
        fun bindDescription(text: String) {
            val lastAdded = lastAddedElement
                ?: throw IllegalStateException("No previous element to bind description to")
            addElement(EditGuiDescription(text, lastAdded))
        }

        fun addParagraph(text: String, priority: Int?) {
            val element = EditGuiDescription(text)
            priority?.let {
                element.priority = it
            }
            addElement(element)
        }

        fun addSwitch(name: String, getter: () -> Boolean, setter: (Boolean) -> Unit, priority: Int? = null){
            val element = EditGuiSwitchElement(name, getter, setter)
            priority?.let {
                element.priority = it
            }
            addElement(element)
        }
        fun addSelector(name: String, options: ArrayList<String>, getter: () -> Int, setter: (Int) -> Unit, priority: Int? = null) {
            val element = EditGuiSelector(name, options, getter, setter)
            priority?.let {
                element.priority = it
            }
            addElement(element)
        }

        private var onOpen: () -> Unit = {}
        private var onClose: () -> Unit = {}
        private var name = ""
        fun setName(name: String) {
            this.name = name
        }
        fun setOnOpen(action: () -> Unit) {
            this.onOpen = action
        }
        fun setOnClose(action: () -> Unit) {
            this.onClose = action
        }

        fun build(): EditGuiBase {
            val base = EditGuiBase()
            base.root = root
            base.addChild(root)

            for (page in pages) {
                page.updateYPositions()
            }

            base.name = name
            base.height = root.height
            base.onOpen = this.onOpen
            base.onClose = this.onClose
            base.updatePosition(editGuiBaseX, editGuiBaseY)
            return base
        }
    }



    val pageSwitchAnimation = CubicBezierAnimation(250)
    var pageSwitchDirection = LEFT
    var previousPage: EditGuiPage? = null
    private val redoStack = Stack<EditGuiPage>()

    private fun handlePreviousPage() {
        val prevPage = this.previousPage ?: return
        uiChildren.remove(prevPage)
        previousPage = null
    }

    fun pushPageLayer(page: EditGuiPage) {
        handlePreviousPage()
        val previousPage = getCurrentPage()
        this.previousPage = previousPage
        pageSwitchDirection = LEFT

        pageStack.push(page)
        addChild(page)
        pageSwitchAnimation.start(true)
        redoStack.clear()
    }

    private fun redo() {
        if (redoStack.empty()) return
        handlePreviousPage()
        val page = redoStack.pop()
        val previousPage = getCurrentPage()
        this.previousPage = previousPage
        pageSwitchDirection = LEFT

        pageStack.push(page)
        addChild(page)
        pageSwitchAnimation.start(true)
    }

    fun popPageLayer() {
        if (pageStack.empty()) return
        handlePreviousPage()
        val previousPage = pageStack.pop()
        this.previousPage = previousPage
        pageSwitchDirection = RIGHT

        redoStack.add(previousPage)
        val currentPage = getCurrentPage()
        if (currentPage !in this.uiChildren) addChild(getCurrentPage())
        pageSwitchAnimation.start(true)
    }

    var dragging = false
    var x2 = 0f
    var y2 = 0f
    override fun mouseClicked(mouseButton: Int): Boolean {
        if (mouseButton != 0) return false
        when {
            isHoveredLeftArrow -> {
                popPageLayer()
                return true
            }
            isHoveredRightArrow -> {
                redo()
                return true
            }
            isAreaHovered(0f, 0f, 600f, 70f) -> {
                x2 = x - MouseUtils.mouseX
                y2 = y - MouseUtils.mouseY
                dragging = true
                return true
            }
        }



        return false
    }


    override fun mouseReleased(): Boolean {
        dragging = false
        return false
    }

    private fun getCurrentPage(): EditGuiPage {
        return if (pageStack.empty()) root else pageStack.peek()
    }

    fun updateYPositions(){
        getCurrentPage().updateYPositions()
    }

    fun handlePageAnimation() {
        if (!pageSwitchAnimation.isAnimating()) {
            val prevPage = previousPage ?: return
            previousPage = null
            uiChildren.remove(prevPage)
            return
        }
        val sign = if (pageSwitchDirection) 1 else -1


        val prevPage = previousPage ?: return
        val x = pageSwitchAnimation.get(0f,sign * 600f, false)
        prevPage.updatePosition(x, 0f)

        this.height = pageSwitchAnimation.get(prevPage.height, getCurrentPage().height, false)
        val currentPage = getCurrentPage()
        currentPage.updatePosition(sign * -600 + x, 0f)


    }

    private inline val isHoveredLeftArrow get() = isAreaHovered(WIDTH - 88.75f, 18.75f, 37.5f, 37.5f)
    private inline val isHoveredRightArrow get() = isAreaHovered(WIDTH - 48.75f, 17.75f, 37.5f, 37.5f)


//70
    override fun draw() {
        GlStateManager.pushMatrix()
        if (dragging) {
            updatePosition(x2 + MouseUtils.mouseX, y2 + MouseUtils.mouseY)
            editGuiBaseX = x
            editGuiBaseY = y
        }
        handlePageAnimation()
        translate(x, y)
        blurRoundedRectangle(0f, 0f, WIDTH, height, 20f, 20f, 20f, 20f, 0.5f)
        roundedRectangle(0f, 0f, WIDTH, 70f, ColorPalette.titlePanelColor,  ColorPalette.titlePanelColor, Color.TRANSPARENT, 0, 20f, 20f, 0f, 0f, 0f)
        roundedRectangle(0f, 0f, WIDTH, height, ColorPalette.buttonColor, radius = 20)
        text(name, X_ALIGNMENT_LEFT - 10, 37.5, Color.WHITE, size = 30)
        drawArrow(WIDTH - 70f, 37.5f, 180f, 1.5f, dark = isHoveredLeftArrow)
        drawArrow(WIDTH - 30f, 36.5f, 0f, 1.5f, dark = isHoveredRightArrow)
        scissorChildren(0f, 0f, WIDTH, height)
        GlStateManager.popMatrix()
    }

    companion object {
        var editGuiBaseX = 0f
        var editGuiBaseY = 0f
        private const val LEFT = false
        private const val RIGHT = true


        const val X_ALIGNMENT_LEFT = 30f
        const val X_ALIGNMENT_RIGHT = 300f
        const val WIDTH = 600f
        const val BUTTON_WIDTH = WIDTH - TEXT_OFFSET * 2f - 60f //60f is X_ALIGNMENT_LEFT * 2f
        const val HALF_BUTTON_WIDTH = BUTTON_WIDTH * 0.5f
    }
}