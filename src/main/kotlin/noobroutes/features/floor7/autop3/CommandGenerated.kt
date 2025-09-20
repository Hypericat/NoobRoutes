package noobroutes.features.floor7.autop3

import net.minecraft.util.MathHelper
import noobroutes.Core.mc

interface CommandGenerated {
    fun generateRing(args: Array<out String>): Ring?

    fun getWalkFromArgs(args: Array<out String>): Boolean {
        return args.any {it.lowercase() == "walk"}
    }
    fun generateRingBaseFromArgs(args: Array<out String>): RingBase {
        val diameterString = args.firstOrNull { RingBase.diameterRegex.matches(it) }
        val diameter = diameterString?.let { RingBase.diameterRegex.find(it)?.groupValues?.get(1)?.toFloatOrNull() } ?: 1f
        val heightString = args.firstOrNull { RingBase.heightRegex.matches(it) }
        val height = heightString?.let { RingBase.heightRegex.find(it)?.groupValues?.get(1)?.toFloatOrNull() } ?: 1f

        var await = RingAwait.NONE
        for (arg in args) {
            val argAwait = RingAwait.getFromNameSafe(arg)
            if (argAwait != RingAwait.NONE) await = argAwait
        }



        return RingBase(
            mc.thePlayer.positionVector,
            MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw),
            await,
            args.any {it.lowercase() == "center"},
            args.any {it.lowercase() == "rotate" || it == "look"},
            diameter,
            height
        )
    }
}