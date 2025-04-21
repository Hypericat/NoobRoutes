package com.github.wadey3636.noobroutes.features.puzzle

import com.github.wadey3636.noobroutes.utils.BowUtils
import com.github.wadey3636.noobroutes.utils.PacketUtils
import com.github.wadey3636.noobroutes.utils.RotationUtils
import com.github.wadey3636.noobroutes.utils.SecretGuideIntegration
import com.github.wadey3636.noobroutes.utils.SwapManager
import me.noobmodcore.events.impl.RoomEnterEvent
import me.noobmodcore.events.impl.S08Event
import me.noobmodcore.features.Category
import me.noobmodcore.features.Module
import me.noobmodcore.features.settings.Setting.Companion.withDependency
import me.noobmodcore.features.settings.impl.BooleanSetting
import me.noobmodcore.features.settings.impl.ColorSetting
import me.noobmodcore.features.settings.impl.NumberSetting
import me.noobmodcore.utils.add
import me.noobmodcore.utils.equalsOneOf
import me.noobmodcore.utils.noControlCodes
import me.noobmodcore.utils.render.Color
import me.noobmodcore.utils.render.Renderer
import me.noobmodcore.utils.skyblock.PlayerUtils
import me.noobmodcore.utils.skyblock.dungeon.DungeonUtils
import me.noobmodcore.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import me.noobmodcore.utils.skyblock.dungeon.tiles.Room
import me.noobmodcore.utils.skyblock.skyblockID
import me.noobmodcore.utils.toBlockPos
import me.noobmodcore.utils.toVec3
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent


