package com.github.wadey3636.noobroutes.features

import com.github.wadey3636.noobroutes.utils.ClientUtils
import me.defnotstolen.features.Category
import me.defnotstolen.features.Module
import me.defnotstolen.features.settings.impl.BooleanSetting
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import org.lwjgl.input.Keyboard
import kotlin.math.floor

object Simulation : Module(
    name = "Simulation",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "Simulates 500 Speed / lava bounces"
) {
    private val speed by BooleanSetting("500 Speed", true, description = "Simulates 500 Speed in singleplayer")
    private val lava by BooleanSetting("Lava Bounce", true, description = "Simulates Lava Bounces in singleplayer")
    private var inLava = false



    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {

        if (!mc.isSingleplayer) return
        if (mc.thePlayer == null) return

        if (speed) {
            mc.thePlayer.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.movementSpeed).baseValue = 0.5
            mc.thePlayer.capabilities?.setPlayerWalkSpeed(0.5F)
        }

        if (!lava) return

        if (mc.thePlayer.isInLava ||
            mc.theWorld.getBlockState(BlockPos(floor(mc.thePlayer.posX), floor(mc.thePlayer.posY), floor(mc.thePlayer.posZ))).block == Blocks.rail
            && mc.thePlayer.posY - floor(mc.thePlayer.posY) < 0.1) {

            inLava = true
            ClientUtils.clientScheduleTask(6) {
                inLava = false
                mc.thePlayer.setVelocity(mc.thePlayer.motionX, 3.5, mc.thePlayer.motionZ)

            }
        }
    }
}