package noobroutes.utils.routes

import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.passive.EntityBat
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S0DPacketCollectItem
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.Core.mc
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.PacketReturnEvent
import noobroutes.features.routes.AutoRoute
import noobroutes.features.routes.nodes.AutorouteNode
import noobroutes.utils.*
import noobroutes.utils.Utils.getEntitiesOfType
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.routes.RouteUtils.aotv
import noobroutes.utils.routes.RouteUtils.aotvTarget
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayerSq
import noobroutes.utils.skyblock.devMessage

object SecretUtils {


    val items = listOf(
        "Health Potion VIII Splash Potion",
        "Healing Potion 8 Splash Potion",
        "Healing Potion VIII Splash Potion",
        "Healing VIII Splash Potion",
        "Healing 8 Splash Potion",
        "Decoy",
        "Inflatable Jerry",
        "Spirit Leap",
        "Trap",
        "Training Weights",
        "Defuse Kit",
        "Dungeon Chest Key",
        "Treasure Talisman",
        "Revive Stone",
        "Architect's First Draft"
    )


    var secretCount = 0
    var awaitingAutorouteNode: AutorouteNode? = null
    var canSendC08 = true
    var batSpawnRegistered = false
    @SubscribeEvent
    fun item(event: PacketEvent.Receive) {
        if (event.packet !is S0DPacketCollectItem) return
        val entity = mc.theWorld.getEntityByID(event.packet.collectedItemEntityID)
        if (entity !is EntityItem) return
        val item = entity.entityItem.displayName.noControlCodes
        if (!items.contains(item)) return
        secretCount++
    }


    @SubscribeEvent
    fun onTick(event: ClientTickEvent){
        if (event.isEnd) return
        if (awaitingAutorouteNode != null && secretCount >= 0) {
            awaitingAutorouteNode?.run()
            awaitingAutorouteNode = null
        }
    }



    @SubscribeEvent
    fun click(event: PacketReturnEvent.Send) {
        if (event.packet !is C08PacketPlayerBlockPlacement) return
        canSendC08 = false
        Scheduler.scheduleLowestPreTickTask {
            canSendC08 = true
        }

        if (
            event.packet.position == null ||
            !isBlock(event.packet.position, Blocks.chest, Blocks.trapped_chest, Blocks.lever, Blocks.skull)
        ) return

        devMessage("clicked ${getBlockAt(event.packet.position).unlocalizedName}")
        Scheduler.schedulePreTickTask(3) {
            secretCount++
        }
    }


    @SubscribeEvent
    fun batDeath(event: PacketEvent.Receive) {
        if (
            event.packet !is S29PacketSoundEffect ||
            event.packet.soundName != "mob.bat.hurt" ||
            event.packet.positionVector.distanceToPlayerSq > 225
        ) return
        secretCount++
    }

    @SubscribeEvent
    fun onBat(event: ClientTickEvent) {
        if (event.isEnd || AutoRoute.canRoute) return
        if (!batSpawnRegistered) return
        val bats = mc.theWorld.getEntitiesOfType<EntityBat>()
        for (bat in bats) {
            if (bat.positionVector.distanceToPlayerSq > 225) continue
            devMessage("Bat Spawned")
            aotvTarget?.let { it1 -> aotv(it1) }
            batSpawnRegistered = false
        }
    }



}