object Blaze : Module(
    "Blaze",
    description = "Automatically completes the higher or lower puzzle",
    category = Category.PUZZLE
) {

    private var lastShotTime = 0L
    private var lastShotTarget: EntityArmorStand? = null

    private var blazes = mutableListOf<EntityArmorStand>()
    private val blazeHealthRegex = Regex("^\\[Lv15] Blaze [\\d,]+/([\\d,]+)❤$")
    private var currentEtherwarpTarget: BlockPos? = null
    private var currentBlazeTarget: EntityArmorStand? = null
    private val silent by BooleanSetting("Silent", description = "Server Side Rotations. Only works with zpew")
    private var awaitingRoomChange = false
    private var warping = false

    private val shotCooldown by NumberSetting(
        "Shot Cooldown",
        1000,
        200,
        1000,
        1,
        description = "Set this based on your attack speed and bow. A bow with a cooldown of 0.5 has a shot cooldown of 450 ms from 0 to 11 attack speed, 350 ms cooldown from 12 to 40 attack speed, 250 ms cooldown from 41 to 66 attack speed, and 200 ms cooldown from 67 to 100 attack speed."
    )
    private val blazeHighlight by BooleanSetting(
        "Highlight Target Blaze",
        description = "Highlights the blaze the module is targeting"
    )
    private val blazeHighlightColor by ColorSetting(
        "Blaze Color",
        Color.GREEN,
        description = "The color of the target blaze"
    ).withDependency { blazeHighlight }
    private val auraSecret by BooleanSetting(
        "Aura Secret",
        description = "Secret Aura for the secret in blaze."
    )
    private val toggleSG by BooleanSetting(
        "Toggle Secret Guide",
        description = "Toggles secret aura in blaze."
    )
    private val lowerBlazeEWLocations = listOf(
        Vec3(22.0, 56.0, 16.0),
        Vec3(15.0, 42.0, 22.0),
        Vec3(11.0, 31.0, 10.0),
        Vec3(11.0, 51.0, 23.0),
        Vec3(14.0, 40.0, 6.0),
        Vec3(8.0, 37.0, 11.0)
    )

    /**
     * Taken from Odin
     */
    private fun getBlaze() {
        val room = DungeonUtils.currentRoom ?: return
        if (!DungeonUtils.inDungeons || !room.data.name.equalsOneOf("Lower Blaze", "Higher Blaze")) return
        val hpMap = mutableMapOf<EntityArmorStand, Int>()
        blazes.clear()
        mc.theWorld?.loadedEntityList?.forEach { entity ->
            if (entity !is EntityArmorStand || entity in blazes) return@forEach

            val hp = blazeHealthRegex.find(entity.name.noControlCodes)?.groups?.get(1)?.value?.replace(",", "")
                ?.toIntOrNull() ?: return@forEach
            hpMap[entity] = hp
            blazes.add(entity)
        }
        if (room.data.name == "Lower Blaze") blazes.sortByDescending { hpMap[it] }
        else blazes.sortBy { hpMap[it] }
    }


    @SubscribeEvent
    fun onEnterRoom(event: RoomEnterEvent) {
        if (awaitingRoomChange) {SecretGuideIntegration.setSecretGuideAura(true); awaitingRoomChange = false}
        if (event.room?.data == null || !event.room.data.name.equalsOneOf("Lower Blaze", "Higher Blaze")) return
        getBlaze()
        when (event.room.data.name) {
            "Lower Blaze" -> {

            }
        }

        if (toggleSG) {
            val state = SecretGuideIntegration.getSecretGuideAura() ?: return
            if (state) {
                SecretGuideIntegration.setSecretGuideAura(false)
                awaitingRoomChange = true
            }
        }
    }

    private fun isOnBlock(vec3: Vec3): Boolean {
        return vec3.add(0.5, 1.0, 0.5).distanceTo(mc.thePlayer.positionVector) < 0.1
    }
    private fun isOnBlock(pos: BlockPos): Boolean {
        return isOnBlock(pos.toVec3())
    }

    private fun etherwarpToVec3(vec3: Vec3){
        PlayerUtils.unPressKeys()
        PlayerUtils.sneak()
        mc.thePlayer.setVelocity(0.0, mc.thePlayer.motionY, 0.0)
        val state = SwapManager.swapFromSBId("ASPECT_OF_THE_END", "ASPECT_OF_THE_VOID")
        val rot = RotationUtils.getYawAndPitch(vec3, true)
       rotateAndClick(state, rot)
    }




    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        val room = DungeonUtils.currentRoom ?: return
        if (!room.data.name.equalsOneOf("Lower Blaze", "Higher Blaze")) return


        getBlaze()
        if (blazes.isEmpty()) return
        currentBlazeTarget = blazes[0]
        val closestBlock = findClosestEwBlock(lowerBlazeEWLocations, currentBlazeTarget!!, room) ?: return
        currentEtherwarpTarget = closestBlock.toBlockPos()
    }

    private fun findClosestEwBlock(locations: List<Vec3>, target: EntityArmorStand, room: Room): Vec3? {
        return locations
            .asSequence()
            .map { loc ->
                val origin = room.getRealCoords(loc).add(0.5, 2.54, 0.5)
                val (yaw, pitch) = BowUtils.getYawAndPitchOrigin(origin, target.positionVector.add(0.0, -0.9, 0.0))
                BowUtils.findHitEntity(origin, yaw.toDouble(), pitch.toDouble())
                    ?.firstOrNull()
                    ?.takeIf {
                        it.positionVector.distanceTo(target.positionVector) < 3
                    }
                    ?.let { hitEntity ->

                        loc to hitEntity.positionVector.distanceTo(origin)
                    }
            }
            .filterNotNull()
            .minByOrNull { it.second }
            ?.first
    }


    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        val room = DungeonUtils.currentRoom ?: return
        if (!room.data.name.equalsOneOf("Lower Blaze", "Higher Blaze")) return
        currentEtherwarpTarget?.let {
            if (blazeHighlight) Renderer.drawBlock(it, blazeHighlightColor)
        }

        val pos = room.getRealCoords(12, 70, 24).toVec3().add(0.5, 0.5, 0.5)
        Renderer.drawBox(
            AxisAlignedBB(
                pos.xCoord - 0.03,
                pos.yCoord,
                pos.zCoord - 0.03,
                pos.xCoord + 0.03,
                pos.yCoord + 0.06,
                pos.zCoord + 0.03
            ), Color.GREEN, fillAlpha = 0, outlineWidth = 1.5F
        )

        lowerBlazeEWLocations.forEach {
            val pos1 = room.getRealCoords(it).add(0.5, 1.0, 0.5)
            Renderer.drawBox(
                AxisAlignedBB(
                    pos1.xCoord - 0.03,
                    pos1.yCoord,
                    pos1.zCoord - 0.03,
                    pos1.xCoord + 0.03,
                    pos1.yCoord + 0.06,
                    pos1.zCoord + 0.03
                ), Color.GREEN, fillAlpha = 0, outlineWidth = 1.5F
            )
        }

        currentBlazeTarget?.let {
            Renderer.drawBox(
                AxisAlignedBB(
                    it.posX + 0.3,
                    it.posY - 1.8,
                    it.posZ + 0.3,
                    it.posX - 0.3,
                    it.posY,
                    it.posZ - 0.3
                ), Color.GREEN, fillAlpha = 0, outlineWidth = 1.5F
            )
        }

    }
    private fun rotateAndClick(state: SwapManager.SwapState, rot: Pair<Float, Float>){
        when (state) {
            SwapManager.SwapState.ALREADY_HELD -> {
                if (!silent) RotationUtils.setAngles(rot.first, rot.second)
                PacketUtils.c03ScheduleTask(cancel = true) {
                    PacketUtils.sendPacket(C05PacketPlayerLook(rot.first, rot.second, mc.thePlayer.onGround))
                    PlayerUtils.airClick()
                }
            }

            SwapManager.SwapState.SWAPPED -> {
                if (!silent) RotationUtils.setAngles(rot.first, rot.second)
                PacketUtils.c03ScheduleTask(1, cancel = true) {
                    PacketUtils.sendPacket(C05PacketPlayerLook(rot.first, rot.second, mc.thePlayer.onGround))
                    PlayerUtils.airClick()
                }
            }

            SwapManager.SwapState.UNKNOWN -> {
                return
            }

            SwapManager.SwapState.TOO_FAST -> {
                return
            }
        }
    }


    private fun shootAt(pos: Vec3) {
        lastShotTime = System.currentTimeMillis()
        val rot = RotationUtils.getYawAndPitch(pos, true)
        mc.thePlayer.heldItem.skyblockID
        val state = SwapManager.swapFromSBId(
            "ARTISANAL_SHORTBOW",
            "DRAGON_SHORTBOW",
            "JUJU_SHORTBOW",
            "ITEM_SPIRIT_BOW"
        )
        rotateAndClick(state, rot)

    }

    @SubscribeEvent
    fun onS08(event: S08Event) {
        PacketUtils.c03ScheduleTask(2) { warping = false }
    }
}