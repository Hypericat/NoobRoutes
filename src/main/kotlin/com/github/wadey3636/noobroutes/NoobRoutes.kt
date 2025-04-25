package com.github.wadey3636.noobroutes

import com.github.wadey3636.noobroutes.commands.AuraTestCommand
import com.github.wadey3636.noobroutes.commands.AutoP3Command
import com.github.wadey3636.noobroutes.commands.NoobRoutesCommand
import com.github.wadey3636.noobroutes.features.Blink
import com.github.wadey3636.noobroutes.utils.*
import me.noobmodcore.Core
import me.noobmodcore.Core.mc
import me.noobmodcore.events.EventDispatcher
import me.noobmodcore.features.ModuleManager
import me.noobmodcore.font.OdinFont
import me.noobmodcore.ui.clickgui.ClickGUI
import me.noobmodcore.utils.clock.Executor
import me.noobmodcore.utils.render.RenderUtils
import me.noobmodcore.utils.render.RenderUtils2D
import me.noobmodcore.utils.render.Renderer
import me.noobmodcore.utils.skyblock.LocationUtils
import me.noobmodcore.utils.skyblock.dungeon.ScanUtils
import net.minecraft.client.Minecraft
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
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

        listOf(
            NoobRoutesCommand(),
            AutoP3Command(),
            AuraTestCommand()
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
            ScanUtils,
            AuraManager,
            Utils,
            LocationUtils,
            EventDispatcher,
            SwapManager,
            BowUtils,
            RotationUtils,
            SilentRotator

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
