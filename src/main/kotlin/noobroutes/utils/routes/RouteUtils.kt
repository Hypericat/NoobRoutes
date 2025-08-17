package noobroutes.utils.routes

import com.google.gson.JsonArray
import net.minecraft.network.play.client.C0BPacketEntityAction
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.MouseEvent
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.InputEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.Core.mc
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.PacketReturnEvent
import noobroutes.features.floor7.autop3.AutoP3MovementHandler
import noobroutes.features.move.Zpew
import noobroutes.features.routes.nodes.AutorouteNode
import noobroutes.features.routes.nodes.autoroutes.*
import noobroutes.utils.RotationUtils
import noobroutes.utils.Scheduler
import noobroutes.utils.SwapManager
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.skyblock.*
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRealCoords
import noobroutes.utils.skyblock.dungeon.tiles.UniqueRoom
import org.lwjgl.input.Keyboard

object RouteUtils {

    /**
     * Call inside a ClientTickEvent (start)
     */
    fun etherwarpToRelativeVec3(vec3: Vec3, room: UniqueRoom, silent: Boolean = false){
        val target = room.getRealCoords(vec3)
        etherwarpToVec3(target, silent)
    }

    /**
     * Call inside a ClientTickEvent (start)
     */
    fun etherwarpToVec3(vec3: Vec3, silent: Boolean = false){
        val rot = RotationUtils.getYawAndPitch(vec3, true)
        etherwarp(rot.first, rot.second, silent)
    }

    fun etherwarp(yaw: Float, pitch: Float, silent: Boolean = false){
        swapToEtherwarp()
        PlayerUtils.stopVelocity()
        setRotation(yaw, pitch, silent)
        AutoP3MovementHandler.resetShit()
        lastRoute = System.currentTimeMillis()
        ether()
    }


    fun swapToEtherwarp(){
        val state = if (LocationUtils.isSinglePlayer) SwapManager.swapFromId(277) else SwapManager.swapFromSBId("ASPECT_OF_THE_VOID")
        if (state == SwapManager.SwapState.UNKNOWN) return
        if (state == SwapManager.SwapState.TOO_FAST) {
            modMessage("Tried to 0 tick swap gg")
            return
        }
    }

    fun pearlClip(distance: Int, silent: Boolean) {
        if (distance > 70) return modMessage("Invalid Clip Distance")
        AutoP3MovementHandler.resetShit()
        PlayerUtils.unPressKeys()
        val state = SwapManager.swapFromName("ender pearl")
        if (state == SwapManager.SwapState.UNKNOWN) return
        if (state == SwapManager.SwapState.TOO_FAST) {
            modMessage("Tried to 0 tick swap gg")
            return
        }
        clipDistance = distance
        if (LocationUtils.isSinglePlayer) {
            Scheduler.schedulePreTickTask(1) { sendChatMessage("/tp ~ ~-$distance ~") }
        }
        pearlSoundRegistered = true
        setRotation(null, 90f, silent)
        Scheduler.schedulePostTickTask { rightClick() }
        //rightClick()
    }

    @SubscribeEvent
    fun onKeyInputEvent(event: InputEvent.KeyInputEvent){
        val key = Keyboard.getEventKey()
        if (key == mc.gameSettings.keyBindSneak.keyCode && routing) {
            PlayerUtils.setSneak(PlayerUtils.lastSetSneakState)
        }
    }

    var pearlSoundRegistered = false
    var sneakRegistered = false
    var unsneakRegistered = false
    var rightClickRegistered = false

    fun rightClick(){
        rightClickRegistered = true
    }

    fun unsneak(){
        unsneakRegistered = true
        PlayerUtils.unSneak(true)
    }

    fun ether() {
        sneakRegistered = true
        PlayerUtils.sneak(true)
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
        if (clipRegistered && event.packet is S08PacketPlayerPosLook && !event.isCanceled) {
            /*event.isCanceled = true
            PacketUtils.sendPacket(C06PacketPlayerPosLook(event.packet.x, event.packet.y, event.packet.z, event.packet.yaw, event.packet.pitch, false))
            mc.thePlayer.setPosition(
                event.packet.x,
                mc.thePlayer.posY.floor() - clipDistance,
                event.packet.z
            )*/
            Scheduler.schedulePreTickTask {
                mc.thePlayer.setPosition(
                    event.packet.x,
                    event.packet.y - clipDistance - 1,
                    event.packet.z
                )
            }
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
        if (mc.thePlayer.isSneaking) PlayerUtils.unSneak(true)
        if (serverSneak) return
        PlayerUtils.airClick()
        unsneakRegistered = false
        PlayerUtils.resyncSneak()
    }

    @SubscribeEvent
    fun rightClickExecute(event: RenderWorldLastEvent) {
        if (!rightClickRegistered) return
        PlayerUtils.airClick()
        rightClickRegistered = false
        PlayerUtils.resyncSneak()
    }

    @SubscribeEvent
    fun sneak(event: RenderWorldLastEvent) {
        if (!sneakRegistered) return
        if (!mc.thePlayer.isSneaking) PlayerUtils.sneak(true)
        if (!serverSneak) return
        PlayerUtils.airClick()
        sneakRegistered = false
        PlayerUtils.resyncSneak()
    }

    fun resetRotation() {
        rotating = false
        rotatingPitch = null
        rotatingYaw = null
    }

    fun setRotation(yaw: Float?, pitch: Float?, silent: Boolean) {
        if (!silent) RotationUtils.setAngles(yaw, pitch)
        rotating = true
        rotatingPitch = pitch
        rotatingYaw = yaw
    }


    private var rotating = false
    private var rotatingYaw: Float? = null
    private var rotatingPitch: Float? = null
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun motion(event: MotionUpdateEvent.Pre) {
        if (PlayerUtils.movementKeysPressed) {
            resetRotation()
            return
        }
        if (rotating) {
            rotatingYaw?.let {
                event.yaw = it
            }
            rotatingPitch?.let {
                event.pitch = it.coerceIn(-90f, 90f)
            }
        }
    }

    var lastRoute = 0L
    inline val routing get() = System.currentTimeMillis() - lastRoute < 51

    var pearls = 0
    @SubscribeEvent(priority = EventPriority.LOWEST)
    fun pearl(event: TickEvent.ClientTickEvent){
        if (event.isEnd || pearls < 1) return
        if (PlayerUtils.movementKeysPressed) {
            pearls = 0
            return
        }
        pearls--
        lastRoute = System.currentTimeMillis()
        unsneak()
    }



    @SubscribeEvent
    fun onMouse(event: MouseEvent){
        if (event.button == 1 && event.buttonstate && routing) {
            event.isCanceled = true
        }
        if ((event.dx != 0 || event.dy != 0) && !routing) resetRotation()
    }

    fun meowConverter(file: Map<String, JsonArray>): MutableMap<String, MutableList<AutorouteNode>> {
        devMessage("meowing")
        val routeMap = mutableMapOf<String, MutableList<AutorouteNode>>()
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
                    routeMap.getOrPut(room) {mutableListOf()} .add(
                        Aotv(
                            coords,
                            yaw,
                            pitch,
                            await,
                            delay,
                            false,
                            false,
                            false,
                            false
                        )
                    )
                }
                "bat" -> {
                    val yaw = data.get("yaw").asFloat
                    val pitch = data.get("pitch").asFloat
                    routeMap.getOrPut(room) {mutableListOf()} .add(
                        Bat(
                            coords,
                            yaw,
                            pitch,
                            await,
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
                            Pearl(
                                coords,
                                1,
                                yaw,
                                pitch,
                                await,
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