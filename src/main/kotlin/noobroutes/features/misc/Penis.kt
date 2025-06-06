package noobroutes.features.misc

import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.utils.PacketUtils
import noobroutes.utils.Scheduler
import noobroutes.utils.isAir
import noobroutes.utils.toBlockPos
import org.lwjgl.input.Keyboard

object Penis: Module(
    name = "Penis Builder",
    Keyboard.KEY_NONE,
    category = Category.MISC,
    description = "it builds a penis north of u"
) {
    private val buildKey by KeybindSetting("build key", Keyboard.KEY_NONE, "builds on keypress").onPress { buildPenis() }

    private fun buildPenis() {
        val playerBlock = mc.thePlayer.positionVector.subtract(0.0, 1.0, 0.0).toBlockPos()
        if (isAir(playerBlock.north()) || isAir(playerBlock.north().east()) || isAir(playerBlock.north().west()) || mc.thePlayer.heldItem.stackSize < 5) return
        mc.thePlayer.setPosition(playerBlock.x + 0.5, playerBlock.y + 1.0, playerBlock.z + 0.5)
        mc.thePlayer.jump()
        Scheduler.schedulePreTickTask {PacketUtils.sendPacket(C08PacketPlayerBlockPlacement(playerBlock.north(), 1, mc.thePlayer.heldItem, 0.5F, 1.0F, 0.5F))}
        Scheduler.schedulePreTickTask(1) {PacketUtils.sendPacket(C08PacketPlayerBlockPlacement(playerBlock.north().east(), 1, mc.thePlayer.heldItem, 0.5F, 1.0F, 0.5F))}
        Scheduler.schedulePreTickTask(2) {PacketUtils.sendPacket(C08PacketPlayerBlockPlacement(playerBlock.north().west(), 1, mc.thePlayer.heldItem, 0.5F, 1.0F, 0.5F))}
        Scheduler.schedulePreTickTask(3) {PacketUtils.sendPacket(C08PacketPlayerBlockPlacement(playerBlock.north().up(), 1, mc.thePlayer.heldItem, 0.5F, 1.0F, 0.5F))}
        Scheduler.schedulePreTickTask(4) {PacketUtils.sendPacket(C08PacketPlayerBlockPlacement(playerBlock.north().up().up(), 1, mc.thePlayer.heldItem, 0.5F, 1.0F, 0.5F))}
    }
}