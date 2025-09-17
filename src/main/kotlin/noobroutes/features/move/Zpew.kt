package noobroutes.features.move

import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraft.nbt.NBTTagList
import net.minecraft.nbt.NBTTagString
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C00PacketKeepAlive
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.client.C0FPacketConfirmTransaction
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.events.BossEventDispatcher.inBoss
import noobroutes.events.impl.ChatPacketEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.WorldChangeEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.render.ClickGUIModule
import noobroutes.features.settings.DevOnly
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.*
import noobroutes.utils.*
import noobroutes.utils.Utils.ID
import noobroutes.utils.skyblock.*
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import java.lang.Thread.sleep
import kotlin.math.floor

@DevOnly
object Zpew : Module(
    name = "ZZZpew (Dangerous)",
    category = Category.MOVE,
    description = "Blink Based \"Zpew\". Does not work with autoroutes. (At least no Proxy needed)"
) {
    private val zpew by BooleanSetting("Zero Ping Etherwarp", description = "Zero ping teleport for right clicking aotv")
    private val zpt by BooleanSetting("Zero Ping Aotv", description = "Zero ping teleport for right clicking aotv")
    private val zph by BooleanSetting("Zero Ping Hyperion", description = "Zero Ping Hyperion wow")

    private val sendTPCommand by BooleanSetting("Send Tp Command", description = "Used for Single Player")
    private val hideFuckingTeleports by BooleanSetting("Hide Tp Messages", description = "Hides the fucking annoying tp messages").withDependency { Minecraft.getMinecraft().isSingleplayer && sendTPCommand }
    //private val zpewOffset by BooleanSetting("Offset", description = "Offsets your position onto the block instead of 0.05 blocks above it")
    private val blinkLimit by NumberSetting("Max Blink Length", 25, 5, 30, 1, description = "")
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
    private val soundSelector = SelectorSetting("Sound", soundOptions[0], soundOptions, description =  "Sound Selection").withDependency { dingdingding }
    private val customSound by StringSetting("Custom Sound", soundOptions[0], description = "Name of a custom sound to play. This is used when Custom is selected in the Sound setting.").withDependency { dingdingding && soundSelector.index == 6 }
    private val pitch by NumberSetting("Pitch", 1.0, 0.1, 2.0, 0.1, description = "").withDependency { dingdingding }
    private val volume by NumberSetting("Volume", 100, 1, 200, 1, description = "").withDependency { dingdingding }
    private val soundButton by ActionSetting("Play Sound", description = "", default = {
        PlayerUtils.playLoudSound(getSound(), volume.toFloat(), pitch.toFloat())
    }).withDependency { dingdingding }

    private val blackListedBlocks = arrayListOf(Blocks.chest, Blocks.trapped_chest, Blocks.enchanting_table, Blocks.hopper, Blocks.furnace, Blocks.crafting_table)

    private var lastPitch: Float = 0f
    private var lastYaw: Float = 0f
    private var lastX: Double = 0.0
    private var lastY: Double = 0.0
    private var lastZ: Double = 0.0
    private var isSneaking: Boolean = false

    private var waitingList = mutableListOf<S08Blink>()

    private var skipPacketCount = 0

    fun holdingTeleportItem(): Boolean {
        val held = mc.thePlayer.heldItem
        if (held.skyblockID.equalsOneOf("ASPECT_OF_THE_VOID", "ASPECT_OF_THE_END")) return true
        if (LocationUtils.isSinglePlayer && (held.ID == 277 || held.ID == 267)) {
            return true
        }
        val scrolls = held.extraAttributes?.getTag("ability_scroll") ?: return false
        if (scrolls !is NBTTagList) return false

        val scrollsString = mutableListOf<String>()
        for (i in 0 until scrolls.tagCount()) {
            if (scrolls.get(i) is NBTTagString) scrollsString.add((scrolls.get(i) as NBTTagString).string)
        }
        return scrollsString.containsAll(listOf("IMPLOSION_SCROLL", "WITHER_SHIELD_SCROLL", "SHADOW_WARP_SCROLL"))
    }

    private fun doZeroPingAotv(pos: BlockPos){
        if (!holdingTeleportItem() || !enabled) return
        if (isSneaking && mc.thePlayer.heldItem.skyblockID.equalsOneOf("ASPECT_OF_THE_VOID", "ASPECT_OF_THE_END")) return


        var yaw = lastYaw
        val pitch = lastPitch

        yaw %= 360
        if (yaw < 0) yaw += 360
        if (yaw > 360) yaw -= 360

        val x = pos.x + 0.5
        val y = pos.y.toDouble()
        val z = pos.z + 0.5

        doTp(x, y, z, yaw, pitch)
    }

    private fun doZeroPingEtherWarp(distance: Float = 57f) {
        val etherBlock = EtherWarpHelper.getEtherPosOrigin(
            Vec3(lastX, lastY, lastZ),
            lastYaw,
            lastPitch,
            distance.toDouble(),
            sneaking = true
        )

        if (!etherBlock.succeeded) return

        val pos = etherBlock.pos!!

        val x: Double = pos.x.toDouble() + 0.5
        val y: Double = pos.y.toDouble() + 1.05
        val z: Double = pos.z.toDouble() + 0.5

        var yaw = lastYaw
        val pitch = lastPitch

        yaw %= 360
        if (yaw < 0) yaw += 360
        if (yaw > 360) yaw -= 360

        doTp(x, y, z, yaw, pitch)
    }

    private fun scheduleTp(x: Double, y: Double, z: Double) {
        Thread {
            sleep(500 + (Math.random() * 50).toLong())
            sendChatMessage("/tp $x $y $z")
        }.start()
    }

    private fun doTp(x: Double, y: Double, z: Double, yaw: Float, pitch: Float) {
        if (dingdingding) PlayerUtils.playLoudSound(getSound(), volume.toFloat(), Zpew.pitch.toFloat())
        if (sendTPCommand && LocationUtils.isSinglePlayer) {
            scheduleTp(x, y, z)

        }
        Scheduler.scheduleHighPostTickTask {
            val speedVec = Vec3(mc.thePlayer.motionX, mc.thePlayer.motionY, mc.thePlayer.motionZ)
            waitingList.add(S08Blink(C03PacketPlayer.C06PacketPlayerPosLook(x, y, z, yaw, pitch, false), mutableListOf(), mc.thePlayer.positionVector, speedVec))

            mc.thePlayer.setPosition(x, y, z)
            mc.thePlayer.setVelocity(0.0, 0.0, 0.0)
        }
    }

    @SubscribeEvent
    fun onChatMessage(event: ChatPacketEvent) {
        if (!hideFuckingTeleports || !Minecraft.getMinecraft().isSingleplayer || !sendTPCommand) return
        if (event.message.startsWith("Teleported " + Minecraft.getMinecraft().thePlayer.name)) event.isCanceled = true
    }

    @SubscribeEvent(priority = EventPriority.HIGH)
    fun onC08(event: PacketEvent.Send) {
        if (skipPacketCount > 0) return
        if (mc.thePlayer == null || event.packet !is C08PacketPlayerBlockPlacement) return
        val dir = event.packet.placedBlockDirection
        if (dir != 255) return
        val info = getTeleportInfo() ?: return

        if (!LocationUtils.isInSkyblock && !ClickGUIModule.forceHypixel) return

        if (info.ether) {
            doZeroPingEtherWarp(info.distance)
            return
        }

        val prediction = predictTeleport(info.distance) ?: return
        doZeroPingAotv(prediction.toBlockPos())
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun onPacket(event: PacketEvent.Send) {
        if (event.packet.javaClass.name.contains("server")) return //i cba to fix wadeys packet event rn
        if (skipPacketCount > 0) {
            skipPacketCount--
            return
        }

        if (waitingList.isEmpty()) return
        if (event.packet is C0FPacketConfirmTransaction || event.packet is C00PacketKeepAlive || event.packet is C01PacketChatMessage) return

        event.isCanceled = true
        waitingList.last().packets.add(event.packet)

        if (waitingList.last().packets.filter { it is C03PacketPlayer }.size > blinkLimit) {
            val listToSend = mutableListOf<Packet<*>>()

            for (s08Response in waitingList) {
                s08Response.packets.filter { it !is C03PacketPlayer && it !is C08PacketPlayerBlockPlacement }.forEach { listToSend.add(it) }
            }

            val lastKnownPos = waitingList.first().startPos
            val lastKnownSpeed = waitingList.first().startSpeed

            waitingList = mutableListOf()

            listToSend.forEach { PacketUtils.sendPacket(it) }

            mc.thePlayer.setPosition(lastKnownPos.xCoord, lastKnownPos.yCoord, lastKnownPos.zCoord)
            mc.thePlayer.setVelocity(lastKnownSpeed.xCoord, lastKnownSpeed.yCoord, lastKnownSpeed.zCoord)

            modMessage("fuck this shit im out")
        }
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    fun onC03(event: PacketEvent.Send) {
        if (skipPacketCount > 0 || event.packet !is C03PacketPlayer) return

        if (event.packet.isMoving) {
            lastX = event.packet.positionX
            lastY = event.packet.positionY
            lastZ = event.packet.positionZ
        }

        if (event.packet.rotating) {
            lastYaw = event.packet.yaw
            lastPitch = event.packet.pitch
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
        if (event.packet !is S08PacketPlayerPosLook || waitingList.isEmpty()) return

        val firstS08 = waitingList.removeFirst()

        if (event.packet.x != firstS08.c06.positionX || event.packet.y != firstS08.c06.positionY || event.packet.z != firstS08.c06.positionZ) {
            devMessage("Position mismatch - X: ${event.packet.x} vs ${firstS08.c06.positionX}, Y: ${event.packet.y} vs ${firstS08.c06.positionY}, Z: ${event.packet.z} vs ${firstS08.c06.positionZ}")
            modMessage("failed zpew")
            for (s08Response in waitingList) {
                s08Response.packets.filter { it !is C03PacketPlayer && it !is C08PacketPlayerBlockPlacement }.forEach { PacketUtils.sendPacket(it) }
            }
            waitingList = mutableListOf()
            return
        }

        event.isCanceled = true

        Scheduler.schedulePreTickTask {
            skipPacketCount = firstS08.packets.size + 1
            PacketUtils.sendPacket(firstS08.c06)
            for (packet in firstS08.packets) { PacketUtils.sendPacket(packet) }
        }
    }

    @SubscribeEvent
    fun onWorldChange(event: WorldChangeEvent) {
        waitingList = mutableListOf()
    }

    @SubscribeEvent
    fun onS29(event: PacketEvent.Receive) {
        if (event.packet !is S29PacketSoundEffect) return
        val packet: S29PacketSoundEffect = event.packet
        if (packet.soundName != "mob.enderdragon.hit" || packet.volume != 1f || packet.pitch != 0.53968257f) return
        event.isCanceled = true
    }

    /**
     * Returns the sound from the selector setting, or the custom sound when the last element is selected
     */
    private fun getSound(): String {
        return if (soundSelector.index < soundOptions.size - 1)
            soundOptions[soundSelector.index]
        else
            customSound
    }


    data class S08Blink(
        val c06: C03PacketPlayer.C06PacketPlayerPosLook,
        val packets: MutableList<Packet<*>>,
        val startPos: Vec3,
        val startSpeed: Vec3
    )

    const val STEPS = 100

    private fun predictTeleport(distance: Float): Vec3? {
        var cur = Vec3(lastX, lastY + mc.thePlayer.eyeHeight, lastZ)
        val forward = RotationUtils.yawAndPitchVector(lastYaw, lastPitch).multiply(1f / STEPS)
        var stepsTaken = 0
        for (i in 0 until (distance * STEPS).toInt() + 1) {
            if (i % STEPS == 0 && !cur.isSpecial && !cur.blockAbove.isSpecial) {
                if (!cur.isIgnored || !cur.blockAbove.isIgnored) {
                    cur = cur.add(forward.multiply(-STEPS))
                    if (i == 0 || !cur.isIgnored || !cur.blockAbove.isIgnored) {
                        return null
                    }
                    return Vec3(floor(cur.xCoord) + 0.5, floor(cur.yCoord), floor(cur.zCoord) + 0.5)
                }
            }
            if ((!cur.isIgnored2 && cur.inBB) || (!cur.blockAbove.isIgnored2 && cur.blockAbove.inBB)) {
                cur = cur.add(forward.multiply(-STEPS))
                if (i == 0 || (!cur.isIgnored && cur.inBB) || (!cur.blockAbove.isIgnored && cur.blockAbove.inBB)) {
                    return null
                }
                stepsTaken = i
                break
            }
            cur = cur.add(forward)
            stepsTaken = i
        }
        val multiplicationFactor = floor(stepsTaken.toFloat() / STEPS)
        val pos = Vec3(lastX, lastY + mc.thePlayer.eyeHeight, lastZ).add(RotationUtils.yawAndPitchVector(lastYaw, lastPitch).multiply(
            multiplicationFactor
        ))
        if ((!cur.isIgnored && cur.inBB) || (!cur.blockAbove.isIgnored && cur.blockAbove.inBB)) return null
        return Vec3(floor(pos.xCoord) + 0.5, floor(pos.yCoord), floor(pos.zCoord) + 0.5)
    }


    private val Vec3.inBB: Boolean get() {
        val bb = getBlockStateAt(this.toBlockPos()).block.getSelectedBoundingBox(mc.theWorld, this.toBlockPos())
        return bb?.isVecInside(this) ?: false
    }


    private inline val Vec3.isSpecial: Boolean get() = special.contains(getBlockIdAt(this.toBlockPos()))
    private inline val Vec3.isIgnored: Boolean get() = ignored.contains(getBlockIdAt(this.toBlockPos()))
    private inline val Vec3.isIgnored2: Boolean get() = ignored2.contains(getBlockIdAt(this.toBlockPos())) || this.isIgnored


    private inline val Vec3.blockAbove get() = Vec3(this.xCoord, this.yCoord + 1, this.zCoord)
    private val special = listOf(65, 106, 111)
    private val ignored = listOf(0, 51, 8, 9, 10, 11, 171, 331, 39, 40, 115, 132, 77, 143, 66, 27, 28, 157)
    private val ignored2 = listOf(44, 182, 126)

    class TeleportInfo(val distance: Float, val ether: Boolean)
    private fun getTeleportInfo(): TeleportInfo? {
        if (inBoss && DungeonUtils.floorNumber == 7) return null
        val held = mc.thePlayer.heldItem
        if (LocationUtils.isSinglePlayer) {
            if (held.ID == 277) {
                return if (isSneaking) {
                    if (zpew) TeleportInfo(61f, true) else null
                } else {
                    if (zpt) TeleportInfo(12f, false) else null
                }
            }
            if (held.ID == 267 && zph) {
                return TeleportInfo(10f, false)
            }
        }
        if (held.skyblockID.equalsOneOf("ASPECT_OF_THE_VOID", "ASPECT_OF_THE_END")) {
            val tuners = held.tuners ?: 0
            return if (isSneaking) {
                if (zpew) TeleportInfo(56f + tuners, true) else null
            } else {
                if (zpt) TeleportInfo(8f + tuners, false) else null
            }
        }
        if (zph && held.skyblockID.equalsOneOf("NECRON_BLADE", "HYPERION", "VALKYRIE", "ASTRAEA", "SCYLLA")) {
            return TeleportInfo(10f, false)
        }
        return null
    }
}