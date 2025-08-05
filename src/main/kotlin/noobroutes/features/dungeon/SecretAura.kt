package noobroutes.features.dungeon

import net.minecraft.block.Block
import net.minecraft.block.BlockChest
import net.minecraft.block.BlockLever
import net.minecraft.block.BlockSkull
import net.minecraft.init.Blocks
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import noobroutes.events.impl.MoveEntityWithHeadingEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.Setting.Companion.withDependency
import noobroutes.features.settings.impl.DropdownSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.utils.AuraManager
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.Utils.isNotEnd
import noobroutes.utils.Utils.isStart
import noobroutes.utils.ceil
import noobroutes.utils.ceilToInt
import noobroutes.utils.getSkull
import noobroutes.utils.render.RenderUtils.renderX
import noobroutes.utils.render.RenderUtils.renderY
import noobroutes.utils.skullTexture
import kotlin.math.pow
import kotlin.math.roundToInt

object SecretAura : Module("Secret Aura", category = Category.DUNGEON, description = "") {
    private val rangeDropDown by DropdownSetting("Range Settings")
    private val chestRange by NumberSetting("Range", 6.2, 4.0, 6.5, 0.1, description = "Range for Blocks inside dungeons.").withDependency { rangeDropDown }
    private val essenceRange by NumberSetting("Skull Range", 4.5, 2.0, 5.0, 0.1, description = "Range for Skulls inside dungeons.").withDependency { rangeDropDown }
    private val bossRange by NumberSetting("Boss Range", 6.0, 4.0, 6.5, 0.1, description = "Range for Levers inside F7 Boss.").withDependency { rangeDropDown }
    private val clickCooldown by NumberSetting("Click Cooldown", 400L, 0L, 1000L, 1L, unit = "ms", description = "Click Cooldown for blocks.in milliseconds")

    private var isHoldingRedstoneKey = true

    private val clickedBlocks: MutableSet<BlockPos> = mutableSetOf()

    private data class BlockDistance(val block: Block, val pos: BlockPos, val distanceSq: Double)
    private var lastClick = 0L

    @SubscribeEvent
    fun onPostTick(event: TickEvent.ClientTickEvent) {
        if (event.isNotEnd || mc.thePlayer == null || mc.theWorld == null || System.currentTimeMillis() - lastClick < clickCooldown || mc.currentScreen != null) return

        var blockCandidate = BlockDistance(Blocks.leaves2, BlockPos(Int.MAX_VALUE, 69, Int.MIN_VALUE), Double.POSITIVE_INFINITY)

        val eyePos = mc.thePlayer.getPositionEyes(0f)

        for (x in (eyePos.xCoord - chestRange.ceil()).toInt()..(eyePos.xCoord + chestRange.ceil()).toInt()) {
            for (y in (eyePos.yCoord - chestRange.ceil()).toInt()..(eyePos.yCoord + chestRange.ceil()).toInt()) {
                for (z in (eyePos.zCoord - chestRange.ceil()).toInt()..(eyePos.zCoord + chestRange.ceil()).toInt()) {

                    val pos = BlockPos(x, y, z)
                    if (clickedBlocks.contains(pos)) continue

                    val currentBlock = mc.theWorld.getBlockState(pos).block
                    if (!isValidBlock(currentBlock, pos)) continue

                    val currentDistanceSq = eyePos.squareDistanceTo(Vec3(x + 0.5, y + 0.5, z + 0.5))

                    if ((currentBlock == Blocks.skull && currentDistanceSq > essenceRange.pow(2)) || currentDistanceSq > chestRange.pow(2)) continue

                    if (currentDistanceSq < blockCandidate.distanceSq) {
                        blockCandidate = BlockDistance(currentBlock, pos, currentDistanceSq)
                    }
                }
            }
        }

        if (blockCandidate.block == Blocks.leaves2) return

        lastClick = System.currentTimeMillis()
        AuraManager.auraBlock(blockCandidate.pos)

    }

    private const val REDSTONE_KEY_ID = "fed95410-aba1-39df-9b95-1d4f361eb66e"
    private const val WITHER_ESSENCE_ID = "e0f3e929-869e-3dca-9504-54c666ee6f23"

    private fun isValidBlock(block: Block, position: BlockPos): Boolean {
        return when (block) {
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
}