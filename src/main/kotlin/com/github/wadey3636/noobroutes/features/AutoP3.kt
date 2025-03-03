package com.github.wadey3636.noobroutes.features

import com.sun.org.apache.xpath.internal.operations.Bool
import me.odinmain.features.Category
import me.odinmain.features.Module
import me.odinmain.utils.Vec2
import me.odinmain.utils.skyblock.modMessage
import net.minecraft.util.Vec3
import org.lwjgl.input.Keyboard

data class Ring (val route: String, val type: String, val coords: Vec3, val direction: Vec2, val walk: Boolean, val look: Boolean)

object AutoP3: Module (
    name = "AutoP3",
    Keyboard.KEY_NONE,
    category = Category.RENDER,
    description = "AutoP3"
) {
    fun addRing(args: Array<out String>?) {
        if (args.isNullOrEmpty()) return modMessage("need args stoopid")
        when(args[0]) {
            "add" -> addNormalRing(args)
            "blink" -> modMessage("coming soon")
        }
    }
    private fun addNormalRing(args: Array<out String>?) {
        modMessage("added")
    }
}