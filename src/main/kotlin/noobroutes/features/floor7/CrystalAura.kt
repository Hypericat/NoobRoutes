package noobroutes.features.floor7

import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.item.EntityEnderCrystal
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.AuraManager
import noobroutes.utils.Utils.getEntitiesOfType
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.skyblock.dungeonScanning.DungeonUtils
import noobroutes.utils.skyblock.dungeonScanning.M7Phases
import org.lwjgl.input.Keyboard
import kotlin.math.pow

//stolen from meow

object CrystalAura: Module(
    name = "Crystal Aura",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "picks up and places crystals"
) {
    private val range by NumberSetting(name = "range", description = "how much reach the aura should have", min = 3f, max = 6.5f, default = 5f, increment = 0.1f)
    private val cooldown by NumberSetting(name = "cooldown", description = "how long to wait beetween presses", min = 0, max = 5, default = 1, unit = "t")

    private var cd = 0

    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.isEnd || mc.thePlayer == null || DungeonUtils.getF7Phase() != M7Phases.P1) return
        if (cd > 0) {
            cd--
            return
        }
        if (mc.thePlayer.inventory.getStackInSlot(8)?.displayName == "Energy Crystal") {
            val armorstands = mc.theWorld.getEntitiesOfType<EntityArmorStand>()
            val theOne = armorstands.find {
                (it.positionVector == Vec3(52.50, 223.50, 41.50)
                        || it.positionVector == Vec3(94.50, 223.50, 41.50))
                        && it.displayName.unformattedText != "Energy Crystal Missing"
                        && it.getDistanceSq(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.eyeHeight, mc.thePlayer.posZ) < range.pow(2)
            } ?: return
            AuraManager.auraEntity(theOne, C02PacketUseEntity.Action.INTERACT_AT)
            cd == cooldown
        }
        else {
            val crystals = mc.theWorld.getEntitiesOfType<EntityEnderCrystal>()
            val theOne = crystals.find { (it.positionVector == Vec3(64.50, 238.375, 50.50)
                    || it.positionVector == Vec3(82.50, 238.375, 50.50))
                    && it.getDistanceSq(mc.thePlayer.posX, mc.thePlayer.posY + mc.thePlayer.eyeHeight, mc.thePlayer.posZ) < range.pow(2)
            } ?: return
            AuraManager.auraEntity(theOne, C02PacketUseEntity.Action.INTERACT_AT)
            cd == cooldown
        }
    }
}