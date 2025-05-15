package noobroutes.features.move

import net.minecraft.network.play.server.S27PacketExplosion
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.events.BossEventDispatcher.inBoss
import noobroutes.events.impl.PacketEvent
import noobroutes.features.Category
import noobroutes.features.Module
import org.lwjgl.input.Keyboard

object NoTnT: Module(
    name = "No TnT kb",
    Keyboard.KEY_NONE,
    category = Category.MOVE,
    description = "cancels tnt kb in f7/m7"
) {
    @SubscribeEvent
    fun onKb(event: PacketEvent.Receive) {
        if (event.packet is S27PacketExplosion && inBoss) event.isCanceled = true
    }
}