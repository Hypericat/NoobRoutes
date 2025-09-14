package noobroutes.features.render

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.Core
import noobroutes.config.Config
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.AlwaysActive
import noobroutes.features.settings.impl.*
import noobroutes.font.FontType
import noobroutes.ui.ColorPalette
import noobroutes.ui.clickgui.ClickGui
import noobroutes.ui.hud.EditHUDGui
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.render.Color
import noobroutes.utils.skyblock.LocationUtils
import org.lwjgl.input.Keyboard

@AlwaysActive
object  ClickGUIModule: Module(
    name = "Click Gui",
    Keyboard.KEY_NONE,
    category = Category.RENDER,
    description = "Allows you to customize the GUI."
) {
    val hideChat by BooleanSetting("Hide Chat in GUIs", description = "Hides the minecraft chat in any NoobRoutes gui", default = false)
    val blur by BooleanSetting(
        "Blur",
        true,
        description = "Toggles the background blur for the gui. Requires the menu to be reopened"
    )
    val enableNotification by BooleanSetting(
        "Enable notifications",
        true,
        description = "Shows you a notification in chat when you toggle an option with a keybind."
    )

    val color by ColorSetting(
        "Gui Color",
        Color(57, 191, 60),
        allowAlpha = false,
        description = "Color theme in the gui."
    )
    val selectedPrefix by StringSetting("prefix", "", description = "leave empty to use original")

    val switchType by BooleanSetting("Switch Type", true, description = "Switches the type of the settings in the gui.")
    val forceHypixel by BooleanSetting(
        "Force Hypixel",
        false,
        description = "Forces the hypixel check to be on (not recommended)."
    )
    val devMode by BooleanSetting("Dev Mode", false, description = "Enables dev debug messages")

    val action by ActionSetting(
        "Open Example Hud",
        description = "Opens an example hud to allow configuration of huds."
    ) {
        Core.display = EditHUDGui
    }
    val font by SelectorSetting("Font", "NUNITO", FontType.entries.map { it.name }.toCollection(ArrayList()), description = "")

    @SubscribeEvent
    fun onTick(tickEvent: TickEvent.ClientTickEvent) {
        if (tickEvent.isEnd) return

        ColorPalette.defaultPalette.font = FontType.entries.firstOrNull { it.name == font } ?: return
    }

    private var joined by BooleanSetting("First join", false, hidden = true, "")
    var lastSeenVersion: String by StringSetting("Last seen version", "1.0.0", hidden = true, description = "")
    var firstTimeOnVersion = false

    val panelX = mutableMapOf<Category, NumberSetting<Float>>()
    val panelY = mutableMapOf<Category, NumberSetting<Float>>()
    val panelExtended = mutableMapOf<Category, BooleanSetting>()
    var searchBarX = +NumberSetting<Float>("searchBarX", 0f, hidden = true, description = "")
        private set
    var searchBarY = +NumberSetting<Float>("searchBarY", 0f, hidden = true, description = "")
        private set
    var editGuiX = +NumberSetting<Float>("editGuiX", 100f, hidden = true, description = "")
        private set
    var editGuiY = +NumberSetting<Float>("editGuiY", 200f, hidden = true, description = "")
        private set


    init {
        execute(250) {
            if (joined) destroyExecutor()
            if (!LocationUtils.isInSkyblock) return@execute
            joined = true
            Config.save()
        }
        resetPositions()
    }

    fun resetPositions() {
        Category.entries.forEach {
            val incr = 10f + 260f * it.ordinal
            panelX.getOrPut(it) { +NumberSetting(it.name + ",x", default = incr, hidden = true, description = "") }.value = incr
            panelY.getOrPut(it) { +NumberSetting(it.name + ",y", default = 10f, hidden = true, description = "") }.value = 10f
            panelExtended.getOrPut(it) { +BooleanSetting(
                it.name + ",extended",
                default = true,
                hidden = true,
                description = ""
            )
            }.enabled = true
        }
        val searchIncrement = 10f + 260f * Category.entries.size
        searchBarX = NumberSetting<Float>(
            "searchBarX",
            default = searchIncrement,
            hidden = true,
            description = ""
        ).apply { value = searchIncrement }
        searchBarY = NumberSetting<Float>(
            "searchBarY",
            default = 10f,
            hidden = true,
            description = ""
        ).apply { value = 10f }
    }

    override fun onKeybind() {
        this.toggle()
    }

    override fun onEnable() {

        Core.display = ClickGui
        super.onEnable()
        toggle()
    }
}