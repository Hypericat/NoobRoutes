package me.noobmodcore

import com.github.wadey3636.noobroutes.features.floor7.AutoP3
import kotlinx.coroutines.*

import me.noobmodcore.config.Config
import me.noobmodcore.font.OdinFont
import me.noobmodcore.ui.clickgui.ClickGUI
import me.noobmodcore.ui.util.shader.RoundedRect
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

object Core {
    val mc: Minecraft = Minecraft.getMinecraft()


    const val VERSION = "@VER@"
    val scope = CoroutineScope(SupervisorJob() + EmptyCoroutineContext)
    val logger: Logger = LogManager.getLogger("NoobRoutes")

    var display: GuiScreen? = null

    fun init() {
        /*
        listOf(
            LocationUtils, ClientUtils, PlayerUtils,
            RenderUtils, Renderer, DungeonUtils, KuudraUtils,
            EventDispatcher, Executor, ModuleManager,
            SkyblockPlayer,
            ScanUtils, HighlightRenderer, //OdinUpdater,
            RenderUtils2D,
            this
        ).forEach { MinecraftForge.EVENT_BUS.register(it) }

         */
        OdinFont.init()


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
