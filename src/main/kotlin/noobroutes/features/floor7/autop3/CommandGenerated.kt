package noobroutes.features.floor7.autop3

import net.minecraft.util.MathHelper
import noobroutes.Core.mc
import java.util.EnumSet

interface CommandGenerated {
    fun generateRing(args: Array<out String>): Ring?

    fun getWalkFromArgs(args: Array<out String>): Boolean {
        return args.any {it.lowercase() == "walk"}
    }

    fun getLowFromArgs(args: Array<out String>): Boolean {
        return args.any {it.lowercase() == "low" || it.lowercase() == "lowhop"}
    }

    fun getWalkBoost(args: Array<out String>): WalkBoost {
        return WalkBoost.entries.firstOrNull { walkBoost ->
            args.any { it.equals(walkBoost.name, ignoreCase = true) }
        } ?: WalkBoost.UNCHANGED
    }

    fun generateRingBaseFromArgs(args: Array<out String>): RingBase {
        val diameterString = args.firstOrNull { RingBase.diameterRegex.matches(it) }
        val diameter = diameterString?.let { RingBase.diameterRegex.find(it)?.groupValues?.get(1)?.toFloatOrNull() } ?: 1f
        val heightString = args.firstOrNull { RingBase.heightRegex.matches(it) }
        val height = heightString?.let { RingBase.heightRegex.find(it)?.groupValues?.get(1)?.toFloatOrNull() } ?: 1f

        val await = EnumSet.noneOf(RingAwait::class.java)
        for (arg in args) {
            val argAwait = RingAwait.getFromNameSafe(arg) ?: continue
            await.add(argAwait)
        }



        return RingBase(
            mc.thePlayer.positionVector,
            MathHelper.wrapAngleTo180_float(mc.thePlayer.rotationYaw),
            await,
            args.any {it.lowercase() == "center"},
            args.any {it.lowercase() == "rotate" || it.lowercase() == "look"},
            args.any {it.lowercase() == "stopwatch"},
            diameter,
            height
        )
    }
}