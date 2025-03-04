package com.github.wadey3636.noobroutes.utils

import com.github.wadey3636.noobroutes.features.AutoP3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import me.odinmain.OdinMain.mc
import me.odinmain.utils.render.RenderUtils.renderX
import me.odinmain.utils.render.RenderUtils.renderY
import me.odinmain.utils.render.RenderUtils.renderZ
import me.odinmain.utils.skyblock.modMessage
import net.minecraft.client.settings.KeyBinding
import net.minecraft.util.Vec3
import org.lwjgl.input.Keyboard
import kotlin.math.pow

object AutoP3Utils {

    private val keyBindings = listOf(
        mc.gameSettings.keyBindForward,
        mc.gameSettings.keyBindLeft,
        mc.gameSettings.keyBindRight,
        mc.gameSettings.keyBindBack
    )

    fun unPressKeys() {
        keyBindings.forEach { KeyBinding.setKeyBindState(it.keyCode, false) }
        modMessage("unpressing")
    }

    private var walking = false
    private var direction = 0F

    fun startWalk(dir: Float) {
        walking = true
        direction  = dir
    }

    @SubscribeEvent
    fun walk(event: TickEvent.ClientTickEvent) {
        if (!walking) return
        if (event.phase == TickEvent.Phase.END) return
        if (keyBindings.any { it.isKeyDown }) {
            walking = false
            return
        }
        val speed = mc.thePlayer.capabilities.walkSpeed
        mc.thePlayer.motionX = speed * 2.806 * Utils.xPart(direction)
        mc.thePlayer.motionZ = speed * 2.806 * Utils.zPart(direction)
    }

    fun distanceToRing(coords: Vec3): Double {
        if(AutoP3.frame) return (coords.xCoord-mc.thePlayer.renderX).pow(2)+(coords.zCoord-mc.thePlayer.renderZ).pow(2).pow(0.5)
        return (coords.xCoord-mc.thePlayer.posX).pow(2)+(coords.zCoord-mc.thePlayer.posZ).pow(2).pow(0.5)
    }

    fun ringCheckY(coords: Vec3): Boolean {
        if(AutoP3.frame) return coords.yCoord <= mc.thePlayer.renderY && coords.yCoord + 1 > mc.thePlayer.renderY
        return coords.yCoord <= mc.thePlayer.posY && coords.yCoord + 1 > mc.thePlayer.posY
    }
}
