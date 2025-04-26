package noobroutes.features.move

import noobroutes.events.impl.PacketEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.floor7.AutoP3
import net.minecraft.network.play.server.S27PacketExplosion
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import org.lwjgl.input.Keyboard

object NoTnT: Module(
    name = "No TnT kb",
    Keyboard.KEY_NONE,
    category = Category.MOVE,
    description = "cancels tnt kb in f7/m7"
) {
    @SubscribeEvent
    fun onKb(event: PacketEvent.Receive) {
        if (event.packet is S27PacketExplosion && AutoP3.inBoss) event.isCanceled = true
    }
}