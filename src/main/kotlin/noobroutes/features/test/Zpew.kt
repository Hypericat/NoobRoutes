package noobroutes.features.test

import noobroutes.events.impl.PacketEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.render.ClickGUIModule
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.features.settings.impl.SelectorSetting
import noobroutes.features.settings.impl.StringSetting
import noobroutes.utils.Scheduler
import noobroutes.utils.skyblock.*
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.utils.skyblock.PlayerUtils.getBlockPlayerIsLookingAt
import kotlin.math.abs

object Zpew : Module(
    name = "Zpew",
    category = Category.MISC,
    description = "Temporary Zpew thing"
) {

    private val sendPacket by BooleanSetting("Send Packet", description = "You send a C06 Packet, aka, it is actual zpew")
    private val sendTPCommand by BooleanSetting("Send Tp Command", description = "Used for Single Player")
    private val zpewOffset by BooleanSetting("Offset", description = "Offsets your position onto the block instead of 0.05 blocks above it")
    private val dingdingding by BooleanSetting("dingdingding", false, description = "")

    private val soundOptions = arrayListOf(
        "note.pling",
        "mob.blaze.hit",
        "fire.ignite",
        "random.orb",
        "random.break",
        "mob.guardian.land.hit",
        "Custom"
    )
    private val soundSelector by SelectorSetting("Sound", soundOptions[0], soundOptions, description =  "Sound Selection").withDependency { dingdingding }
    private val customSound by StringSetting("Custom Sound", soundOptions[0], description = "Name of a custom sound to play. This is used when Custom is selected in the Sound setting.").withDependency { dingdingding && soundSelector == 6 }
    private val pitch by NumberSetting("Pitch", 1.0, 0.1, 2.0, 0.1, description = "").withDependency { dingdingding }



    private const val FAILWATCHPERIOD: Int = 20
    private const val MAXFAILSPERFAILPERIOD: Int = 3
    private const val MAXQUEUEDPACKETS: Int = 5

    private var updatePosition = true
    private val recentlySentC06s = mutableListOf<SentC06>()
    private val recentFails = mutableListOf<Long>()
    private val blackListedBlocks = arrayListOf(Blocks.chest, Blocks.trapped_chest, Blocks.enchanting_table, Blocks.hopper, Blocks.furnace, Blocks.crafting_table)

    private var lastPitch: Float = 0f
    private var lastYaw: Float = 0f
    private var lastX: Double = 0.0
    private var lastY: Double = 0.0
    private var lastZ: Double = 0.0
    private var isSneaking: Boolean = false

    private fun checkAllowedFails(): Boolean {
        if(LocationUtils.currentArea.isArea(Island.SinglePlayer)) return true;

        if (recentlySentC06s.size >= MAXQUEUEDPACKETS) return false

        while (recentFails.isNotEmpty() && System.currentTimeMillis() - recentFails[0] > FAILWATCHPERIOD * 1000) recentFails.removeFirst()

        return recentFails.size < MAXFAILSPERFAILPERIOD
    }

    private fun doZeroPingEtherWarp() {
        val etherBlock = EtherWarpHelper.getEtherPos(
            Vec3(lastX, lastY, lastZ),
            lastYaw,
            lastPitch,
            57.0
        )

        if (!etherBlock.succeeded) return

        val pos = etherBlock.pos!!

        val x: Double = pos.x.toDouble() + 0.5
        var y: Double = pos.y.toDouble() + 1.05
        val z: Double = pos.z.toDouble() + 0.5

        var yaw = lastYaw
        val pitch = lastPitch

        yaw %= 360
        if (yaw < 0) yaw += 360
        if (yaw > 360) yaw -= 360

        lastX = x
        lastY = y
        lastZ = z
        updatePosition = false

        recentlySentC06s.add(SentC06(yaw, pitch, x, y, z, System.currentTimeMillis()))

        if (dingdingding) PlayerUtils.playLoudSound(getSound(), 100f, Zpew.pitch.toFloat())
        if (sendTPCommand) Scheduler.schedulePreTickTask(0) { sendChatMessage("/tp $x $y $z")}

        if (sendPacket) Scheduler.scheduleHighPreTickTask {
            mc.netHandler.addToSendQueue(
                C03PacketPlayer.C06PacketPlayerPosLook(
                    x,
                    y,
                    z,
                    yaw,
                    pitch,
                    mc.thePlayer.onGround
                )
            )
            if (zpewOffset) y -= 0.05
            mc.thePlayer.setPosition(x, y, z)
            mc.thePlayer.setVelocity(0.0, 0.0, 0.0)
            updatePosition = true
        }
    }

    fun isWithinTolerance(n1: Float, n2: Float, tolerance: Double = 1e-4): Boolean {
        return kotlin.math.abs(n1 - n2) < tolerance
    }




    @SubscribeEvent
    fun onC08(event: PacketEvent.Send) {
        if (mc.thePlayer == null || event.packet !is C08PacketPlayerBlockPlacement) return

        val dir = event.packet.placedBlockDirection
        if (dir != 255) return

        if (!LocationUtils.isInSkyblock && !ClickGUIModule.forceHypixel) return
        if (!isSneaking || mc.thePlayer.heldItem.skyblockID != "ASPECT_OF_THE_VOID" || getBlockPlayerIsLookingAt() in blackListedBlocks) return


        if(!checkAllowedFails()) {
            modMessage("§cZero ping etherwarp teleport aborted.")
            modMessage("§c${recentFails.size} fails last ${FAILWATCHPERIOD}s")
            modMessage("§c${recentlySentC06s.size} C06's queued currently")
            return
        }

        doZeroPingEtherWarp()
    }

    @SubscribeEvent
    fun onC03(event: PacketEvent.Send) {
        if (event.packet !is C03PacketPlayer) return
        if (!updatePosition) return
        val x = event.packet.positionX
        val y = event.packet.positionY
        val z = event.packet.positionZ
        val yaw = event.packet.yaw
        val pitch = event.packet.pitch

        if (event.packet.isMoving) {
            lastX = x
            lastY = y
            lastZ = z
        }

        if (event.packet.rotating) {
            lastYaw = yaw
            lastPitch = pitch
        }
    }

    @SubscribeEvent
    fun onC0B(event: PacketEvent.Send) {
        if (event.packet !is C0BPacketEntityAction) return
        if (event.packet.action == C0BPacketEntityAction.Action.START_SNEAKING) isSneaking = true
        if (event.packet.action == C0BPacketEntityAction.Action.STOP_SNEAKING) isSneaking = false
    }

    @SubscribeEvent
    fun onS08(event: PacketEvent.Receive) {
        if (event.packet !is S08PacketPlayerPosLook) return
        if (DungeonUtils.inBoss || (!LocationUtils.isInSkyblock && !ClickGUIModule.forceHypixel)) return
        if (recentlySentC06s.isEmpty()) return

        val sentC06 = recentlySentC06s[0]
        recentlySentC06s.removeFirst()

        val newYaw = event.packet.yaw
        val newPitch = event.packet.pitch
        val newX = event.packet.x
        val newY = event.packet.y
        val newZ = event.packet.z

        val isCorrect = (
                (isWithinTolerance(sentC06.yaw, newYaw) || newYaw == 0f) &&
                        (isWithinTolerance(sentC06.pitch, newPitch) || newPitch == 0f) &&
                        newX == sentC06.x &&
                        newY == sentC06.y &&
                        newZ == sentC06.z
                )

        if (isCorrect) {
            if (sendPacket) event.isCanceled = true
            return
        }

        devMessage("receivedS08($newX, $newY, $newZ)")
        devMessage("sentC06(${sentC06.x}, ${sentC06.y}, ${sentC06.z})")
        devMessage(recentlySentC06s)
        devMessage("Failed")

        recentFails.add(System.currentTimeMillis())
        while (recentlySentC06s.isNotEmpty()) recentlySentC06s.removeFirst()
    }

    @SubscribeEvent
    fun onS29(event: PacketEvent.Receive) {
        if (event.packet !is S29PacketSoundEffect) return
        val packet: S29PacketSoundEffect = event.packet
        if (packet.soundName != "mob.enderdragon.hit" || packet.volume != 1f || packet.pitch != 0.53968257f || !checkAllowedFails()) return
        event.isCanceled = true
    }

    /**
     * Returns the sound from the selector setting, or the custom sound when the last element is selected
     */
    private fun getSound(): String {
        return if (soundSelector < soundSelector - 1)
            soundOptions[soundSelector]
        else
            customSound
    }

    data class SentC06(
        val yaw: Float,
        val pitch: Float,
        val x: Double,
        val y: Double,
        val z: Double,
        val sentAt: Long
    )
}