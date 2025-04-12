package com.github.wadey3636.noobroutes.utils

import com.github.wadey3636.noobroutes.features.puzzle.WaterBoard
import me.defnotstolen.Core.mc
import me.defnotstolen.events.impl.PacketEvent
import net.minecraft.client.settings.KeyBinding
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.network.play.client.C03PacketPlayer.C05PacketPlayerLook
import net.minecraft.network.play.client.C03PacketPlayer.C06PacketPlayerPosLook
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

object Utils {

    fun xPart(yaw: Float): Double {
        return -sin(yaw * Math.PI /180)
    }

    fun zPart(yaw: Float): Double {
        return cos(yaw * Math.PI /180)
    }

    val rat = listOf(
        "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡿⣻⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡏⠉⠻⣿⣿⢿⣿⠿⠛⢻⣟⣛⣩⣵⡾⠋⠁⢹⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣷⠀⣶⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡗⠀⠀⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣶⡿⠿⢿⣿⣿⣿⣿⣿⠋⢿⠛⣿⣿⡀⢾⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣷⣦⣭⣙⠻⢿⣿⣶⣦⣴⣿⣿⣿⣿⣿⣤⣴⣶⣿⣿⣧⣘⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣟⣛⣛⣛⣓⡒⠒⠛⠿⠟⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣯⣭⣛⣛⣛⠻⢿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣭⣭⡍⠀⠀⠀⣾⣿⣿⣏⠉⣹⣿⣿⡿⠋⠙⣿⢻⣿⡦⣭⣙⠻⢿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⠛⠋⠁⠀⠀⠀⠀⠘⠻⢿⣥⣍⣛⣿⡆⠀⠀⠉⠈⠉⢻⣦⣝⣻⣶⣭⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⡟⠉⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠈⠙⠃⠀⠀⠀⠀⠀⠀⠀⣷⣝⣿⣿⣿⣿⣿⣿⡿⠟⠛⠻⣿⣿⣿⣿⣿",
        "⡿⠿⢿⣿⣿⣿⣿⣿⣿⣿⠿⠟⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣿⣿⣿⣿⣿⣿⣿⣏⠀⠀⠀⠀⠘⣿⣿⣿⣿",
        "⢠⣶⡄⠘⣿⣿⣿⣿⡯⠁⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣿⣿⣿⣿⣿⣿⣿⣿⣆⠀⠀⠀⠀⠈⢿⣿⣿",
        "⣿⣿⡇⠀⢸⣿⣿⡟⠁⠀⠀⠀⠀⠀⠀⠀⠀⢠⣤⣤⡄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠐⣿⣿⣿⣿⣿⣿⣿⣿⣿⣧⠀⠀⠀⠀⠀⢻⣿",
        "⣿⣿⠁⠀⣾⣿⠏⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠉⠉⣿⣦⣀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢘⣿⣿⣿⣿⣿⣿⣿⣿⣿⣀⡀⠀⠀⠀⠀⠹",
        "⣿⠇⠀⣸⣿⡧⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢀⣸⣿⣿⣷⣦⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠈⠻⣿⣿⣿⣿⣿⣿⣿⣿⣿⣷⡄⠀⠀⠀⠀",
        "⡿⠀⢀⣿⣿⠃⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢠⡟⠉⠛⠻⠿⠋⠱⣄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠻⠿⣿⣿⣿⣿⣿⣿⠿⠿⣿⣶⡆⠀⣸",
        "⡇⠀⢸⣿⡏⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⡼⣷⠶⠾⠿⠶⠶⠶⣿⠂⢀⣤⣤⣄⣤⣄⣀⣶⣦⠀⠀⠰⠿⠿⠃⠀⣰⣿⣷⣌⠻⣿⠆⣽",
        "⠁⠀⣿⣿⣀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢱⣿⣿⣿⣿⡆⢸⣿⣿⡀⢸⣿⣿⣿⣿⣿⣿⣿⣅⢀⣀⣀⡀⠀⣠⣾⣿⣿⣿⣿⣿⣶⣾⣿",
        "⡄⠀⢸⣿⣿⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢸⣿⣿⣿⣿⡇⢸⣿⣿⣧⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⡇⠀⠈⣿⣿⣇⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢸⣿⣿⣿⣿⣿⣼⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⡀⠀⠘⣿⣿⡄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠘⠿⠽⣿⣿⣿⣭⡭⠿⠸⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣷⡀⠀⠈⠻⣿⡆⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣿⣷⡀⠀⠀⠀⠉⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣿⣿⣿⣆⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣠⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣿⣿⣿⣿⣷⣄⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⣰⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣿⣿⣿⣿⣿⣿⣿⣦⣄⣀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⠀⢤⣼⣿⠿⢿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿",
        "⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣷⣶⣦⣤⣤⣤⣶⣦⣿⣿⣶⣾⣿⣥⣤⣤⣬⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿⣿"
    )



    fun isClose(number1: Double, number2: Double): Boolean {
        return abs(number1 - number2) < 0.0001F
    }

}