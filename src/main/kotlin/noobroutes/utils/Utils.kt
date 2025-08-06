package noobroutes.utils

import net.minecraft.block.state.IBlockState
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.GuiScreen
import net.minecraft.client.multiplayer.WorldClient
import net.minecraft.entity.Entity
import net.minecraft.entity.EntityLivingBase
import net.minecraft.entity.SharedMonsterAttributes
import net.minecraft.event.ClickEvent
import net.minecraft.event.HoverEvent
import net.minecraft.inventory.Container
import net.minecraft.inventory.ContainerChest
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.network.INetHandler
import net.minecraft.network.Packet
import net.minecraft.network.play.client.C03PacketPlayer
import net.minecraft.network.play.client.C0FPacketConfirmTransaction
import net.minecraft.network.play.server.S08PacketPlayerPosLook
import net.minecraft.network.play.server.S23PacketBlockChange
import net.minecraft.network.play.server.S32PacketConfirmTransaction
import net.minecraft.util.BlockPos
import net.minecraft.util.ChatComponentText
import net.minecraft.util.ChatStyle
import net.minecraft.util.Vec3
import net.minecraftforge.common.MinecraftForge
import net.minecraftforge.fml.common.eventhandler.Event
import net.minecraftforge.fml.common.eventhandler.EventPriority
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.Core
import noobroutes.Core.logger
import noobroutes.Core.mc
import noobroutes.INetwork
import noobroutes.IS23
import noobroutes.events.impl.MoveEntityWithHeadingEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.features.floor7.autop3.AutoP3MovementHandler

import noobroutes.utils.render.Color
import noobroutes.utils.render.ColorUtil.withAlpha
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.DungeonUtils
import noobroutes.utils.skyblock.dungeon.DungeonUtils.getRelativeCoords
import noobroutes.utils.skyblock.modMessage
import java.util.*
import kotlin.math.*

object Utils {
    const val COLOR_NORMALIZER = 1 / 255f

    inline val Float.xPart: Double get() = xPart(this)
    inline val Float.zPart: Double get() = zPart(this)


    fun xPart(yaw: Float): Double {
        return -sin(yaw * Math.PI /180)
    }

    fun Array<out String>.containsOneOf(vararg inputs: String, ignoreCase: Boolean = false): Boolean {
        if (!ignoreCase) return inputs.any {
            this.contains(it)
        }
        else {
            val lowercaseArray = this.map { it.lowercase() }
            return inputs.map { it.lowercase() }.any {
                lowercaseArray.contains(it)
            }
        }

    }

    fun zPart(yaw: Float): Double {
        return cos(yaw * Math.PI /180)
    }

    inline val TickEvent.isStart get() = this.phase == TickEvent.Phase.START
    inline val TickEvent.isEnd get() = this.phase == TickEvent.Phase.END
    inline val TickEvent.isNotStart get() = this.phase == TickEvent.Phase.END
    inline val TickEvent.isNotEnd get() = this.phase == TickEvent.Phase.START



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

    fun testFunctions(args: Array<out String>) {
        if (args.size < 2) {
            modMessage("Test: rel, relp")
            return
        }
        
        when (args[1].lowercase()) {
            "relativepos", "relpos", "rel" -> {
                val blockPos = DungeonUtils.currentRoom?.getRelativeCoords(mc.objectMouseOver.blockPos)  ?: return devMessage("Not in a room")
                GuiScreen.setClipboardString("BlockPos(${blockPos.x}, ${blockPos.y}, ${blockPos.z})")
                modMessage(blockPos)
            }

            "relativeplayerpos", "relppos", "relplayer", "playerrel", "relp" -> {
                val pos = DungeonUtils.currentRoom?.getRelativeCoords(mc.thePlayer.positionVector) ?: return
                GuiScreen.setClipboardString("Vec3(${pos.xCoord}, ${pos.yCoord}, ${pos.zCoord})")
                modMessage(pos)
            }

            "motion" -> {
                if (args.size < 4) return
                AutoP3MovementHandler.motionDrag = args[2].toDoubleOrNull() ?: return
                AutoP3MovementHandler.motionPush = args[3].toDoubleOrNull() ?: return

                devMessage("drag: ${AutoP3MovementHandler.motionDrag}, push: ${AutoP3MovementHandler.motionPush}")
            }

            "motionstart" -> {
                if (args.size < 4) return
                AutoP3MovementHandler.motionTick1 = args[2].toDoubleOrNull() ?: return
                AutoP3MovementHandler.motionTick2 = args[3].toDoubleOrNull() ?: return

                devMessage("tick1: ${AutoP3MovementHandler.motionTick1}, tick2: ${AutoP3MovementHandler.motionTick2}")
            }

            else -> {
                modMessage("All tests passed")
            }
        }
    }

    var lastPlayerPos = Vec3(0.0, 0.0, 0.0)
    var lastPlayerSpeed = Vec3(0.0, 0.0, 0.0)

