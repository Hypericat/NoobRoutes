package noobroutes.features.floor7

import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.move.LavaClip
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.utils.Scheduler
import org.lwjgl.input.Keyboard
import kotlin.math.floor

object Simulation : Module(
    name = "Simulation",
    Keyboard.KEY_NONE,
    category = Category.MISC,
    description = "Simulates 500 Speed / lava bounces"
) {
    private val speed by BooleanSetting("500 Speed", true, description = "Simulates 500 Speed in singleplayer")
    private val lava by BooleanSetting("Lava Bounce", true, description = "Simulates Lava Bounces in singleplayer")
    private var force by BooleanSetting("force Singleplayer", false, description = "always does this also on servers (ban in 3, 2, 1, rn)")

    private var inLava = false



    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {

        if (!mc.isSingleplayer && !force) return
        if (mc.thePlayer == null) return

        if (speed) {
            mc.thePlayer.getEntityAttribute(net.minecraft.entity.SharedMonsterAttributes.movementSpeed).baseValue = 0.5
            mc.thePlayer.capabilities?.setPlayerWalkSpeed(0.5F)
        }

        if (!lava || inLava) return

        if (LavaClip.customInLavaCheck() ||
            mc.theWorld.getBlockState(BlockPos(floor(mc.thePlayer.posX), floor(mc.thePlayer.posY), floor(mc.thePlayer.posZ))).block == Blocks.rail
            && mc.thePlayer.posY - floor(mc.thePlayer.posY) < 0.1) {

            inLava = true
            Scheduler.schedulePreTickTask(2) {
                mc.thePlayer.setVelocity(mc.thePlayer.motionX, 3.5, mc.thePlayer.motionZ)

            }
            Scheduler.schedulePreTickTask(3) { inLava = false }
        }
    }

    @SubscribeEvent
    fun onUnload(event: WorldEvent.Unload) {
        force = false
    }

    @SubscribeEvent
    fun onLoad(event: WorldEvent.Load) {
        force = false
    }
}