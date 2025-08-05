package noobroutes.features.dungeon

import net.minecraft.block.Block
import net.minecraft.block.state.IBlockState
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.network.play.server.S04PacketEntityEquipment
import net.minecraft.network.play.server.S22PacketMultiBlockChange
import net.minecraft.network.play.server.S23PacketBlockChange
import net.minecraft.network.play.server.S24PacketBlockAction
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.Core.logger
import noobroutes.events.BossEventDispatcher
import noobroutes.events.impl.PacketEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.DropdownSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.features.settings.impl.SelectorSetting
import noobroutes.utils.*
import noobroutes.utils.Utils.isNotStart

object SecretAura : Module("Secret Aura", category = Category.DUNGEON, description = "Typical Secret Aura, is Disabled Inside of Boss. (Use Lever Aura)") {
    val enableOutsideOfDungeons by BooleanSetting("Enable Everywhere", description = "Enables the Aura outside of dungeons.")
    private val rangeDropDown by DropdownSetting("Range Settings")
    private val chestRange by NumberSetting("Range", 6.2, 4.0, 6.5, 0.1, description = "Range for Blocks inside dungeons.").withDependency { rangeDropDown }
    private val essenceRange by NumberSetting("Skull Range", 4.5, 2.0, 5.0, 0.1, description = "Range for Skulls inside dungeons.").withDependency { rangeDropDown }
    private val swapOn by SelectorSetting("Swap On", "Skulls", options = arrayListOf("None", "Skulls", "All"), description = "Swaps")
    private val swapTo by NumberSetting("Swap Slot", 1, 1, 9, 1, description = "The hotbar slot that the Secret Aura swaps to on click.").withDependency { swapOn != 0 }
    private val swapBack by BooleanSetting("Swap Back", description = "Determines whether the Secret Aura swaps back to the original slot after clicking").withDependency { swapOn != 0 }

    private const val REDSTONE_KEY_ID = "fed95410-aba1-39df-9b95-1d4f361eb66e"
    private const val WITHER_ESSENCE_ID = "e0f3e929-869e-3dca-9504-54c666ee6f23"

    private var isHoldingRedstoneKey = true
    private val clickedBlocks: BlockPosMap<Long> = BlockPosMap()
    private val blocksDone: BlockPosCache = BlockPosCache()
    var previousSlot = -1

    private data class BlockDistance(val block: Block, val pos: BlockPos, val distanceSq: Double)


    override fun onDisable() {
        super.onDisable()
        clear()
    }

    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        clear()
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        clear()
    }

    @SubscribeEvent
    fun onPostTick(event: ClientTickEvent) {
        if (event.isNotStart || mc.thePlayer == null || mc.theWorld == null || mc.currentScreen != null || BossEventDispatcher.inBoss) return

        var blockCandidate = BlockDistance(Blocks.air, BlockPos(Int.MAX_VALUE, 69, Int.MIN_VALUE), Double.POSITIVE_INFINITY)

        val eyePos = mc.thePlayer.getPositionEyes(0f)

        val box: AxisAlignedBB = AxisAlignedBB(eyePos.xCoord, eyePos.yCoord, eyePos.zCoord, eyePos.xCoord, eyePos.yCoord, eyePos.zCoord).expand(chestRange, chestRange, chestRange)

        val sqEssence = essenceRange * essenceRange
        val sqChest = chestRange * chestRange

        for (pos in getBlockPosWithinAABB(box)) {
            if (blocksDone.contains(pos)) continue
            val lastClickTime = clickedBlocks[pos]
            if (lastClickTime != null && lastClickTime + 500 > System.currentTimeMillis()) continue



            val currentBlock = mc.theWorld.getBlockState(pos).block
            if (!isValidBlock(currentBlock, pos)) continue
            val currentDistanceSq = eyePos.squareDistanceTo(Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5))

            if ((currentBlock == Blocks.skull && currentDistanceSq > sqEssence) || currentDistanceSq > sqChest) continue

            if (currentDistanceSq < blockCandidate.distanceSq) {
                blockCandidate = BlockDistance(currentBlock, pos, currentDistanceSq)
            }
        }

        if (blockCandidate.block == Blocks.air) {
            if (previousSlot != -1) {
                SwapManager.swapToSlot(previousSlot)
                previousSlot = -1
            }
            return
        }
        //Coercing the value should be pointless, but just in case the user somehow sets it to a higher or lower value than possible
        when (swapOn) {
            0 -> {}
            1 -> {
                if (blockCandidate.block === Blocks.skull) handleSwap()
            }
            2 -> {
                handleSwap()
            }
        }


        clickedBlocks[blockCandidate.pos] = System.currentTimeMillis()
        AuraManager.auraBlock(blockCandidate.pos)
    }

    private fun handleSwap() {
        if (previousSlot == -1) previousSlot = mc.thePlayer.inventory.currentItem
        SwapManager.swapToSlot((swapTo - 1).coerceIn(0, 8))
    }

    private fun isValidBlock(block: Block, position: BlockPos): Boolean {
        return when (block) {
            Blocks.air -> false // Prob save time on average
            Blocks.chest -> true
            Blocks.lever -> true
            Blocks.redstone_block -> isHoldingRedstoneKey
            Blocks.skull -> {
                val skull = getSkull(position) ?: return false
                skull.profileID == REDSTONE_KEY_ID || skull.profileID == WITHER_ESSENCE_ID
            }
            else -> false
        }
    }

    /**
     * Logic taken from secret guide
     * The original code is so bad T-T
     */
    @SubscribeEvent
    fun onPacketReceive(event: PacketEvent.Receive){
        val packet = event.packet
        when (packet) {
            is S24PacketBlockAction -> {
                if (packet.blockType == Blocks.chest) blocksDone.add(packet.blockPosition)
            }
            is S23PacketBlockChange -> {
                handleChangedBlock(packet.blockState, packet.blockPosition)
            }
            is S22PacketMultiBlockChange -> {
                for (block in packet.changedBlocks) {
                    handleChangedBlock(block.blockState, block.pos)
                }
            }
            is S04PacketEntityEquipment -> {
                val entity = mc.theWorld.getEntityByID(packet.entityID) as? EntityArmorStand ?: return
                if (packet.equipmentSlot != 4) return
                val profileID = packet.itemStack?.profileID ?: return
                if (profileID != WITHER_ESSENCE_ID) return
                blocksDone.add(BlockPos(entity.posX, entity.posY + 2, entity.posZ))
            }
        }
    }

    private fun handleChangedBlock(packetState: IBlockState, pos: BlockPos){
        val state = getBlockStateAt(pos)
        if (state.block === Blocks.lever) {
            blocksDone.add(pos)
            return
        }
        if (state.block === Blocks.skull && packetState.block === Blocks.air) {
            val profileID = getSkull(pos)?.profileID ?: return
            if (profileID == REDSTONE_KEY_ID) isHoldingRedstoneKey = true
            return
        }
        if (state.block === Blocks.redstone_block) {
            blocksDone.add(pos)
        }
    }

    fun clear(){
        blocksDone.clear()
        isHoldingRedstoneKey = false
    }

}