    @SubscribeEvent
    fun beforeMoveEntityWithHeading(event: MoveEntityWithHeadingEvent.Pre) {
        if (mc.thePlayer == null) return
        lastPlayerPos = Vec3(mc.thePlayer.posX, mc.thePlayer.posY, mc.thePlayer.posZ)
        lastPlayerSpeed = Vec3(mc.thePlayer.motionX, mc.thePlayer.motionY, mc.thePlayer.motionZ)
    }


    fun isClose(number1: Double, number2: Double): Boolean {
        return abs(number1 - number2) < 0.0005F
    }

    inline fun <reified T : Entity> WorldClient.getEntitiesOfType(): List<T> {
        return this.loadedEntityList.filterIsInstance<T>()
    }

    val ItemStack?.ID: Int
        get() = Item.getIdFromItem(this?.item)

}

fun <T> Array<T>.requirement(req: Int): Boolean {
    return requirement(req, this)
}

fun <T> requirement(req: Int, args: Array<T>): Boolean {
    return args.size >= req
}

/**
 * Writes the given text to the clipboard.
 */
fun writeToClipboard(text: String, successMessage: String = "§aCopied to clipboard.") {
    GuiScreen.setClipboardString(text)
    if (successMessage.isNotEmpty()) modMessage(
        successMessage
    )
}

fun isOnBlock(vec3: Vec3): Boolean {
    return vec3.add(0.5, 1.0, 0.5).distanceTo(mc.thePlayer.positionVector) < 0.1
}

fun isOnBlock(pos: BlockPos): Boolean {
    return isOnBlock(pos.toVec3())
}

val FORMATTING_CODE_PATTERN = Regex("§[0-9a-fk-or]", RegexOption.IGNORE_CASE)

/**
 * Returns the string without any minecraft formatting codes.
 */
inline val String?.noControlCodes: String
    get() = this?.replace(FORMATTING_CODE_PATTERN, "") ?: ""

/**
 * Checks if the current string contains at least one of the specified strings.
 *
 * @param options List of strings to check.
 * @param ignoreCase If comparison should be case-sensitive or not.
 * @return `true` if the string contains at least one of the specified options, otherwise `false`.
 */
fun String.containsOneOf(vararg options: String, ignoreCase: Boolean = false): Boolean {
    return options.any { this.contains(it, ignoreCase) }
}

fun <E> MutableList<E>.coerceMax(max: Int): MutableList<E> {
    if (this.size > max) {
        this.subList(max, this.size).clear()
    }
    return this
}

fun <E> MutableList<E>.addLast(element: E): MutableList<E> {
    this.add(this.size - 1, element)
    return this
}



/**
 * Checks if the current string contains at least one of the specified strings.
 *
 * @param options List of strings to check.
 * @param ignoreCase If comparison should be case-sensitive or not.
 * @return `true` if the string contains at least one of the specified options, otherwise `false`.
 */
fun String.containsOneOf(options: Collection<String>, ignoreCase: Boolean = false): Boolean {
    return options.any { this.contains(it, ignoreCase) }
}

fun String.startsWithOneOf(vararg options: String, ignoreCase: Boolean = false): Boolean {
    return options.any { this.startsWith(it, ignoreCase) }
}

/**
 * Checks if the current object is equal to at least one of the specified objects.
 *
 * @param options List of other objects to check.
 * @return `true` if the object is equal to one of the specified objects.
 */
fun Any?.equalsOneOf(vararg options: Any?): Boolean {
    return options.any { this == it }
}

fun String?.matchesOneOf(vararg options: Regex): Boolean {
    return options.any { it.matches(this ?: "") }
}

/**
 * Floors the current Double number.
 * @return The floored Double number.
 */
fun Double.floor(): Double {
    return floor(this)
}

/**
 * Floors the current Float number.
 * @return The floored Float number.
 */
fun Float.floor(): Float {
    return floor(this.toDouble()).toFloat()
}




/**
 * Ceils the current Long number.
 * @return The ceiled Long number (no change as Long is already an integer).
 */
fun Long.floor(): Long {
    return this
}

fun Double.ceilToInt(): Int {
    return ceil(this).toInt()
}


/**
 * Ceils the current Double number.
 * @return The ceiled Double number.
 */
fun Double.ceil(): Double {
    return ceil(this)
}

/**
 * Ceils the current Float number.
 * @return The ceiled Float number.
 */
fun Float.ceil(): Float {
    return ceil(this.toDouble()).toFloat()
}




/**
 * Floors the current Long number.
 * @return The floored Long number (no change as Long is already an integer).
 */
fun Long.ceil(): Long {
    return this
}


/**
 * Rounds the current number to the specified number of decimals.
 * @param decimals The number of decimals to round to.
 * @return The rounded number.
 */
fun Number.round(decimals: Int): Number {
    require(decimals >= 0) { "Decimals must be non-negative" }
    val factor = 10.0.pow(decimals)
    return round(this.toDouble() * factor) / factor
}

val ContainerChest.name: String
    get() = this.lowerChestInventory?.displayName?.unformattedText ?: ""

val Container.name: String
    get() = (this as? ContainerChest)?.name ?: "Undefined Container"

operator fun Number.div(number: Number): Number {
    return this.toDouble() / number.toDouble()
}

operator fun Number.times(number: Number): Number {
    return this.toDouble() * number.toDouble()
}

