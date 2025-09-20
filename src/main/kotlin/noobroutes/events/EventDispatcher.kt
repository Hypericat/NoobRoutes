package noobroutes.events

import kotlinx.coroutines.launch
import net.minecraft.client.gui.inventory.GuiChest
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.item.EntityItem
import net.minecraft.inventory.ContainerChest
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S02PacketChat
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraft.network.play.server.S32PacketConfirmTransaction
import net.minecraftforge.client.event.GuiOpenEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.Core.mc
import noobroutes.Core.scope
import noobroutes.events.BossEventDispatcher.inBoss
import noobroutes.events.impl.*
import noobroutes.utils.*
import noobroutes.utils.skyblock.dungeon.DungeonUtils.dungeonItemDrops
import noobroutes.utils.skyblock.dungeon.DungeonUtils.inDungeons
import noobroutes.utils.skyblock.dungeon.DungeonUtils.isSecret
import noobroutes.utils.skyblock.unformattedName

object EventDispatcher {

    /**
     * Dispatches [SecretPickupEvent.Item]
     */
    @SubscribeEvent
    fun onRemoveEntity(event: EntityLeaveWorldEvent) = with(event.entity) {
        if (inDungeons && this is EntityItem && this.entityItem?.unformattedName?.containsOneOf(dungeonItemDrops, true) != false && mc.thePlayer.getDistanceToEntity(this) <= 6)
            SecretPickupEvent.Item(this).postAndCatch()
    }


    /**
     * Dispatches [SecretPickupEvent.Interact]
     */
    @SubscribeEvent
    fun onPacket(event: PacketEvent.Send) = with(event.packet) {
        if (inDungeons && this is C08PacketPlayerBlockPlacement && position != null)
            SecretPickupEvent.Interact(position, mc.theWorld?.getBlockState(position)?.takeIf { isSecret(it, position) } ?: return).postAndCatch()
    }

    /**
     * Dispatches [ChatPacketEvent], [ServerTickEvent], and [SecretPickupEvent.Bat]
     */
    @SubscribeEvent
    fun onPacket(event: PacketEvent.Receive) {
        if (event.packet is S29PacketSoundEffect && inDungeons && !inBoss && (event.packet.soundName.equalsOneOf("mob.bat.hurt", "mob.bat.death") && event.packet.volume == 0.1f)) SecretPickupEvent.Bat(event.packet).postAndCatch()

        if (event.packet is S32PacketConfirmTransaction) ServerTickEvent().postAndCatch()

        //if (event.packet !is S02PacketChat || !ChatPacketEvent(event.packet.chatComponent.unformattedText.noControlCodes).postAndCatch()) return
        //event.isCanceled = true
    }

    private var lastEntityClick = System.currentTimeMillis()

    @SubscribeEvent
    fun onPacketSent(event: PacketEvent.Send) {
        if (event.packet !is C02PacketUseEntity) return
        val entity = event.packet.getEntityFromWorld(mc.theWorld)
        if (entity !is EntityArmorStand) return
        val armorStand: EntityArmorStand = entity
        if (armorStand.name.noControlCodes.contains("Inactive Terminal", true)) lastEntityClick = System.currentTimeMillis()
    }

    val termNames = listOf(
        Regex("^Click in order!$"),
        Regex("^Select all the (.+?) items!$"),
        Regex("^What starts with: '(.+?)'\\?$"),
        Regex("^Change all to same color!$"),
        Regex("^Correct all the panes!$"),
        Regex("^Click the button on time!$")
    )

    @SubscribeEvent
    fun onS2D(event: S2DEvent) {
        val title = event.packet.windowTitle.unformattedText
        if (System.currentTimeMillis() - lastEntityClick < 400 && termNames.any{regex -> regex.matches(title)}) TermOpenEvent().postAndCatch()
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload){
        WorldChangeEvent().postAndCatch()
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        WorldChangeEvent().postAndCatch()
    }

}
