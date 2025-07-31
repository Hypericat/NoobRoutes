package noobroutes.features.floor7.autop3

import net.minecraft.util.MathHelper
import noobroutes.Core.mc

interface CommandGenerated {
    fun generateRing(args: Array<out String>): Ring?

    fun getWalkFromArgs(args: Array<out String>): Boolean {
        return args.any {it == "walk"}
    }
    fun generateRingBaseFromArgs(args: Array<out String>): RingBase {
        val diameterString = args.firstOrNull { RingBase.diameterRegex.matches(it) }
        val diameter = diameterString?.let { RingBase.diameterRegex.find(it)?.groupValues?.get(1)?.toFloatOrNull() } ?: 1f
        val heightString = args.firstOrNull { RingBase.heightRegex.matches(it) }
        val height = heightString?.let { RingBase.heightRegex.find(it)?.groupValues?.get(1)?.toFloatOrNull() } ?: 1f
        return RingBase(
            mc.thePlayer.positionVector,
            MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw),
            args.any {it == "term"},
            args.any { it == "leap" },
            args.any {it == "left"},
            args.any {it == "center"},
            args.any {it == "rotate" || it == "look"},
            diameter,
            height
        )
    }
}