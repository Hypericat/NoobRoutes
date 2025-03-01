package com.github.wadey3636.noobroutes

import com.github.wadey3636.noobroutes.commands.NoobRoutesCommand
import net.minecraft.client.Minecraft
import net.minecraft.client.renderer.GlStateManager
import net.minecraft.init.Blocks
import net.minecraftforge.client.ClientCommandHandler
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.common.event.FMLInitializationEvent


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
        println("Dirt: ${Blocks.dirt.unlocalizedName}")
	    // Below is a demonstration of an access-transformed class access.
	    println("Color State: " + GlStateManager.Color());
    }
}
