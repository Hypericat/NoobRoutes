package com.github.wadey3636.noobroutes

import com.github.wadey3636.noobroutes.commands.AuraTestCommand
import com.github.wadey3636.noobroutes.commands.AutoP3Command
import com.github.wadey3636.noobroutes.commands.NoobRoutesCommand
import com.github.wadey3636.noobroutes.features.Blink
import com.github.wadey3636.noobroutes.utils.*
import io.netty.channel.ChannelHandlerContext
import io.netty.handler.codec.MessageToMessageDecoder
import me.defnotstolen.Core
import me.defnotstolen.Core.mc
import me.defnotstolen.events.EventDispatcher
import me.defnotstolen.features.ModuleManager
import me.defnotstolen.font.OdinFont
import me.defnotstolen.ui.clickgui.ClickGUI
import me.defnotstolen.utils.clock.Executor
import me.defnotstolen.utils.render.RenderUtils
import me.defnotstolen.utils.render.RenderUtils2D
import me.defnotstolen.utils.render.Renderer
import me.defnotstolen.utils.skyblock.LocationUtils
import me.defnotstolen.utils.skyblock.dungeon.ScanUtils
import me.defnotstolen.utils.skyblock.modMessage
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraft.network.Packet
import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.network.FMLNetworkEvent
import java.io.File
import javax.rmi.CORBA.Util


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
            EventDispatcher

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
