package noobroutes.features.dungeon.maplobotomizer

import net.minecraft.util.MovementInput
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.RenderTickEvent
import noobroutes.features.Module
import noobroutes.features.dungeon.maplobotomizer.FreeCam.freeCam
import noobroutes.features.dungeon.maplobotomizer.FreeCam.freeCamMomentumX
import noobroutes.features.dungeon.maplobotomizer.FreeCam.freeCamMomentumY
import noobroutes.features.dungeon.maplobotomizer.FreeCam.freeCamMomentumZ
import noobroutes.features.dungeon.maplobotomizer.FreeCam.freeCamPitch
import noobroutes.features.dungeon.maplobotomizer.FreeCam.freeCamX
import noobroutes.features.dungeon.maplobotomizer.FreeCam.freeCamY
import noobroutes.features.dungeon.maplobotomizer.FreeCam.freeCamYaw
import noobroutes.features.dungeon.maplobotomizer.FreeCam.freeCamZ
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.DropdownSetting
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.render.RenderUtils.renderVec
import org.lwjgl.input.Keyboard
import kotlin.jvm.internal.Intrinsics


object MapLobotomizer : Module("Map Lobotomizer", description = "It is just fme but way less laggy.") {


    val freeCamDropDown by DropdownSetting("Free Cam")
    val freeCamButton by KeybindSetting("Toggle", description = "Enter free cam", key = Keyboard.KEY_NONE).onPress {
        val freeCamState = !freeCam
        if (freeCamState) {
            val renderVec = mc.thePlayer.renderVec
            freeCamX = renderVec.xCoord
            freeCamY = renderVec.yCoord
            freeCamZ = renderVec.zCoord
            freeCamYaw = mc.thePlayer.rotationYaw
            freeCamPitch = mc.thePlayer.rotationPitch


        }
        freeCam = freeCamState
    }.withDependency { freeCamDropDown }
    val freeCamSpectatorMovement by BooleanSetting("Spectator Movement", description = "Moving forward and backward in free cam mode changes y level.")


}