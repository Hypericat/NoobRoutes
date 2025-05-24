package noobroutes.features.dungeon.autoroute

import net.minecraft.entity.item.EntityItem
import net.minecraft.entity.passive.EntityBat
import net.minecraft.init.Blocks
import net.minecraft.network.play.client.C08PacketPlayerBlockPlacement
import net.minecraft.network.play.server.S0DPacketCollectItem
import net.minecraft.network.play.server.S29PacketSoundEffect
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.Core.mc
import noobroutes.events.impl.MotionUpdateEvent
import noobroutes.events.impl.PacketEvent
import noobroutes.events.impl.PacketReturnEvent
import noobroutes.features.dungeon.autoroute.AutoRoute.batSpawnRegistered
import noobroutes.features.dungeon.autoroute.AutoRoute.clear
import noobroutes.features.dungeon.autoroute.AutoRoute.items
import noobroutes.features.dungeon.autoroute.AutoRoute.rotating
import noobroutes.features.dungeon.autoroute.AutoRoute.rotatingPitch
import noobroutes.features.dungeon.autoroute.AutoRoute.rotatingYaw
import noobroutes.utils.*
import noobroutes.utils.Utils.getEntitiesOfType
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.skyblock.PlayerUtils
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayerSq
import noobroutes.utils.skyblock.devMessage
import noobroutes.utils.skyblock.dungeon.DungeonUtils

object SecretUtils {
    var secretCount = 0
    var awaitingNode: Node? = null

    @SubscribeEvent
    fun item(event: PacketEvent.Receive) {
        if (event.packet !is S0DPacketCollectItem) return
        val item = (mc.theWorld.getEntityByID(event.packet.collectedItemEntityID) as EntityItem).entityItem.displayName.noControlCodes
        if (!items.contains(item)) return
        secretCount++
    }


    @SubscribeEvent
    fun onTick(event: ClientTickEvent){
        if (event.isEnd) return
        if (awaitingNode != null && secretCount >= 0) {
            val room = DungeonUtils.currentRoom ?: return
            awaitingNode?.tick(room)
            Scheduler.schedulePreMovementUpdateTask {
                rotating = false
                rotatingYaw = null
                rotatingPitch = null
                awaitingNode?.motion((it as MotionUpdateEvent.Pre), room)
                awaitingNode = null
            }
        }
    }



    @SubscribeEvent
    fun click(event: PacketReturnEvent.Send) {
        if (event.packet !is C08PacketPlayerBlockPlacement ||
            event.packet.position == null ||
            !isBlock(event.packet.position, Blocks.chest, Blocks.trapped_chest, Blocks.lever, Blocks.skull)
        ) return
        devMessage("clicked ${getBlockAt(event.packet.position).unlocalizedName}")
        Scheduler.schedulePreTickTask(1) {
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
        if (event.isEnd) return
        if (!batSpawnRegistered) return
        val bats = mc.theWorld.getEntitiesOfType<EntityBat>()
        for (bat in bats) {
            if (bat.positionVector.distanceToPlayerSq > 225) continue
            devMessage("Bat Spawned")
            Scheduler.schedulePreTickTask {
                PlayerUtils.airClick()
                rotating = false
                clear()
            }
        }
    }



}