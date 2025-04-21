package com.github.wadey3636.noobroutes

import com.github.wadey3636.noobroutes.commands.AuraTestCommand
import com.github.wadey3636.noobroutes.commands.AutoP3Command
import com.github.wadey3636.noobroutes.commands.NoobRoutesCommand
import com.github.wadey3636.noobroutes.features.Blink
import com.github.wadey3636.noobroutes.utils.*
import me.modcore.Core
import me.modcore.Core.mc
import me.modcore.events.EventDispatcher
import me.modcore.features.ModuleManager
import me.modcore.font.OdinFont
import me.modcore.ui.clickgui.ClickGUI
import me.modcore.utils.clock.Executor
import me.modcore.utils.render.RenderUtils
import me.modcore.utils.render.RenderUtils2D
import me.modcore.utils.render.Renderer
import me.modcore.utils.skyblock.LocationUtils
import me.modcore.utils.skyblock.dungeon.ScanUtils
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
            ClientUtils,
            PacketUtils,
            NettyS2DPacketInterceptor,
            ScanUtils,
            AuraManager,
            Utils,
            LocationUtils,
            EventDispatcher,
            SwapManager,
            BowUtils

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
