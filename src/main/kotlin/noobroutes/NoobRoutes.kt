package noobroutes

import gg.essential.elementa.font.FontRenderer
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import noobroutes.Core.mc
import noobroutes.commands.*
import noobroutes.events.BossEventDispatcher
import noobroutes.events.EventDispatcher
import noobroutes.features.ModuleManager
import noobroutes.features.floor7.autop3.AutoP3MovementHandler
import noobroutes.font.FontType
import noobroutes.ui.clickgui.ClickGUI
import noobroutes.utils.*
import noobroutes.utils.clock.Executor
import noobroutes.utils.render.RenderUtils
import noobroutes.utils.render.RenderUtils2D
import noobroutes.utils.render.Renderer
import noobroutes.utils.render.initUIFramebufferStencil
import noobroutes.utils.routes.RouteUtils
import noobroutes.utils.routes.SecretUtils
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.dungeon.Dungeon
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import java.io.File


const val MODID = "@MOD_ID@"

@Mod(modid = MODID, useMetadata = true)
class NoobRoutes {
    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        listOf(
            NoobRoutesCommand(),
            AutoP3Command(),
            PathCommand(),
            CopyCommand(),
            //AuraTestCommand(),
            //EtherwarpTestCommand(),
            AutoRouteCommand(),
            DynamicRouteCommand(),
            YawPitchCommand(),
            BrushCommand(),
            NoobCommand()
        ).forEach {
            ClientCommandHandler.instance.registerCommand(it)
        }

        listOf(
            Core,
            ModuleManager,
            Executor,
            Renderer,
            RenderUtils2D,
            RenderUtils,
            ClickGUI,
            Scheduler,
            PacketUtils,
            NettyS2DPacketInterceptor,
            AuraManager,
            Utils,
            LocationUtils,
            EventDispatcher,
            SwapManager,
            BowUtils,
            RotationUtils,
            BossEventDispatcher,
            SecretUtils,
            RouteUtils,
            Dungeon,
            DungeonUtils,
            AutoP3MovementHandler
        ).forEach {
            MinecraftForge.EVENT_BUS.register(it)
        }
        //this is probably done already by other mods, but it wasn't in the dev env, so I am doing it here
        FontRenderer.initShaders()
        FontType.entries.forEach {
            it.font.init()
        }
    }

    @Mod.EventHandler
    fun postInit(event: FMLPostInitializationEvent) {
        Core.postInit()
        initUIFramebufferStencil()
    }

    @Mod.EventHandler
    fun loadComplete(event: FMLLoadCompleteEvent) {
        File(mc.mcDataDir, "config/noobroutes").takeIf { !it.exists() }?.mkdirs()
        Core.loadComplete()
        ModuleManager.addModules()

    }

}
