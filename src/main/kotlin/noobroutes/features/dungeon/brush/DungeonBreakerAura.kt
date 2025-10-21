package noobroutes.features.dungeon.brush

import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.Utils.isEnd

object DungeonBreakerAura : Module("Dungeon Breaker Aura", description = "Dungeon breaker extras.", category = Category.DUNGEON) {
    private var delay by NumberSetting<Int>("Delay", 1, 1, 20, 1, description = "Delay between block breaks")
    private var forceHypixel by BooleanSetting("Force Hypixel", false, description = "Forces hypixel and dungeons")

    @SubscribeEvent
    fun onTick(event: TickEvent) {
        if (event.isEnd) return;
        // Probably need to handle packet order

    }
}