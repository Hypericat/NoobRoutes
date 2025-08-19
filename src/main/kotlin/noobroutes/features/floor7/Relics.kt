package noobroutes.features.floor7

import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.events.BossEventDispatcher
import noobroutes.events.impl.Phase
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.features.floor7.autop3.AutoP3MovementHandler
import noobroutes.features.settings.DevOnly
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.AuraManager
import noobroutes.utils.PacketUtils
import noobroutes.utils.Scheduler
import noobroutes.utils.SwapManager
import noobroutes.utils.Utils.getEntitiesOfType
import noobroutes.utils.Utils.isNotStart
import noobroutes.utils.distanceSquaredTo
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.skyblock.skyblockID
import noobroutes.utils.toVec3
import org.lwjgl.input.Keyboard
import kotlin.math.pow

object Relics : Module(
    name = "Relics",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "Relic related features"
) {
    private val doBlink by BooleanSetting("Blink Relics", description = "blink relics, this module can also be used to just aura relics")
    private val pickupRange by NumberSetting("Pickup Range", 4.5, 3.0, 5.0, description = "How far away from the relic the player can be away when picking it up")
    private val placeRange by NumberSetting("Place Range", 6.0, 4.0, 6.5, description = "How far away from the cauldron the player can be when placing the relic")

    private var pickedUpRelic = false
    private var placedRelic = false

    private val ORANGE_PACKETS = listOf(
        C03PacketPlayer.C04PacketPlayerPosition(89.89643235105844, 6.0, 55.50852574402165, true),
        C03PacketPlayer.C04PacketPlayerPosition(88.58170277071568, 6.0625, 55.01873483907161, true),
        C03PacketPlayer.C04PacketPlayerPosition(87.26697319037292, 6.0625, 54.52894393412158, true),
        C03PacketPlayer.C04PacketPlayerPosition(85.95224361003017, 6.0, 54.039153029171544, true),
        C03PacketPlayer.C04PacketPlayerPosition(84.63751402968741, 6.0, 53.54936212422151, true),
        C03PacketPlayer.C04PacketPlayerPosition(83.32278444934465, 6.0, 53.059571219271476, true),
        C03PacketPlayer.C04PacketPlayerPosition(82.0080548690019, 6.0, 52.56978031432144, true),
        C03PacketPlayer.C04PacketPlayerPosition(80.69332528865914, 6.0, 52.07998940937141, true),
        C03PacketPlayer.C04PacketPlayerPosition(79.37859570831638, 6.0, 51.59019850442137, true),
        C03PacketPlayer.C04PacketPlayerPosition(78.06386612797363, 6.0, 51.10040759947134, true),
        C03PacketPlayer.C04PacketPlayerPosition(76.74913654763087, 6.0, 50.610616694521305, true),
        C03PacketPlayer.C04PacketPlayerPosition(75.43440696728811, 6.0, 50.12082578957127, true),
        C03PacketPlayer.C04PacketPlayerPosition(74.11967738694536, 6.0, 49.631034884621236, true),
        C03PacketPlayer.C04PacketPlayerPosition(72.8049478066026, 6.0, 49.1412439796712, true),
        C03PacketPlayer.C04PacketPlayerPosition(71.49021822625984, 6.0, 48.65145307472117, true),
        C03PacketPlayer.C04PacketPlayerPosition(70.17548864591708, 6.0, 48.161662169771134, true),
        C03PacketPlayer.C04PacketPlayerPosition(68.86075906557433, 6.0, 47.6718712648211, true),
        C03PacketPlayer.C04PacketPlayerPosition(67.4909919957619, 6.0, 47.368313734100035, true),
        C03PacketPlayer.C04PacketPlayerPosition(66.12122492594948, 6.0, 47.06475620337897, true),
        C03PacketPlayer.C04PacketPlayerPosition(64.75145785613705, 6.0, 46.761198672657905, true),
        C03PacketPlayer.C04PacketPlayerPosition(63.381690786324626, 6.0, 46.45764114193684, true),
        C03PacketPlayer.C04PacketPlayerPosition(62.0119237165122, 6.0, 46.154083611215775, true),
        C03PacketPlayer.C04PacketPlayerPosition(60.642156646699775, 6.0, 45.85052608049471, true),
        C03PacketPlayer.C04PacketPlayerPosition(59.27238957688735, 6.0, 45.546968549773645, true),
        C03PacketPlayer.C04PacketPlayerPosition(57.90262250707492, 6.0, 45.24341101905258, true),
        C03PacketPlayer.C04PacketPlayerPosition(56.5328554372625, 6.0, 44.939853488331515, true),
        C03PacketPlayer.C04PacketPlayerPosition(55.16308836745007, 6.0, 44.63629595761045, true),
        C03PacketPlayer.C04PacketPlayerPosition(54.5, 6.0, 44.5, true)
    )

    private val RED_PACKETS = listOf(
        C03PacketPlayer.C04PacketPlayerPosition(23.00832295647662, 6.0, 57.51628500725534, true),
        C03PacketPlayer.C04PacketPlayerPosition(24.30220646180458, 6.0, 56.973814294091746, true),
        C03PacketPlayer.C04PacketPlayerPosition(25.596089967132542, 6.0, 56.43134358092815, true),
        C03PacketPlayer.C04PacketPlayerPosition(26.889973472460504, 6.0, 55.888872867764555, true),
        C03PacketPlayer.C04PacketPlayerPosition(28.183856977788466, 6.0, 55.34640215460096, true),
        C03PacketPlayer.C04PacketPlayerPosition(29.477740483116428, 6.0, 54.80393144143736, true),
        C03PacketPlayer.C04PacketPlayerPosition(30.77162398844439, 5.921599998474121, 54.26146072827377, false),
        C03PacketPlayer.C04PacketPlayerPosition(31.93919761670149, 5.766367993957519, 53.77194641250769, false),
        C03PacketPlayer.C04PacketPlayerPosition(33.02566753295541, 5.535840625044555, 53.3164354581798, false),
        C03PacketPlayer.C04PacketPlayerPosition(34.03833306915941, 5.231523797587011, 52.89186756365243, false),
        C03PacketPlayer.C04PacketPlayerPosition(34.98383661758238, 5.0, 52.49545785435507, true),
        C03PacketPlayer.C04PacketPlayerPosition(36.27772012291034, 5.0, 51.95298714119147, true),
        C03PacketPlayer.C04PacketPlayerPosition(37.5716036282383, 5.0, 51.410516428027876, true),
        C03PacketPlayer.C04PacketPlayerPosition(38.86548713356626, 5.0, 50.86804571486428, true),
        C03PacketPlayer.C04PacketPlayerPosition(40.159370638894224, 5.0, 50.325575001700685, true),
        C03PacketPlayer.C04PacketPlayerPosition(41.453254144222186, 5.0, 49.78310428853709, true),
        C03PacketPlayer.C04PacketPlayerPosition(44.22978780606567, 5.419999986886978, 48.63707859635127, false),
        C03PacketPlayer.C04PacketPlayerPosition(45.76980861169142, 5.7531999805212015, 48.00142867285062, false),
        C03PacketPlayer.C04PacketPlayerPosition(47.19526083537276, 6.001335979112147, 47.413067402907444, false),
        C03PacketPlayer.C04PacketPlayerPosition(48.51645564648001, 6.166109260938214, 46.86773880894176, false),
        C03PacketPlayer.C04PacketPlayerPosition(49.742776209410586, 6.249187078744681, 46.36156995124417, false),
        C03PacketPlayer.C04PacketPlayerPosition(50.882761204012205, 6.249187078744681, 45.89103645457756, false),
        C03PacketPlayer.C04PacketPlayerPosition(51.944180829170236, 6.170787077218802, 45.45293113738371, false),
        C03PacketPlayer.C04PacketPlayerPosition(52.93410596607414, 6.0155550727022, 45.044335464360536, false),
        C03PacketPlayer.C04PacketPlayerPosition(53.85897111679178, 6.0, 44.6625935683066, true),
        C03PacketPlayer.C04PacketPlayerPosition(54.5, 6.0, 44.5, true)
    )

    private val startPositions = listOf(ORANGE_PACKETS.first().toVec3(), RED_PACKETS.first().toVec3())

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        pickedUpRelic = false
        placedRelic = false
    }

    @SubscribeEvent
    fun relicAura(event: TickEvent.ClientTickEvent) {
        if (event.isNotStart || BossEventDispatcher.currentBossPhase != Phase.P5 || pickedUpRelic || placedRelic) return
        val amorstand = mc.theWorld.getEntitiesOfType<EntityArmorStand>().find {
            it.inventory?.get(4)?.displayName?.contains("Relic") == true && mc.thePlayer.getDistanceSqToEntity(it) < pickupRange.pow(2)
        } ?: return

        AuraManager.auraEntity(amorstand, C02PacketUseEntity.Action.INTERACT_AT)
        pickedUpRelic = true

        Scheduler.schedulePreTickTask(1) { doBlinkLogic() }
    }

    private fun doBlinkLogic() {
        if (!doBlink) return
        if (mc.thePlayer.capabilities.walkSpeed < 0.5) return modMessage("need 500 speed for blink")

        if (mc.thePlayer.positionVector.squareDistanceTo(ORANGE_PACKETS.first().toVec3()) < 1.96) doRelicBlink(ORANGE_PACKETS)
        else if (mc.thePlayer.positionVector.squareDistanceTo(RED_PACKETS.first().toVec3()) < 1.96) doRelicBlink(RED_PACKETS)
    }

    private fun doRelicBlink(packets: List<C03PacketPlayer.C04PacketPlayerPosition>) {
        if (packets.size > AutoP3.cancelled) return modMessage("not enough packets, stand still next time")

        if (mc.thePlayer.posY != 6.0 || !mc.thePlayer.onGround) return

        packets.forEach { PacketUtils.sendPacket(it) }
        AutoP3.cancelled -= packets.size
        val lastPacket = packets.last()

        mc.thePlayer.setPosition(lastPacket.positionX, lastPacket.positionY, lastPacket.positionZ)
        AutoP3MovementHandler.resetShit()

        SwapManager.swapToSlot(8)
    }

    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        if (BossEventDispatcher.currentBossPhase != Phase.P5 || pickedUpRelic || placedRelic) return

        for (pos in startPositions) {
            val color = if (mc.thePlayer.distanceSquaredTo(pos) < 1.96 && mc.thePlayer.onGround && mc.thePlayer.posY == 6.0) Color.GREEN else Color.RED
            Renderer.drawCylinder(pos, 1.4, 1.4, -0.03, 100, 1, 90, 0, 0, color, depth = true, )
        }
    }

    private fun C03PacketPlayer.C04PacketPlayerPosition.toVec3(): Vec3 {
        return Vec3(this.positionX, this.positionY, this.positionZ)
    }

    @SubscribeEvent
    fun placeRelic(event: TickEvent.ClientTickEvent) {
        if (event.isNotStart || BossEventDispatcher.currentBossPhase != Phase.P5 || placedRelic) return

        val heldRelic = Relic.entries.find { it.skyblockID == mc.thePlayer.inventory.mainInventory[8]?.skyblockID } ?: return
        val pos = heldRelic.blockPos
        if (pos.toVec3(0.5, 0.5, 0.5).squareDistanceTo(mc.thePlayer.getPositionEyes(0f)) > placeRange.pow(2)) return

        val state = SwapManager.swapToSlot(8)

        if (state == SwapManager.SwapState.ALREADY_HELD) {
            AuraManager.auraBlock(pos, true)
            return
        }

        Scheduler.scheduleFrameTask {
            AuraManager.auraBlock(pos, true)
        }
    }

    private enum class Relic(val skyblockID: String, val blockPos: BlockPos) { //taken from CGA
        Red("RED_KING_RELIC", BlockPos(51.0, 7.0, 42.0)),
        Green("GREEN_KING_RELIC", BlockPos(49.0, 7.0, 44.0)),
        Purple("PURPLE_KING_RELIC", BlockPos(54.0, 7.0, 41.0)),
        Blue("BLUE_KING_RELIC", BlockPos(59.0, 7.0, 44.0)),
        Orange("ORANGE_KING_RELIC", BlockPos(57.0, 7.0, 42.0))
    }
}