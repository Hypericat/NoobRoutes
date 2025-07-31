package noobroutes.features.move

import net.minecraft.client.settings.KeyBinding
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import noobroutes.events.impl.ClickEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.skyblockID
import org.lwjgl.input.Keyboard
import org.lwjgl.input.Mouse

object QOL: Module(
    name = "QOL",
    Keyboard.KEY_NONE,
    category = Category.MOVE,
    description = "some random stuff"
) {
    val speed by NumberSetting(name = "bhop speed", description = "when u jump how big the speed boost should be (0.2 is default)", min = 0.2f, max = 4f, default = 2f, increment = 0.05)
    val jumpDelay by NumberSetting(name = "jump delay", description = "when holding jump the delay beetween jumps (10 is default)", min = 0, max = 10, default = 0, increment = 1)
    val lavaFix by BooleanSetting("lava fix", false, description = "the shit from sby")
    private val offTickAotvC08 by BooleanSetting("Faster aotv?", false, description = "off ticks the c08( right click packet) when holding aotv to give 25ms faster response on average")

    private var cancelNext = false

    @SubscribeEvent
    fun onRightClick(event: ClickEvent.Right) {
        if (!offTickAotvC08 || mc.thePlayer.heldItem.skyblockID != "ASPECT_OF_THE_VOID") return

        if (mc.theWorld.getBlockState(mc.objectMouseOver.blockPos) == Blocks.chest) return

        devMessage(System.currentTimeMillis())
        event.isCanceled = true
        PlayerUtils.airClick()

        //cancelNext = true
    }

    @SubscribeEvent
    fun onC08(event: PacketEvent.Send) {
        if (!cancelNext || event.packet !is C08PacketPlayerBlockPlacement) return
        event.isCanceled = true
        cancelNext = false
    }
}