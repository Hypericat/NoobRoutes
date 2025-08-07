package noobroutes.features.floor7

import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.events.BossEventDispatcher
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.events.impl.MoveEntityWithHeadingEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.utils.BlockUtils.removeHitbox
import noobroutes.utils.BlockUtils.restoreHitbox
import noobroutes.utils.isAir
import org.lwjgl.input.Keyboard
import kotlin.math.abs


object CoreClip: Module(
    name = "CoreClip",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "The cleanest CoreClip™ in the west, east, north, and south. (Much nicer with no push in No Debuff)"
) {
    private const val CORE_Y = 115.0

    private const val CORE_Z_EDGE1 = 53.7
    private const val CORE_Z_EDGE2 = 55.3

    private const val MAX_INSIDE_BLOCK = 0.0624

    private const val SLIGHTLY_IN1 = CORE_Z_EDGE1 + MAX_INSIDE_BLOCK
    private const val SLIGHTLY_IN2 = CORE_Z_EDGE2 - MAX_INSIDE_BLOCK

    private val MIDDLE_CORE_BLOCK = BlockPos(54, 115, 54) //this like is a const trust

    private var inDoorTicks = 0

    private inline val doorOpen: Boolean
         get() = isAir(MIDDLE_CORE_BLOCK)

    fun inDoor(posZ: Double): Boolean {
        return posZ in CORE_Z_EDGE1 ..CORE_Z_EDGE2 && posZ != SLIGHTLY_IN1 && posZ != SLIGHTLY_IN2
    }

    @SubscribeEvent
    fun onMovePre(event: MoveEntityWithHeadingEvent.Pre) {
        if (abs(MIDDLE_CORE_BLOCK.z - mc.thePlayer.posZ) > 3 || mc.thePlayer.isSneaking || !mc.thePlayer.onGround || doorOpen || !BossEventDispatcher.inF7Boss) {
            Blocks.gold_block.restoreHitbox()
            Blocks.barrier.restoreHitbox()
        }
        else {
            Blocks.gold_block.removeHitbox()
            Blocks.barrier.removeHitbox()
        }
    }

    @SubscribeEvent
    fun onMovePost(event: MoveEntityWithHeadingEvent.Post) {
        Blocks.gold_block.restoreHitbox()
        Blocks.barrier.restoreHitbox()
    }

    @SubscribeEvent()
    fun noInWall(event: MotionUpdateEvent.Pre) {
        if (!BossEventDispatcher.inF7Boss || doorOpen) return

        if (inDoor(event.z)) inDoorTicks++ else inDoorTicks = 0

        if (event.y != CORE_Y || inDoorTicks == 0) return

        val goatedZ = listOf(SLIGHTLY_IN1, SLIGHTLY_IN2).minBy { abs(it - if (inDoorTicks == 1) mc.thePlayer.prevPosZ else event.z) }

        event.z = goatedZ
    }
}