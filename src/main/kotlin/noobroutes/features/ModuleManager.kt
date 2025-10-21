package noobroutes.features

import net.minecraft.network.Packet
import net.minecraftforge.client.event.RenderGameOverlayEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.Core.logger
import noobroutes.Core.mc
import noobroutes.events.impl.ChatPacketEvent
import noobroutes.events.impl.InputEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.ServerTickEvent
import noobroutes.features.dungeon.AutoBloodRush
import noobroutes.features.dungeon.AutoClose
import noobroutes.features.dungeon.SecretAura
import noobroutes.features.dungeon.brush.BrushModule
import noobroutes.features.dungeon.brush.DungeonBreakerAura
import noobroutes.features.dungeon.puzzle.IceFill
import noobroutes.features.dungeon.puzzle.TicTacToe
import noobroutes.features.dungeon.puzzle.WaterBoard
import noobroutes.features.dungeon.puzzle.Weirdos
import noobroutes.features.floor7.*
import noobroutes.features.floor7.autop3.AutoP3
import noobroutes.features.misc.*
import noobroutes.features.move.*
import noobroutes.features.render.*
import noobroutes.features.routes.AutoRoute
import noobroutes.features.routes.DynamicRoute
import noobroutes.features.settings.impl.KeybindSetting
import noobroutes.ui.clickgui.ClickGui
import noobroutes.ui.hud.EditHUDGui
import noobroutes.ui.hud.HudElement
import noobroutes.utils.capitalizeFirst
import noobroutes.utils.profile
import noobroutes.utils.render.getTextWidth

/**
 * Class that contains all Modules and huds
 * @author Aton, Bonsai
 */
object ModuleManager {
    data class PacketFunction<T : Packet<*>>(
        val type: Class<T>,
        val function: (T) -> Unit,
        val shouldRun: () -> Boolean,
    )

    data class MessageFunction(val filter: Regex, val shouldRun: () -> Boolean, val function: (MatchResult) -> Unit)

    data class TickTask(var ticksLeft: Int, val server: Boolean, val function: () -> Unit)

    val packetFunctions = mutableListOf<PacketFunction<Packet<*>>>()
    val messageFunctions = mutableListOf<MessageFunction>()
    val worldLoadFunctions = mutableListOf<() -> Unit>()
    val tickTasks = mutableListOf<TickTask>()
    val huds = arrayListOf<HudElement>()

    val modules: ArrayList<Module> = arrayListOf(
        ClickGUIModule,
        Esp,
        BlinkKeybind,
        AutoP3,
        NoDebuff,
        Zpew,
        Simulation,
        CoreClip,
        LavaClip,
        HClip,
        StormClip,
        Trail,
        NoTnT,
        VertJerry,
        SexAura,
        TimerHud,
        InstaMid,
        Auto4,
        LeverAura,
        TicTacToe,
        Weirdos,
        MelodyMessage,
        AutoSS,
        InstantSlow,
        Doorless,
        Penis,
        RotationVisualizer,
        BrushModule,
        FreeCam,
        QOL,
        BarPhase,
        AutoBloodRush,
        AutoPath,
        EWPathfinderModule,
        DynamicRoute,
        WaterBoard,
        IceFill,
        SecretAura,
        AutoClose,
        AutoRoute,
        Relics,
        TickControl,
        HotBarSwitcher,
        LowHop,
        SilentRotations,
        DungeonBreakerAura,
        //AutoAlign,
        AutoDojo
    )

    init {
        logger.info("initializing modules")
        for (module in modules) {
            module.keybinding?.let {
                module.register(KeybindSetting("Keybind", it, "Toggles the module"))
            }
        }
    }

    fun addModules(vararg module: Module) {
        for (i in module) {
            modules.add(i)
            i.keybinding?.let { i.register(KeybindSetting("Keybind", it, "Toggles the module")) }
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onTick(event: TickEvent.ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        tickTaskTick()
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onServerTick(event: ServerTickEvent) {
        tickTaskTick(true)
    }

    private fun tickTaskTick(server: Boolean = false) {
        tickTasks.removeAll {
            if (it.server != server) return@removeAll false
            if (it.ticksLeft <= 0) {
                it.function()
                return@removeAll true
            }
            it.ticksLeft--
            false
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onReceivePacket(event: PacketEvent.Receive) {
        packetFunctions.forEach {
            if (it.type.isInstance(event.packet) && it.shouldRun.invoke()) it.function(event.packet)
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onSendPacket(event: PacketEvent.Send) {
        packetFunctions.forEach {
            if (it.type.isInstance(event.packet) && it.shouldRun.invoke()) it.function(event.packet)
        }
    }

    @SubscribeEvent(receiveCanceled = true)
    fun onChatPacket(event: ChatPacketEvent) {
        messageFunctions.forEach {
            if (it.shouldRun()) it.function(it.filter.find(event.message) ?: return@forEach)
        }
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        worldLoadFunctions
            .forEach { it.invoke() }
    }

    @SubscribeEvent
    fun activateModuleKeyBinds(event: InputEvent.Keyboard) {
        for (module in modules) {
            for (setting in module.settings) {
                if (setting is KeybindSetting && setting.value.key == event.keycode) {
                    setting.value.onPress?.invoke()
                }
            }
        }
    }

    @SubscribeEvent
    fun activateModuleMouseBinds(event: InputEvent.Mouse) {
        for (module in modules) {
            for (setting in module.settings) {
                if (setting is KeybindSetting && setting.value.key + 100 == event.keycode) {
                    setting.value.onPress?.invoke()
                }
            }
        }
    }

    @SubscribeEvent
    fun onRenderOverlay(event: RenderGameOverlayEvent.Post) {
        if (event.type != RenderGameOverlayEvent.ElementType.ALL || mc.currentScreen == EditHUDGui || mc.currentScreen == ClickGui) return

        profile("Noobroutes Hud") {
            for (i in 0 until huds.size) {
                huds[i].draw(false)
            }
        }
    }

    fun getModuleByName(name: String?): Module? = modules.firstOrNull { it.name.equals(name, true) }

    fun generateFeatureList(): String {
        val sortedCategories = modules.sortedByDescending { getTextWidth(it.name, 18f) }.groupBy { it.category }.entries
            .sortedBy{ Category.entries.associateWith { it.ordinal }[it.key] }

        val featureList = StringBuilder()

        for ((category, modulesInCategory) in sortedCategories) {
            val displayName = category.name.lowercase().capitalizeFirst()
            featureList.appendLine("Category: ${if (displayName == "Floor7") "Floor 7" else displayName}")
            for (module in modulesInCategory) {
                featureList.appendLine("- ${module.name}: ${module.description}")
            }
            featureList.appendLine()
        }
        return featureList.toString()
    }
}