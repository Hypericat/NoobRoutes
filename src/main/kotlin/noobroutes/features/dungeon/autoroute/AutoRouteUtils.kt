package noobroutes.features.dungeon.autoroute

import com.google.gson.JsonArray
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.Core.mc
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.PacketReturnEvent
import noobroutes.features.dungeon.autoroute.nodes.*
import noobroutes.features.move.Zpew
import noobroutes.utils.*
import noobroutes.utils.AutoP3Utils.walking
import noobroutes.utils.RotationUtils.offset
import noobroutes.utils.skyblock.LocationUtils
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.tiles.Room
import noobroutes.utils.skyblock.modMessage

object AutoRouteUtils {




    /**
     * Call inside a ClientTickEvent (start)
     */
    fun etherwarpToRelativeVec3(vec3: Vec3, room: Room, silent: Boolean = false){
        val target = room.getRealCoords(vec3)
        etherwarpToVec3(target, silent)
    }

    /**
     * Call inside a ClientTickEvent (start)
     */
    fun etherwarpToVec3(vec3: Vec3, silent: Boolean = false){
        val rot = RotationUtils.getYawAndPitch(vec3)
        etherwarp(rot.first, rot.second, silent)
    }

    fun etherwarp(yaw: Float, pitch: Float, silent: Boolean = false){
        PlayerUtils.stopVelocity()
        val state = if (LocationUtils.isSinglePlayer) SwapManager.swapFromId(277) else SwapManager.swapFromSBId("ASPECT_OF_THE_VOID")
        if (state == SwapManager.SwapState.UNKNOWN) return
        if (state == SwapManager.SwapState.TOO_FAST) {
            modMessage("Tried to 0 tick swap gg")
            return
        }

        if (!silent) RotationUtils.setAngles(yaw, pitch)
        walking = false
        PlayerUtils.sneak()
        Scheduler.schedulePreMotionUpdateTask {
            val event = it as MotionUpdateEvent.Pre
            event.yaw = yaw
            event.pitch = pitch
            if (!mc.thePlayer.isSneaking || state == SwapManager.SwapState.SWAPPED) {
                setRotation(yaw + offset, pitch)
                Scheduler.schedulePreTickTask {
                    ether()
                }
                return@schedulePreMotionUpdateTask
            }
            ether()
        }
    }

    var pearlSoundRegistered = false
    var sneakRegistered = false
    var unsneakRegistered = false

    fun ether() {
        sneakRegistered = true
        PlayerUtils.sneak()
    }

    var aotvTarget: BlockPos? = null
    fun aotv(pos: BlockPos?) {
        aotvTarget = pos
        unsneakRegistered = true
        PlayerUtils.unSneak()
    }

    var clipDistance = 0
    var clipRegistered = false

    fun testFunction(){
        devMessage(sneak)
        devMessage(serverSneak)
    }

    var sneak = false
    var serverSneak = false
    @SubscribeEvent
    fun onPacketSendReturn(event: PacketReturnEvent.Send) {
        if (event.packet !is C0BPacketEntityAction) return
        serverSneak = when (event.packet.action) {
            C0BPacketEntityAction.Action.START_SNEAKING -> true
            C0BPacketEntityAction.Action.STOP_SNEAKING -> false
            else -> serverSneak
        }
    }

    @SubscribeEvent
    fun onPacket(event: PacketEvent.Receive) {
        if (clipRegistered && event.packet is S08PacketPlayerPosLook) {
            event.isCanceled = true
            PacketUtils.sendPacket(C06PacketPlayerPosLook(event.packet.x, event.packet.y, event.packet.z, event.packet.yaw, event.packet.pitch, false))
            mc.thePlayer.setPosition(
                mc.thePlayer.posX.floor() + 0.5,
                mc.thePlayer.posY.floor() - clipDistance,
                mc.thePlayer.posZ.floor() + 0.5
            )
            pearlSoundRegistered = false
            clipRegistered = false
        }

        if (!pearlSoundRegistered || event.packet !is S29PacketSoundEffect) return
        if (event.packet.soundName != "random.bow" || event.packet.volume != 0.5f) return
        clipRegistered = true
    }

    @SubscribeEvent
    fun unsneak(event: RenderWorldLastEvent) {
        if (!unsneakRegistered) return
        if (mc.thePlayer.isSneaking) PlayerUtils.unSneak()
        if (serverSneak) return
        PlayerUtils.airClick()
        aotvTarget?.let { Zpew.doZeroPingAotv(it) }
        resetRotation()
        unsneakRegistered = false
        PlayerUtils.resyncSneak()
    }

