package com.github.wadey3636.noobroutes

import com.github.wadey3636.noobroutes.commands.AutoP3Command
import com.github.wadey3636.noobroutes.commands.NoobRoutesCommand
import me.odinmain.OdinMain
import me.odinmain.OdinMain.mc
import me.odinmain.config.Config
import me.odinmain.features.ModuleManager
import me.odinmain.features.impl.render.ClickGUIModule
import me.odinmain.font.OdinFont
import me.odinmain.ui.clickgui.ClickGUI
import me.odinmain.utils.clock.Executor
import me.odinmain.utils.render.RenderUtils
import me.odinmain.utils.render.RenderUtils2D
import me.odinmain.utils.render.Renderer
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.Mod.EventHandler
import net.minecraftforge.fml.common.event.FMLInitializationEvent
import net.minecraftforge.fml.common.event.FMLLoadCompleteEvent
import java.io.File


@Mod(modid = "noobroutes", useMetadata = true)
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
        ClientCommandHandler.instance.registerCommand(NoobRoutesCommand())
        ClientCommandHandler.instance.registerCommand(AutoP3Command())
        println("Dirt: ${Blocks.dirt.unlocalizedName}")
	    // Below is a demonstration of an access-transformed class access.
	    println("Color State: " + GlStateManager.Color());
        val Modules = listOf(
            OdinMain,
            ModuleManager,
            Executor,
            Renderer,
            RenderUtils2D,
            RenderUtils,
            ClickGUI
        )
        Modules.forEach {
            MinecraftForge.EVENT_BUS.register(it)
        }
    }
    @Mod.EventHandler
    fun loadComplete(event: FMLLoadCompleteEvent) {
        ModuleManager.addModules()
        OdinMain.loadComplete()

        File(mc.mcDataDir, "config/noobroutes").takeIf { !it.exists() }?.mkdirs()
        Config.load()
    }

}
