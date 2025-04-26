package noobroutes.features.test

import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.floor7.AutoP3
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.utils.Etherwarper
import noobroutes.utils.add
import noobroutes.utils.render.Color
import noobroutes.utils.render.Renderer
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import kotlin.math.sin

object EtherwarpTest : Module("Ew Test", description = "testing etherwarp", category = Category.MISC) {
    val etherwarps = mutableListOf<Pair<Vec3, Vec3>>()
    private val silent by BooleanSetting("Silent", true, description = "Silent etherwarp")


    @SubscribeEvent
    fun onClientTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START || mc.thePlayer == null) return
        for (etherwarp in etherwarps) {
            if (mc.thePlayer.positionVector.distanceTo(etherwarp.first) < 0.3) {
                Etherwarper.etherwarpToVec3(etherwarp.second.add(0.5, 0.5, 0.5), silent)
                return
            }
        }
    }

    @SubscribeEvent
    fun onRenderWorldLast(event: RenderWorldLastEvent) {
        etherwarps.forEach {
            renderRing(it.first)
            Renderer.draw3DLine(listOf(it.first, it.second.add(0.5, 0.5, 0.5)), Color.Companion.GREEN, depth = false)
        }

    }


    fun renderRing(ring: Vec3) {
        Renderer.drawCylinder(ring.add(
            Vec3(
                0.0,
                (0.45 * sin(System.currentTimeMillis().toDouble() / 300)) + 0.528,
                0.0
            )
        ), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.Companion.GREEN, depth = AutoP3.depth)
        Renderer.drawCylinder(ring.add(
            Vec3(
                0.0,
                (-0.45 * sin(System.currentTimeMillis().toDouble() / 300)) + 0.528,
                0.0
            )
        ), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.Companion.GREEN, depth = AutoP3.depth)
        Renderer.drawCylinder(ring.add(Vec3(0.0, 0.503, 0.0)), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.Companion.GREEN, depth = AutoP3.depth)
        Renderer.drawCylinder(ring.add(Vec3(0.0, 0.03, 0.0)), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.Companion.DARK_GRAY, depth = AutoP3.depth)
        Renderer.drawCylinder(ring.add(Vec3(0.0, 1.03, 0.0)), 0.6, 0.6, 0.01, 24, 1, 90, 0, 0, Color.Companion.DARK_GRAY, depth = AutoP3.depth)
    }


}