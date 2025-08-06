package noobroutes.features.misc

import net.minecraft.entity.Entity
import net.minecraft.entity.monster.EntityEnderman
import net.minecraft.util.MovingObjectPosition
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.DevOnly
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.RotationUtils
import noobroutes.utils.Scheduler
import noobroutes.utils.add
import noobroutes.utils.distanceSquaredTo
import noobroutes.utils.routes.RouteUtils
import org.lwjgl.input.Keyboard
import kotlin.math.pow

@DevOnly
object ZealotAura: Module(
    name = "Zealot Aura",
    Keyboard.KEY_NONE,
    category = Category.MISC,
    description = "shoots black men"
) {
    private val headShots by BooleanSetting(name = "headshots", description = "go for headshots or feet")
    private val range by NumberSetting(name = "Range", description = "how far the mobs can be away", min = 3.0, max = 100.0, default = 30.0)
    private val cooldown by NumberSetting(name = "Cooldown", description = "how long to wait beetween shots", min = 1, max = 20, default = 1, unit = "t")
    private val projectileSpeed by NumberSetting(name = "Speed", description = "how fast the projectile moves", min = 0.3, max = 3.0, default = 1.0)

    private val shotEntityList = mutableListOf<Int>()

    private var cooldownCounter = 0

    @SubscribeEvent
    fun onMotion(event: MotionUpdateEvent.Pre) {
        if (mc.thePlayer == null || mc.theWorld == null || mc.thePlayer.heldItem == null) return

        if (cooldownCounter > 0) {
            cooldownCounter--
            return
        }

        val entities = mc.theWorld.loadedEntityList.filter { !it.isInvisible &&
                it is EntityEnderman &&
                it.distanceSquaredTo(mc.thePlayer.positionVector) < range.pow(2) &&
                !shotEntityList.contains(it.entityId) &&
                !isBlockInWay(it.getLookAhead(), mc.thePlayer.getPositionEyes(0f))
        }

        if (entities.isEmpty()) return
        val first = entities.minBy { it.distanceSquaredTo(mc.thePlayer.positionVector) }

        val coords = first.getLookAhead()
        val yawPitch = RotationUtils.getYawAndPitch(coords, sneaking = false)
        event.yaw = yawPitch.first
        event.pitch = yawPitch.second

        shotEntityList.add(first.entityId)
        Scheduler.schedulePreTickTask(100) { shotEntityList.remove(first.entityId) }
        cooldownCounter = cooldown - 1 //theres always one tick of delay
        RouteUtils.rightClick()
    }

    private fun Entity.getLookAhead(): Vec3 {
        val entityEyePos = if (headShots) this.getPositionEyes(0f) else this.positionVector.add(0.0, 0.5, 0.0)
        val playerEyePos = mc.thePlayer.getPositionEyes(0f)

        val velocity = Vec3(this.motionX, this.motionY, this.motionZ)
        val distance = entityEyePos.distanceTo(playerEyePos)
        val timeToArrive = distance / projectileSpeed

        return Vec3(
            entityEyePos.xCoord + velocity.xCoord * timeToArrive,
            entityEyePos.yCoord + velocity.yCoord * timeToArrive,
            entityEyePos.zCoord + velocity.zCoord * timeToArrive
        )
    }

    private fun isBlockInWay(start: Vec3, end: Vec3): Boolean {
        val result = mc.theWorld.rayTraceBlocks(start, end, false)
        return result != null && result.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK
    }
}