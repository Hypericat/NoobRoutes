package noobroutes.features.misc

import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.events.impl.BossEvent
import noobroutes.events.impl.RoomEnterEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.utils.SecretGuideIntegration
import noobroutes.utils.skyblock.dungeon.DungeonUtils

object SecretGuideManager : Module(
    "Secret Guide Manager",
    description = "Toggles Secret Guide in certain situations",
    category = Category.MISC
) {
    private var secretGuideBoolean: Boolean? = null
    private var lastRoom: String? = null
    private val onBoss by BooleanSetting("In Boss", description = "Toggles Secret Guide while in Floor 7 boss Fight")
    private val inWaterBoard by BooleanSetting("In WaterBoard", description = "Toggles Secret Guide while in Water Board")

    @SubscribeEvent
    fun onBossStart(event: BossEvent.BossStart) {
        if (!onBoss) return
        if (event.floor.floorNumber == 7) {
            secretGuideBoolean = SecretGuideIntegration.getSecretGuideAura()
            SecretGuideIntegration.setSecretGuideAura(false)
        }
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        secretGuideBoolean?.let { SecretGuideIntegration.setSecretGuideAura(it) }
    }

    @SubscribeEvent
    fun onBossEnd(event: BossEvent.BossFinish) {
        if (!onBoss) return
        secretGuideBoolean?.let { SecretGuideIntegration.setSecretGuideAura(it) }
        secretGuideBoolean = null
    }

    @SubscribeEvent
    fun onRoom(event: RoomEnterEvent) {
        if (lastRoom == "Water Board" && inWaterBoard && DungeonUtils.currentRoomName != "Water Board") {
            secretGuideBoolean?.let { SecretGuideIntegration.setSecretGuideAura(it) }
            secretGuideBoolean = null
        }
        lastRoom = event.room?.name
        if (event.room?.name != "Water Board" || !inWaterBoard) return
        secretGuideBoolean = SecretGuideIntegration.getSecretGuideAura()
        SecretGuideIntegration.setSecretGuideAura(false)
    }
}