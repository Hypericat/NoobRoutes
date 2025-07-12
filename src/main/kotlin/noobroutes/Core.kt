package noobroutes

import kotlinx.coroutines.*
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.entity.player.EntityPlayer
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.config.Config
import noobroutes.features.dungeon.brush.BrushModule
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.features.render.ClickGUIModule
import noobroutes.features.routes.AutoRoute
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

    }

    fun postInit() {
        File(mc.mcDataDir, "config/noobroutes").takeIf { !it.exists() }?.mkdirs()
    }

    fun onFMLServerStopped() {
        BrushModule.saveConfig()
        BrushModule.editMode = false
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
        BrushModule.loadConfig()
    }
    private var lastChatVisibility: EntityPlayer.EnumChatVisibility? = null
    private var inUI = false

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        lastChatVisibility?.let { mc.gameSettings.chatVisibility = it }
        lastChatVisibility = null
        inUI = false
    }

    @SubscribeEvent
    fun onGuiClose(event: GuiOpenEvent){
        if (event.gui == null) {
            lastChatVisibility?.let { mc.gameSettings.chatVisibility = it }
            lastChatVisibility = null
            inUI = false
        }
    }

    @SubscribeEvent
    fun onRenderHUD(event: RenderGameOverlayEvent.Pre) {
        if (inUI && event.type == RenderGameOverlayEvent.ElementType.HOTBAR) {
            event.isCanceled = true
        }
    }

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (display == null) return
        lastChatVisibility = mc.gameSettings.chatVisibility
        inUI = true
        if (ClickGUIModule.hideChat) {
            mc.gameSettings.chatVisibility = EntityPlayer.EnumChatVisibility.HIDDEN
        }
        mc.displayGuiScreen(display)
        display = null
    }
}