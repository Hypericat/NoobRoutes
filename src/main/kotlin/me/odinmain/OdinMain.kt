package me.odinmain

import kotlinx.coroutines.*

import me.odinmain.config.Config
import me.odinmain.events.EventDispatcher
import me.odinmain.features.ModuleManager
import me.odinmain.font.OdinFont
import me.odinmain.ui.clickgui.ClickGUI
import me.odinmain.ui.util.shader.RoundedRect
import me.odinmain.utils.ServerUtils
import me.odinmain.utils.clock.Executor
import me.odinmain.utils.render.HighlightRenderer
import me.odinmain.utils.render.RenderUtils
import me.odinmain.utils.render.RenderUtils2D
import me.odinmain.utils.render.Renderer
import me.odinmain.utils.skyblock.KuudraUtils
import me.odinmain.utils.skyblock.LocationUtils
import me.odinmain.utils.skyblock.PlayerUtils
import me.odinmain.utils.skyblock.SkyblockPlayer
import me.odinmain.utils.skyblock.dungeon.DungeonUtils
import me.odinmain.utils.skyblock.dungeon.ScanUtils
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Logger
import java.io.File
import kotlin.coroutines.EmptyCoroutineContext

object OdinMain {
    val mc: Minecraft = Minecraft.getMinecraft()


    const val VERSION = "@VER@"
    val scope = CoroutineScope(SupervisorJob() + EmptyCoroutineContext)
    val logger: Logger = LogManager.getLogger("NoobRoutes")

    var display: GuiScreen? = null

    fun init() {
        /*
        listOf(
            LocationUtils, ServerUtils, PlayerUtils,
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
