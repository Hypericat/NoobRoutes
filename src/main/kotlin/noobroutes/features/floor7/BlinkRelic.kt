package noobroutes.features.floor7

import net.minecraft.entity.Entity
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.network.play.client.C02PacketUseEntity
import net.minecraft.network.play.client.C03PacketPlayer.C04PacketPlayerPosition
import net.minecraft.util.BlockPos
import net.minecraft.util.Vec3
import net.minecraftforge.client.event.RenderWorldLastEvent
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import noobroutes.events.BossEventDispatcher.inBoss
import noobroutes.events.impl.ServerTickEvent
import noobroutes.features.Category
import noobroutes.features.Module
import noobroutes.features.floor7.autop3.Blink
import noobroutes.utils.AuraManager
import noobroutes.utils.PacketUtils
import noobroutes.utils.Scheduler
import noobroutes.utils.SwapManager
import noobroutes.utils.render.Color
import noobroutes.utils.render.RenderUtils
import noobroutes.utils.skyblock.modMessage
import noobroutes.utils.skyblock.skyblockID
import noobroutes.utils.toAABB
import org.lwjgl.input.Keyboard

object BlinkRelic: Module(
    name = "Blink Relics",
    Keyboard.KEY_NONE,
    category = Category.FLOOR7,
    description = "blink boom relic done"
) {
    private val orangePackets = listOf(
        C04PacketPlayerPosition(89.49532717466354, 6.0, 55.435900926589966, true),
        C04PacketPlayerPosition(88.59915296836067, 6.0625, 55.02760376077882, true),
        C04PacketPlayerPosition(87.53016898146899, 6.0625, 54.54057442086289, true),
        C04PacketPlayerPosition(86.36683084341635, 6.0, 54.01055730889261, true),
        C04PacketPlayerPosition(85.15197533284594, 6.0, 53.45706887065439, true),
        C04PacketPlayerPosition(83.90899133361363, 6.0, 52.89076508678534, true),
        C04PacketPlayerPosition(82.65064917778804, 6.0, 52.31746412338913, true),
        C04PacketPlayerPosition(81.3839214674885, 6.0, 51.74034269952732, true),
        C04PacketPlayerPosition(80.11261524391439, 6.0, 51.161135304009, true),
        C04PacketPlayerPosition(78.838809151802, 6.0, 50.580788967833946, true),
        C04PacketPlayerPosition(77.56363813130916, 6.0, 49.99982076998808, true),
        C04PacketPlayerPosition(76.28772185983404, 6.0, 49.418513035630504, true),
        C04PacketPlayerPosition(75.01139868127532, 6.0, 48.83701991431601, true),
        C04PacketPlayerPosition(73.73485333142317, 6.0, 48.25542557171128, true),
        C04PacketPlayerPosition(72.45818667603069, 6.0, 47.67377596227566, true),
        C04PacketPlayerPosition(71.18145378780551, 6.0, 47.09209617714687, true),
        C04PacketPlayerPosition(69.90468473644947, 6.0, 46.5103999160877, true),
        C04PacketPlayerPosition(68.62789594002169, 6.0, 45.928694659169494, true),
        C04PacketPlayerPosition(67.3510963627835, 6.0, 45.34698449051168, true),
        C04PacketPlayerPosition(66.07429089922212, 6.0, 44.765271640043736, true),
        C04PacketPlayerPosition(64.79748222172792, 6.0, 44.183557325307284, true),
        C04PacketPlayerPosition(63.52067178942619, 6.0, 43.60184221108014, true),
        C04PacketPlayerPosition(62.243860398999445, 6.0, 43.02012666033102, true),
        C04PacketPlayerPosition(60.96704848543637, 6.0, 42.43841087124088, true),
        C04PacketPlayerPosition(59.69023628624084, 6.0, 41.856694952016525, true),
        C04PacketPlayerPosition(58.41342393108996, 6.0, 41.27497896173888, true)
    )

    private val redPackets = listOf(
        C04PacketPlayerPosition(23.485512018203735, 6.0, 57.397353172302246, true),
        C04PacketPlayerPosition(24.352063615464257, 6.0, 56.92946117591761, true),
        C04PacketPlayerPosition(25.38571284880744, 6.0, 56.37134530044137, true),
        C04PacketPlayerPosition(26.51059740204898, 6.0, 55.763967181259176, true),
        C04PacketPlayerPosition(27.685296445741105, 6.0, 55.12969187388936, true),
        C04PacketPlayerPosition(28.887194204378453, 6.0, 54.48073070006328, true),
        C04PacketPlayerPosition(30.103942463100815, 5.921599998474121, 53.823751042220735, false),
        C04PacketPlayerPosition(30.790707572238603, 5.766367993957519, 53.45293423265625, false),
        C04PacketPlayerPosition(31.438084322275124, 5.535840625044555, 53.10338505151027, false),
        C04PacketPlayerPosition(32.049617664496495, 5.231523797587011, 52.773189412782905, false),
        C04PacketPlayerPosition(32.628533504666045, 5.0, 52.46060549816404, true),
        C04PacketPlayerPosition(33.71585894068581, 5.0, 51.873507311886215, true),
        C04PacketPlayerPosition(34.870050703992874, 5.0, 51.250304849168316, true),
        C04PacketPlayerPosition(36.06075148623942, 5.0, 50.60738944922443, true),
        C04PacketPlayerPosition(37.2713861951423, 5.0, 49.95371078430497, true),
        C04PacketPlayerPosition(38.492904829263736, 5.0, 49.29415537602625, true),
        C04PacketPlayerPosition(39.720366087244756, 5.0, 48.63139126550067, true),
        C04PacketPlayerPosition(40.951072018229986, 5.0, 47.96687520334481, true),
        C04PacketPlayerPosition(42.18354954088129, 5.0, 47.30140257548772, true),
        C04PacketPlayerPosition(43.416994352694616, 5.0, 46.63540766269709, true),
        C04PacketPlayerPosition(44.65096730445176, 5.0, 45.96912758229962, true),
        C04PacketPlayerPosition(45.88522862065172, 5.0, 45.30269180037073, true),
        C04PacketPlayerPosition(47.11964738385575, 5.0, 44.636171005395816, true)
    )

    private val onOrangeSpot get() = mc.thePlayer.posX in 90.0..90.7 && mc.thePlayer.posY == 6.0 && mc.thePlayer.posZ in 55.0..55.7
    private val onRedSpot get() = mc.thePlayer.posX in 22.3..23.0 && mc.thePlayer.posY == 6.0 && mc.thePlayer.posZ in 58.0..58.7

    private var currentRelic = Relic.None
    private var auraCooldown = false
    private var clicked = false

    @SubscribeEvent
    fun onRenderWorld(event: RenderWorldLastEvent) {
        drawRings()
        if (auraCooldown) return
        val armourStands = mc.theWorld?.loadedEntityList?.firstOrNull {
            it is EntityArmorStand &&
                    it.inventory?.get(4)?.displayName?.contains("Relic") == true &&
                    mc.thePlayer.getDistanceToEntity(it) < 4.5
        } ?: return

        if (currentRelic == Relic.None) return

        auraCooldown = true
        grabRelic(armourStands)

        if (currentRelic != Relic.None && !clicked) {
            clicked = true
            AuraManager.auraBlock(currentRelic.blockPos)
        }

        doBlink()

        Scheduler.schedulePreTickTask(20) {
            auraCooldown = false
        }
    }

    @SubscribeEvent
    fun onServerTick(event: ServerTickEvent) {
        currentRelic = Relic.entries.find {
            mc.thePlayer?.inventory?.mainInventory?.any { item ->
                item.skyblockID == it.skyblockID
            } == true
        } ?: Relic.None
    }

    private fun drawRings() {
        if (!inBoss) return
        RenderUtils.drawOutlinedAABB(Vec3(89.85, 6.0, 54.85).toAABB(), if (onOrangeSpot) Color.GREEN else Color.RED, thickness = 3, depth = true)
        RenderUtils.drawOutlinedAABB(Vec3(22.15, 6.0, 57.85).toAABB(), if (onRedSpot) Color.GREEN else Color.RED, thickness = 3, depth = true)
    }

    private fun grabRelic(entity: Entity) {
        val objectMouseOver = mc.objectMouseOver?.hitVec ?: return
        mc.netHandler.addToSendQueue(C02PacketUseEntity(entity, Vec3(objectMouseOver.xCoord - entity.posX, objectMouseOver.yCoord - entity.posY, objectMouseOver.zCoord - entity.posZ)))
    }

    private fun doBlink() {
        when {
            onOrangeSpot -> handleRelic(Blink.cancelled >= orangePackets.size, 111f, orangePackets)
            onRedSpot -> handleRelic(Blink.cancelled >= redPackets.size, -120f, redPackets)
        }
    }

    private fun handleRelic(
        isEnoughPackets: Boolean,
        yaw: Float,
        packets: List<C04PacketPlayerPosition>
    ) {
        SwapManager.swapToSlot(8)
        if (!isEnoughPackets) return
        modMessage("Blinking relic")
        Scheduler.schedulePreTickTask {
            packets.forEach { PacketUtils.sendPacket(it) }
            with(packets.last()) {mc.thePlayer.setPosition(positionX, positionY, positionZ)}
        }
    }

    enum class Relic(val skyblockID: String, val blockPos: BlockPos) {
        Red("RED_KING_RELIC", BlockPos(51.0, 7.0, 42.0)),
        Green("GREEN_KING_RELIC", BlockPos(49.0, 7.0, 44.0)),
        Purple("PURPLE_KING_RELIC", BlockPos(54.0, 7.0, 41.0)),
        Blue("BLUE_KING_RELIC", BlockPos(59.0, 7.0, 44.0)),
        Orange("ORANGE_KING_RELIC", BlockPos(57.0, 7.0, 42.0)),
        None("", BlockPos(0.0, 0.0, 0.0))
    }

    @SubscribeEvent
    fun onServerLoad(event: WorldEvent.Load) {clicked = false}
}