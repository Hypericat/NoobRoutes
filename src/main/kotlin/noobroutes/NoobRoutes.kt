package noobroutes

import net.minecraft.client.Minecraft
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
import noobroutes.features.dungeon.autoroute.AutoRouteUtils
import noobroutes.features.dungeon.autoroute.SecretUtils
import noobroutes.features.floor7.autop3.Blink
import noobroutes.features.move.AutoPath
import noobroutes.features.move.DynamicRoute
import noobroutes.font.OdinFont
import noobroutes.ui.clickgui.ClickGUI
import noobroutes.utils.*
import noobroutes.utils.clock.Executor
import noobroutes.utils.render.RenderUtils
import noobroutes.utils.render.RenderUtils2D
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.dungeon.ScanUtils
import noobroutes.utils.skyblock.dungeonscanning.Dungeon
import java.io.File


const val MODID = "noobroutes"

@Mod(modid = MODID, useMetadata = true)
class NoobRoutes {
    @Mod.EventHandler
    fun init(event: FMLInitializationEvent) {
        try {
            val resource: net.minecraft.client.resources.IResource = Minecraft.getMinecraft().resourceManager
                .getResource(net.minecraft.util.ResourceLocation("test:test.txt"))
            org.apache.commons.io.IOUtils.copy(resource.inputStream, java.lang.System.out)
        } catch (e: java.io.IOException) {
            throw java.lang.RuntimeException(e)
        }

        AutoPath.onInitKeys()

        listOf(
            NoobRoutesCommand(),
            AutoP3Command(),
            PathCommand(),
            CpCommand(),
            //AuraTestCommand(),
            //EtherwarpTestCommand(),
            AutoRouteCommand(),
            DynamicRouteCommand(),
            YawPitchCommand(),
            AutoBloodRushCommand()
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
            AutoP3Utils,
            Blink,
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
            AutoRouteUtils,
            Dungeon
        ).forEach {
            MinecraftForge.EVENT_BUS.register(it)
        }
        OdinFont.init()
    }
    @Mod.EventHandler
    fun postInit(event: FMLPostInitializationEvent) {
        Core.postInit()
    }

    @Mod.EventHandler
    fun loadComplete(event: FMLLoadCompleteEvent) {
        File(mc.mcDataDir, "config/noobroutes").takeIf { !it.exists() }?.mkdirs()
        Core.loadComplete()
        ModuleManager.addModules()

    }

    

}
