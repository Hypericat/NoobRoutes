package noobroutes

import kotlinx.coroutines.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.config.Config
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.features.routes.AutoRoute
import noobroutes.font.MinecraftFont
import noobroutes.ui.clickgui.ClickGUI
import noobroutes.ui.util.shader.RoundedRect
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

object Core {
    @JvmStatic
    val mc: Minecraft = Minecraft.getMinecraft()


    const val VERSION = "@VER@"
    val scope = CoroutineScope(SupervisorJob() + EmptyCoroutineContext)
    val logger: Logger = LogManager.getLogger("NoobRoutes")

    var display: GuiScreen? = null

    fun init() {
        MinecraftFont.init()


    }

    fun postInit() {
        File(mc.mcDataDir, "config/noobroutes").takeIf { !it.exists() }?.mkdirs()
    }

    fun loadComplete() {
        runBlocking(Dispatchers.IO) {
            launch {
                Config.load()
            }.join()
        }
        AutoP3.loadRings()
        AutoRoute.loadFile()
        ClickGUI.init()
        RoundedRect.initShaders()

    }
    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (display == null) return
        mc.displayGuiScreen(display)
        display = null
    }
}