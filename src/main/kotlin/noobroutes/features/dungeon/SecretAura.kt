package noobroutes.features.dungeon

import net.minecraft.block.Block
import net.minecraft.init.Blocks
import net.minecraft.util.AxisAlignedBB
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.events.BossEventDispatcher
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.DropdownSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.*
import noobroutes.utils.Utils.isNotEnd
object SecretAura : Module("Secret Aura", category = Category.DUNGEON, description = "") {
    val enableOutsideOfDungeons by BooleanSetting("Enable Everywhere", description = "Enables the Aura outside of dungeons.")
    private val disableInBoss by BooleanSetting("Disable In Boss", description = "")
    private val rangeDropDown by DropdownSetting("Range Settings")
    private val chestRange by NumberSetting("Range", 6.2, 4.0, 6.5, 0.1, description = "Range for Blocks inside dungeons.").withDependency { rangeDropDown }
    private val essenceRange by NumberSetting("Skull Range", 4.5, 2.0, 5.0, 0.1, description = "Range for Skulls inside dungeons.").withDependency { rangeDropDown }
    private val bossRange by NumberSetting("Boss Range", 6.0, 4.0, 6.5, 0.1, description = "Range for Levers inside F7 Boss.").withDependency { rangeDropDown && !disableInBoss}
    private val clickCooldown by NumberSetting("Click Cooldown", 400L, 0L, 1000L, 1L, unit = "ms", description = "Click Cooldown for blocks.in milliseconds")


    private const val REDSTONE_KEY_ID = "fed95410-aba1-39df-9b95-1d4f361eb66e"
    private const val WITHER_ESSENCE_ID = "e0f3e929-869e-3dca-9504-54c666ee6f23"

    private var isHoldingRedstoneKey = true
    private val clickedBlocks: BlockPosCache = BlockPosCache()

    private data class BlockDistance(val block: Block, val pos: BlockPos, val distanceSq: Double)
    private var lastClick: Long = -1L



    @SubscribeEvent
    fun onWorldUnload(event: WorldEvent.Unload) {
        clickedBlocks.clear()
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        clickedBlocks.clear()
    }

    @SubscribeEvent
    fun onPostTick(event: ClientTickEvent) {
        if (event.isNotEnd || mc.thePlayer == null || mc.theWorld == null || System.currentTimeMillis() - lastClick < clickCooldown || mc.currentScreen != null || (disableInBoss && BossEventDispatcher.inBoss)) return

        var blockCandidate = BlockDistance(Blocks.air, BlockPos(Int.MAX_VALUE, 69, Int.MIN_VALUE), Double.POSITIVE_INFINITY)

        val eyePos = mc.thePlayer.getPositionEyes(0f)

        val expandRange = if (BossEventDispatcher.inBoss) bossRange else chestRange
        val box: AxisAlignedBB = AxisAlignedBB(eyePos.xCoord, eyePos.yCoord, eyePos.zCoord, eyePos.xCoord, eyePos.yCoord, eyePos.zCoord).expand(expandRange, expandRange, expandRange)

        val sqEssence = essenceRange * essenceRange
        val sqChest = chestRange * chestRange

        for (pos in getBlockPosWithinAABB(box)) {
            if (clickedBlocks.contains(pos)) continue

            val currentBlock = mc.theWorld.getBlockState(pos).block
            if (!isValidBlock(currentBlock, pos)) continue
            val currentDistanceSq = eyePos.squareDistanceTo(Vec3(pos.x + 0.5, pos.y + 0.5, pos.z + 0.5))

            if ((currentBlock == Blocks.skull && currentDistanceSq > sqEssence) || currentDistanceSq > sqChest) continue

            if (currentDistanceSq < blockCandidate.distanceSq) {
                blockCandidate = BlockDistance(currentBlock, pos, currentDistanceSq)
            }
        }

        if (blockCandidate.block == Blocks.air) return

        lastClick = System.currentTimeMillis()
        clickedBlocks.add(blockCandidate.pos)
        AuraManager.auraBlock(blockCandidate.pos)
    }

    private fun isValidBlock(block: Block, position: BlockPos): Boolean {
        return when (block) {
            Blocks.air -> false // Prob save time on average
            Blocks.chest -> true
            Blocks.lever -> true
            Blocks.redstone_block -> isHoldingRedstoneKey
            Blocks.skull -> {
                val skull = getSkull(position) ?: return false
                skull.skullTexture == REDSTONE_KEY_ID || skull.skullTexture == WITHER_ESSENCE_ID
            }
            else -> false
        }
    }

    fun clear(){
        clickedBlocks.clear()
        isHoldingRedstoneKey = false
    }

}