    @SubscribeEvent
    fun sneak(event: RenderWorldLastEvent) {
        if (!sneakRegistered) return
        if (!mc.thePlayer.isSneaking) PlayerUtils.sneak()
        if (!serverSneak) return
        PlayerUtils.airClick()
        resetRotation()
        sneakRegistered = false
        PlayerUtils.resyncSneak()
    }

    fun resetRotation() {
        rotating = false
        rotatingPitch = null
        rotatingYaw = null
    }
    fun setRotation(yaw: Float?, pitch: Float?) {
        rotating = true
        rotatingPitch = pitch
        rotatingYaw = yaw
    }


    var rotating = false
    var rotatingYaw: Float? = null
    var rotatingPitch: Float? = null
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun motion(event: MotionUpdateEvent.Pre) {
        if (rotating) {
            rotatingYaw?.let {
                event.yaw = it + offset
            }
            rotatingPitch?.let {
                event.pitch = it + offset
            }
        }
    }

    fun meowConverter(file: Map<String, JsonArray>): MutableMap<String, MutableList<Node>> {
        devMessage("meowing")
        val routeMap = mutableMapOf<String, MutableList<Node>>()
        val routes = file["Routes"] ?: return mutableMapOf()
        routes.forEach {
            val route = it.asJsonObject
            val room = route.get("room").asString
            val meowType = route.get("type").asString
            val coords = Vec3(route.get("x").asDouble, route.get("y").asDouble, route.get("z").asDouble)
            val args = route.get("args").asJsonObject
            val data = route.get("data").asJsonObject
            val await = if (args.has("await_secret")) 1 else 0
            val delay = args.get("delay")?.asInt?.toLong() ?: 0L

            if (routeMap[room] == null) routeMap[room] = mutableListOf()
            when (meowType) {
                "etherwarp_target" -> {
                    val target = Vec3(data.get("x").asDouble, data.get("y").asDouble, data.get("z").asDouble)
                    routeMap[room]?.add(
                        Etherwarp(
                            coords,
                            target,
                            await,
                            false,
                            delay,
                            false,
                            false,
                            false,
                            false
                        )
                    )
                }
                "aotv" -> {
                    val yaw = data.get("yaw").asFloat
                    val pitch = data.get("pitch").asFloat
                    val target = BlockPos(data.get("x").asInt, data.get("y").asInt, data.get("z").asInt)
                    val aotv = Aotv(
                        coords,
                        target,
                        yaw,
                        pitch,
                        await,
                        false,
                        delay,
                        false,
                        false,
                        false,
                        false
                    )
                    aotv.meow = true
                    routeMap[room]?.add(
                        aotv
                    )
                }
                "bat" -> {
                    val yaw = data.get("yaw").asFloat
                    val pitch = data.get("pitch").asFloat
                    routeMap.getOrPut(room) {mutableListOf()} .add(
                        Bat(
                            coords,
                            null,
                            yaw,
                            pitch,
                            await,
                            false,
                            delay,
                            false,
                            false,
                            false,
                            false
                        )
                    )
                }
                "pearl_clip" -> {
                    val distance = data.get("distance").asInt
                    routeMap[room]?.add(
                        PearlClip(
                            coords,
                            distance - 1,
                            await,
                            false,
                            delay,
                            false,
                            false,
                            false,
                            false
                        )
                    )
                }
                "use_item" -> {
                    val name = data.get("item_name").asString
                    val yaw = data.get("yaw").asFloat
                    val pitch = data.get("pitch").asFloat
                    if (name == "pearl") {
                        routeMap[room]?.add(
                            Pearl(coords,
                                1,
                                yaw,
                                pitch,
                                await,
                                false,
                                delay,
                                false,
                                false,
                                false,
                                false
                            )
                        )

                    } else {
                        routeMap[room]?.add(
                            UseItem(
                                coords,
                                name,
                                yaw,
                                pitch,
                                await,
                                false,
                                delay,
                                false,
                                false,
                                false,
                                false
                            )
                        )
                    }
                }
                "walk" -> {
                    val yaw = data.get("yaw").asFloat
                    routeMap[room]?.add(
                        Walk(
                        coords,
                        yaw,
                        await,
                        false,
                        delay,
                        false,
                        false,
                        false,
                        false
                        )
                    )
                }
                else -> {}
            }
        }
        return routeMap
    }
}