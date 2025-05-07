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
){

    private var delay by NumberSetting("Delay", 200.0, 50.0, 500.0, 10.0, unit = "ms", description = "The delay for next click")

    var startDelay1 by NumberSetting("Autostart delay", 125.0, 50.0, 200.0, 1.0, unit = "ms", description = "The delay used for starting autoSS")

    var startDelay2 by NumberSetting("Autostart delay", 125.0, 50.0, 200.0, 1.0, unit = "ms", description = "The delay used for starting autoSS")

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
    fun onWorldChange(e: WorldEvent.Load?) {
        reset()
        doingSS = false
        clicked = false
    }

    @SubscribeEvent
    fun onTick(event: RenderTickEvent?) {
        if (!AutoP3.inBoss) return
        val detect = mc.theWorld.getBlockState(BlockPos(110, 123, 92)).block
        val startButton = BlockPos(110, 121, 91)
        val mouseOver: MovingObjectPosition? = mc.objectMouseOver
        if (mouseOver == null || mc.thePlayer.getDistanceSq(startButton) > 25.0) return
        var device = false
        for (entity in mc.theWorld.loadedEntityList) {
            if (entity is EntityArmorStand) {
                val stand = entity
                if (stand.getDistanceToEntity(mc.thePlayer as Entity) < 6.0f && stand.displayName.unformattedText.contains("Device")) device = true
            }
        }
        if (!device) return
        if (!this.clicked) {
            this.clicked = true
            this.doingSS = true
            reset()
            (Thread(Runnable {
                 try {
                     AuraManager.auraBlock(startButton)
                     Thread.sleep(startDelay1.toLong())
                     AuraManager.auraBlock(startButton)
                     Thread.sleep(startDelay2.toLong())
                     AuraManager.auraBlock(startButton)
                 } catch (exception: Exception) {
                 }
             })).start()
             return
        }
        if (detect === Blocks.air) {
            this.progress = 0
        } else if (detect === Blocks.cobblestone && this.doingSS) {
            val currentTime = System.currentTimeMillis()
            if (this.lastClick + delay.toLong() <= currentTime) {
                if (!this.doneFirst) {
                    if (this.clicks.size === 3) clicks.removeAt(0)
                    this.doneFirst = true
                }
                if (this.progress < this.clicks.size) {
                    val next = this.clicks[this.progress]
                    if (next == null) return
                    val nextBlock = mc.theWorld.getBlockState(next).block
                    if (nextBlock === Blocks.cobblestone) {
                        this.progress++
                        this.lastClick = currentTime.toDouble()
                        AuraManager.auraBlock(next)
                    }
                }
            }
        }
    }

    @SubscribeEvent
    fun onPlayerInteract(event: PlayerInteractEvent) {
        val mop = mc.objectMouseOver
        if (mop == null) return
        val startButton = BlockPos(110, 121, 91)
        //if (mop.blockPos === MovingObjectPosition.MovingObjectType.BLOCK && startButton == event.pos && startButton == mop.hitVec && event.action == PlayerInteractEvent.Action.RIGHT_CLICK_BLOCK) reset()
    }

    @SubscribeEvent
    fun onBlockChange(event: BlockChangeEvent) {
        if (event.pos.x == 111 && event.pos.y >= 120 && event.pos.y <= 123 && event.pos.z >= 92 && event.pos.z <= 95) {
            val button = BlockPos(110, event.pos.z, event.pos.z)
            if (event.update.block === Blocks.diamond_block && (!this.clicks.contains(button) || !this.doneFirst)) this.clicks.add(
                button
            )
        }
    }
}