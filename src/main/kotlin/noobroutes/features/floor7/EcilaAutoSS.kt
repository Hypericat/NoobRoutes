package noobroutes.features.floor7

import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.util.MovingObjectPosition
import net.minecraftforge.event.entity.player.PlayerInteractEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent
import noobroutes.events.BossEventDispatcher.inBoss
import noobroutes.events.impl.BlockChangeEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.AuraManager

//this is not in module manager yet, also need might need offtick?


object EcilaAutoSS : Module(
    name = "EcilaAutoSS",
    category = Category.FLOOR7,
    description = "Does SS, (stolen from Ecila)"
) {
    private val startButton = BlockPos(110, 121, 91)

    private var delay by NumberSetting(
        "Delay",
        200.0,
        50.0,
        500.0,
        10.0,
        unit = "ms",
        description = "The delay for next click"
    )

    var startDelay1 by NumberSetting(
        "Autostart delay",
        125.0,
        50.0,
        200.0,
        1.0,
        unit = "ms",
        description = "The delay used for starting autoSS"
    )

    var startDelay2 by NumberSetting(
        "Autostart delay",
        125.0,
        50.0,
        200.0,
        1.0,
        unit = "ms",
        description = "The delay used for starting autoSS"
    )

    private var clicks = ArrayList<BlockPos?>()
    private var doingSS = false
    private var next = false
    private var doneFirst = false
    private var clicked = false
    private var progress = 0
    private var lastClick = 0.0

    override fun onEnable() {
        reset()
        doingSS = false
        clicked = false
        super.onEnable()
    }

    fun reset() {
        clicks.clear()
        next = false
        progress = 0
        doneFirst = false
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldEvent.Load) {
        reset()
        doingSS = false
        clicked = false
    }

    @SubscribeEvent
    fun onTick(event: RenderTickEvent) {
        if (!inBoss) return
        val detect = mc.theWorld.getBlockState(BlockPos(110, 123, 92)).block
        if (mc.thePlayer.getDistanceSq(startButton) > 25.0 || mc.objectMouseOver == null) return
        var device = false
        for (entity in mc.theWorld.loadedEntityList) {
            if (entity is EntityArmorStand) {
                if (entity.getDistanceToEntity(mc.thePlayer as Entity) < 6.0f && entity.displayName.unformattedText.contains(
                        "Device"
                    )
                ) device = true
            }
        }
        if (!device) return
        if (!clicked) {
            clicked = true
            doingSS = true
            reset()
            (Thread {
                try {
                    AuraManager.auraBlock(startButton)
                    Thread.sleep(startDelay1.toLong())
                    AuraManager.auraBlock(startButton)
                    Thread.sleep(startDelay2.toLong())
                    AuraManager.auraBlock(startButton)
                } catch (_: Exception) {
                }
            }).start()
            return
        }
        if (detect == Blocks.air) {
            progress = 0
        } else if (detect == Blocks.cobblestone && doingSS) {
            val currentTime = System.currentTimeMillis()
            if (lastClick + delay.toLong() <= currentTime) {
                if (!doneFirst) {
                    if (clicks.size == 3) clicks.removeAt(0)
                    doneFirst = true
                }
                if (progress < clicks.size) {
                    val next = clicks[progress] ?: return
                    val nextBlock = mc.theWorld.getBlockState(next).block
                    if (nextBlock == Blocks.cobblestone) {
                        progress++
                        lastClick = currentTime.toDouble()
                        AuraManager.auraBlock(next)
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val mop = mc.objectMouseOver ?: return
        if (mop.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK && startButton == event.pos && startButton == mop.hitVec && event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) reset()
    }

    @SubscribeEvent
    fun onBlockChange(event: BlockChangeEvent) {
        if (event.pos.x == 111 && event.pos.y >= 120 && event.pos.y <= 123 && event.pos.z >= 92 && event.pos.z <= 95) {
            val button = BlockPos(110, event.pos.z, event.pos.z)
            if (event.update.block == Blocks.diamond_block && (!clicks.contains(button) || !doneFirst)) clicks.add(
                button
            )
        }
    }
}