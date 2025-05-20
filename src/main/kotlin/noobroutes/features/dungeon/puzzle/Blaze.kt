package noobroutes.features.dungeon.puzzle


import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.events.impl.RoomEnterEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.ColorSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.*
import noobroutes.utils.render.Color
import noobroutes.utils.render.RenderUtils.renderVec
import noobroutes.utils.render.Renderer
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.tiles.Room


object Blaze : Module(
    "Blaze",
    description = "Automatically completes the higher or lower puzzle",
    category = Category.DUNGEON
) {

    private var lastShotTime = 0L
    private var lastShotTarget: EntityArmorStand? = null

    private var blazes = mutableListOf<EntityArmorStand>()
    private val blazeHealthRegex = Regex("^\\[Lv15] Blaze [\\d,]+/([\\d,]+)❤$")
    private var currentEtherwarpTarget: BlockPos? = null
    private var currentBlazeTarget: EntityArmorStand? = null
    private val silent by BooleanSetting("Silent", description = "Server Side Rotations. Only works with zpew")
    private var awaitingRoomChange = false

    private var hasAuraedBlock = false

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
        Vec3(8.0, 37.0, 11.0)
    )
    private val higherBlazeEWLocations = listOf<Vec3>(
        Vec3(19.0, 90.0, 20.0),
        Vec3(20.0, 85.0, 11.0),
        Vec3(9.0, 113.0, 18.0),
        Vec3(8.0, 107.0, 9.0)
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
        hasAuraedBlock = false
        if (event.room?.data == null || !event.room.data.name.equalsOneOf("Lower Blaze", "Higher Blaze")) return
        getBlaze()
        when (event.room.data.name) {
            "Lower Blaze" -> {
                Etherwarper.etherwarpToVec3(event.room.getRealCoords(12, 70, 24).toVec3().add(0.5, 0.5, 0.5), silent)
            }
            "Higher Blaze" -> {
                Etherwarper.etherwarpToVec3(event.room.getRealCoords(15, 69, 14).toVec3().add(0.5, 0.5, 0.5), silent)
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







    @SubscribeEvent
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || Etherwarper.warping || mc.thePlayer == null) return
        val room = DungeonUtils.currentRoom ?: return
        if (!room.data.name.equalsOneOf("Lower Blaze", "Higher Blaze")) return

        if (auraSecret && mc.thePlayer.renderVec.distanceTo(room.getRealCoords(2, 68, 27).toVec3()) < 5.7 && !hasAuraedBlock) {
            AuraManager.auraBlock(room.getRealCoords(2, 68, 27))
            hasAuraedBlock = true
        }
        getBlaze()
        if (handleConnectPoints(room)) return
        if (blazes.isEmpty()) return
        currentBlazeTarget = blazes[0]
        if (mc.thePlayer.posY > 60 && room.data.name == "Lower Blaze") return
        if (mc.thePlayer.posY < 78 && room.data.name == "Higher Blaze") return
        val ewPoints: List<Vec3> = if (room.data.name == "Lower Blaze") {
            lowerBlazeEWLocations.map { room.getRealCoords(it) }
        } else {
            higherBlazeEWLocations.map { room.getRealCoords(it) }
        }

        val closestBlock = findClosestEwBlock(ewPoints, currentBlazeTarget!!, room) ?: return
        currentEtherwarpTarget = closestBlock.toBlockPos()

        if (!isOnBlock(currentEtherwarpTarget!!)) {
            Etherwarper.etherwarpToVec3(closestBlock.add(0.5, 1.1, 0.5), silent)
            return
        }


        if (System.currentTimeMillis() - lastShotTime < shotCooldown) return
        shootAt(currentBlazeTarget!!.positionVector.add(0.0, -0.9, 0.0))
    }

    private fun handleConnectPoints(room: Room): Boolean {
        if (room.data.name == "Lower Blaze" && isOnBlock(room.getRealCoords(BlockPos(12, 70, 24)))) {
            Etherwarper.etherwarpToVec3(room.getRealCoords(22, 56, 16).toVec3().add(0.5, 1.0, 0.5), silent)
            return true
        } else if (room.data.name == "Higher Blaze" && isOnBlock(room.getRealCoords(BlockPos(15, 69, 14)))) {
            Etherwarper.etherwarpToVec3(room.getRealCoords(20, 85, 11).toVec3().add(0.5, 1.0, 0.5), silent)
            return true
        }
        return false
    }



    private fun findClosestEwBlock(locations: List<Vec3>, target: EntityArmorStand, room: Room): Vec3? {
        return locations
            .asSequence()
            .map { loc ->
                val origin = loc.add(0.5, 2.56, 0.5)
                val (yaw, pitch) = BowUtils.getYawAndPitchOrigin(origin, target.positionVector.add(0.0, -0.9, 0.0))
                BowUtils.findHitEntity(origin, yaw.toDouble(), pitch.toDouble())
                    ?.firstOrNull()
                    ?.takeIf {
                        it.positionVector.distanceTo(target.positionVector) < 3
                    }
                    ?.let { hitEntity ->
                        origin to hitEntity.positionVector.distanceTo(loc)
                    }
            }
            .filterNotNull()
            .minByOrNull { it.second }
            ?.first?.add(-0.5, -2.56, -0.5)
    }


    @SubscribeEvent
    fun onRender(event: RenderWorldLastEvent) {
        val room = DungeonUtils.currentRoom ?: return
        if (!room.data.name.equalsOneOf("Lower Blaze", "Higher Blaze")) return
        currentEtherwarpTarget?.let {
            if (blazeHighlight) Renderer.drawBlock(it, blazeHighlightColor)
        }

        if (room.data.name == "Higher Blaze") {
            higherBlazeEWLocations.forEach {
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
        } else {
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
                RotationUtils.rotate(rot.first, rot.second, silent, RotationUtils.Action.RightClick)
            }

            SwapManager.SwapState.SWAPPED -> {
                Scheduler.schedulePreTickTask {
                    RotationUtils.rotate(rot.first, rot.second, silent, RotationUtils.Action.RightClick)
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
        lastShotTarget = currentBlazeTarget
        val rot = RotationUtils.getYawAndPitch(pos, true)
        val state = SwapManager.swapFromSBId(
            "ARTISANAL_SHORTBOW",
            "DRAGON_SHORTBOW",
            "JUJU_SHORTBOW",
            "ITEM_SPIRIT_BOW"
        )
        rotateAndClick(state, rot)
    }


}