operator fun Number.minus(number: Number): Number {
    return this.toDouble() - number.toDouble()
}

operator fun Number.plus(number: Number): Number {
    return this.toDouble() + number.toDouble()
}

/**
 * Posts an event to the event bus and catches any errors.
 * @author Skytils
 */
fun Event.postAndCatch(): Boolean {
    return runCatching {
        MinecraftForge.EVENT_BUS.post(this)
    }.onFailure {
        it.printStackTrace()
        logger.error("An error occurred", it)
        val style = ChatStyle()
        style.chatClickEvent = ClickEvent(ClickEvent.Action.RUN_COMMAND, "/od copy ```${it.stackTraceToString().lineSequence().take(10).joinToString("\n")}```")
        style.chatHoverEvent = HoverEvent(HoverEvent.Action.SHOW_TEXT, ChatComponentText("§6Click to copy the error to your clipboard."))
        modMessage(
            "${Core.VERSION} Caught an ${it::class.simpleName ?: "error"} at ${this::class.simpleName}. §cPlease click this message to copy and send it in the Noobroutes discord!",
            chatStyle = style
        )
    }.getOrDefault(isCanceled)
}

/**
 * Profiles the specified function with the specified string as profile section name.
 * Uses the minecraft profiler.
 *
 * @param name The name of the profile section.
 * @param func The code to profile.
 */
inline fun profile(name: String, func: () -> Unit) {
    startProfile(name)
    func()
    endProfile()
}

/**
 * Starts a minecraft profiler section with the specified name + "Noobroutes: ".
 * */
fun startProfile(name: String) {
    mc.mcProfiler.startSection("Noobroutes: $name")
}

/**
 * Ends the current minecraft profiler section.
 */
fun endProfile() {
    mc.mcProfiler.endSection()
}

/**
 * Returns the String with the first letter capitalized
 *
 * @return The String with the first letter capitalized
 */
fun String.capitalizeFirst(): String = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }

fun Color.coerceAlpha(min: Float, max: Float): Color {
    return if (this.alpha < min) this.withAlpha(min)
    else if (this.alpha > max) this.withAlpha(max)
    else this
}

fun <T> Collection<T>.getSafe(index: Int?): T? {
    return try {
        this.toList()[index ?: return null]
    } catch (_: Exception) {
        null
    }
}
fun <T> Collection<T>.lastSafe(): T? {
    return try {
        this.toList().last()
    } catch (_: Exception) {
        null
    }
}

/**
 * Formats a time duration in milliseconds into a human-readable string.
 *
 * The string will show hours, minutes, and seconds, with an optional number of decimal places for the seconds.
 *
 * @param time The time duration in milliseconds to be formatted.
 * @param decimalPlaces The number of decimal places to show for the seconds. Default is 2.
 * @return A formatted string representing the time duration. For example, "1h 2m 3.45s".
 */
fun formatTime(time: Long, decimalPlaces: Int = 2): String {
    if (time == 0L) return "0s"
    var remaining = time
    val hours = (remaining / 3600000).toInt().let {
        remaining -= it * 3600000
        if (it > 0) "${it}h " else ""
    }
    val minutes = (remaining / 60000).toInt().let {
        remaining -= it * 60000
        if (it > 0) "${it}m " else ""
    }
    val seconds = (remaining / 1000f).let {
        String.format(Locale.US, "%.${decimalPlaces}f", it)
    }
    return "$hours$minutes${seconds}s"
}


private val romanMap = mapOf('I' to 1, 'V' to 5, 'X' to 10, 'L' to 50, 'C' to 100, 'D' to 500, 'M' to 1000)
private val numberRegex = Regex("^[0-9]+$")
fun romanToInt(s: String): Int {
    return if (s.matches(numberRegex)) s.toInt()
    else {
        var result = 0
        for (i in 0 until s.length - 1) {
            val current = romanMap[s[i]] ?: 0
            val next = romanMap[s[i + 1]] ?: 0
            result += if (current < next) -current else current
        }
        result + (romanMap[s.last()] ?: 0)
    }
}


fun simulateClientReceivePacket(packet: Packet<*>) {
    (Minecraft.getMinecraft().netHandler.networkManager as INetwork).receive(packet as Packet<INetHandler>);
}

fun setClientSideBlockPacket(blockPos: BlockPos, blockState: IBlockState) {
    val packet = S23PacketBlockChange();
    (packet as IS23).`noobRoutes$setBlock`(blockPos, blockState)
    simulateClientReceivePacket(packet);
}


inline fun <T> MutableCollection<T>.removeFirstOrNull(predicate: (T) -> Boolean): T? {
    val first = firstOrNull(predicate) ?: return null
    this.remove(first)
    return first
}

fun Int.rangeAdd(add: Int): IntRange = this..this+add


fun runOnMCThread(run: () -> Unit) {
    if (!mc.isCallingFromMinecraftThread) mc.addScheduledTask(run) else run()
}

fun EntityLivingBase?.getSBMaxHealth(): Float {
    return this?.getEntityAttribute(SharedMonsterAttributes.maxHealth)?.baseValue?.toFloat() ?: 0f
}
