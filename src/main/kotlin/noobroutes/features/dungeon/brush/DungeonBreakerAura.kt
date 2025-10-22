package noobroutes.features.dungeon.brush

import net.minecraft.block.Block
import net.minecraft.client.Minecraft
import net.minecraft.init.Blocks
import net.minecraft.init.Items
import net.minecraft.item.Item
import net.minecraft.network.play.client.C07PacketPlayerDigging
import net.minecraft.network.play.client.C0APacketAnimation
import net.minecraft.util.BlockPos
import net.minecraft.util.EnumFacing
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import noobroutes.Core
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.settings.impl.BooleanSetting
import noobroutes.features.settings.impl.NumberSetting
import noobroutes.features.settings.impl.SelectorSetting
import noobroutes.utils.SwapManager
import noobroutes.utils.SwapManager.SwapState
import noobroutes.utils.Utils.isEnd
import noobroutes.utils.hash
import noobroutes.utils.skyblock.*
import noobroutes.utils.skyblock.PlayerUtils.distanceToPlayerSq


object DungeonBreakerAura : Module("Dungeon Breaker Aura", description = "Dungeon breaker extras.", category = Category.DUNGEON) {
    private var delay by NumberSetting<Int>("Delay", 1, 1, 20, 1, description = "Delay between block breaks")
    private var range by NumberSetting<Float>("Range", 5.7f, 0.1f, 5.7f, 0.1f, description = "Aura range")

    private var instabreak by BooleanSetting("Zero Ping", true, description = "Insta breaks all blocks")
    private var forceHypixel by BooleanSetting("Force Hypixel", false, description = "Forces hypixel and dungeons")
    private var sorting by SelectorSetting("Sorting", "distance", arrayListOf("distance", "fastest"), description = "Forces hypixel and dungeons")

    // Dont remove air from the fucking black list you will get banned
    private val blackList: HashSet<Block> = hashSetOf(Blocks.bedrock, Blocks.chest, Blocks.skull, Blocks.hopper, Blocks.redstone_torch, Blocks.anvil, Blocks.cauldron, Blocks.double_plant, Blocks.barrier, Blocks.command_block, Blocks.barrier, Blocks.trapdoor, Blocks.stone_button, Blocks.wooden_button, Blocks.air, Blocks.lava, Blocks.water)
    private val DISTANCE = "distance".hashCode();
    private val FASTEST = "fastest".hashCode();
    private val DUNGEONBREAKER = "DUNGEONBREAKER".hashCode();
    private val CHARGE_REGEX = Regex("""^Charges: (.+)/20$""");

    private val awaiting: HashMap<Int, Long> = hashMapOf()
    private var lastBreak: Int = 0;

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (event.isEnd || Minecraft.getMinecraft().thePlayer == null || Minecraft.getMinecraft().theWorld == null || (!forceHypixel && mc.isSingleplayer)) return;
        if (++lastBreak < delay) return


        var bestBlock: BlockPos? = null;
        BrushModule.findBest({ pos -> if ((awaiting.contains(pos.hash()) && awaiting[pos.hash()]!! > (System.currentTimeMillis() - 500)) || blackList.contains(Minecraft.getMinecraft().theWorld.getBlockState(pos).block)) -1.0 else pos.distanceToPlayerSq }, { pos -> bestBlock = pos})
        if (bestBlock == null) return;

        //devMessage("Pos : " + bestBlock!!)
        //devMessage("dist : " + bestBlock!!.distanceSq(mc.thePlayer.posX, mc.thePlayer.posY + (if (mc.thePlayer.isSneaking) PlayerUtils.SNEAK_EYE_HEIGHT else PlayerUtils.STAND_EYE_HEIGHT), mc.thePlayer.posZ))
        if (bestBlock!!.distanceSq(mc.thePlayer.posX, mc.thePlayer.posY + (if (mc.thePlayer.isSneaking) PlayerUtils.SNEAK_EYE_HEIGHT else PlayerUtils.STAND_EYE_HEIGHT), mc.thePlayer.posZ) > range * range) return// devMessage("Fail range check!")
        //devMessage("Valid block!")

        val slot = getDungeonBreakerSlot();
        if (slot < 0 || slot > 8) return

        if (!mc.isSingleplayer) {
            val lore = mc.thePlayer.inventory.getStackInSlot(slot).lore;
            for (l in lore) {
                if (!l.contains("Charges")) continue;

                val match = CHARGE_REGEX.find(l)?: return modMessage("Failed to find charge regex, report this bug!")
                if ((match.toString().toIntOrNull() ?: 0) < 1) return
            }
        }

        val state = SwapManager.swapToSlot(slot)
        if (state != SwapState.SWAPPED && state != SwapState.ALREADY_HELD) return

        lastBreak = 0;
        awaiting[bestBlock!!.hash()] = System.currentTimeMillis()
        //Minecraft.getMinecraft().theWorld.setBlockToAir(bestBlock)

        // Fix for packet order
        Minecraft.getMinecraft().netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.START_DESTROY_BLOCK, bestBlock, EnumFacing.UP))
        Minecraft.getMinecraft().netHandler.addToSendQueue(C0APacketAnimation())
        Minecraft.getMinecraft().netHandler.addToSendQueue(C07PacketPlayerDigging(C07PacketPlayerDigging.Action.ABORT_DESTROY_BLOCK, bestBlock, EnumFacing.UP))
    }

    // Receive packet event, remove from await, maybe dont even need to actually?

    private fun getDungeonBreakerSlot() : Int {
        for (i in 0..8) {
            val stack = Core.mc.thePlayer.inventory.getStackInSlot(i) ?: continue
            if (stack.skyblockID.hashCode() == DUNGEONBREAKER) return i;
            if (mc.isSingleplayer && forceHypixel && stack.item == Items.diamond_pickaxe) return i;
        }
        return -1;
    }
    private fun isHoldingDungeonBreaker(): Boolean {
        return mc.thePlayer?.heldItem?.item == Items.diamond_pickaxe && (mc.thePlayer?.heldItem?.skyblockID.hashCode() == DUNGEONBREAKER || (forceHypixel && mc.isSingleplayer));
    }

    @SubscribeEvent
    fun onWorldLoad(event: WorldEvent.Load) {
        awaiting.clear();
    }

    // Skidded from thea
    @SubscribeEvent
    fun onBreakSpeed(event: BreakSpeed) {
        if (event.entity.entityId != mc.thePlayer?.entityId || !isHoldingDungeonBreaker() || !LocationUtils.currentArea.isArea(Island.Dungeon) || !instabreak) return
        event.newSpeed = if (blackList.contains(event.state.block)) 0f else 1500f
    }
}