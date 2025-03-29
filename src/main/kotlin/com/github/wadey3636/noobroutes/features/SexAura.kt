package com.github.wadey3636.noobroutes.features

import com.github.wadey3636.noobroutes.utils.ClientUtils
import com.github.wadey3636.noobroutes.utils.PacketUtils
import me.defnotstolen.features.Category
import me.defnotstolen.features.Module
import me.defnotstolen.features.settings.impl.BooleanSetting
import me.defnotstolen.features.settings.impl.NumberSetting
import me.defnotstolen.utils.skyblock.modMessage
import me.defnotstolen.utils.skyblock.sendCommand
import net.minecraft.entity.item.EntityArmorStand
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.play.client.C01PacketChatMessage
import net.minecraftforge.event.world.WorldEvent
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent
import net.minecraftforge.fml.common.gameevent.TickEvent
import net.minecraftforge.fml.common.gameevent.TickEvent.ClientTickEvent
import org.lwjgl.input.Keyboard

object SexAura: Module(
    name = "Sex Aura",
    Keyboard.KEY_NONE,
    category = Category.MISC,
    description = "picks up them hoes"
) {
    private val pickupRange by NumberSetting(name = "range", description = "how far the bitches can be away", min = 0, max = 15, default = 5)
    private val actuallySend by BooleanSetting("Im ready", false, description = "wether to actually start the rizz")
    private val sentList = mutableListOf<String>()
    private var lastMessage = System.currentTimeMillis()

    private val pickupLines = listOf(
        "Are you WiFi? Because I’m feeling a strong connection… even though you’re clearly out of my league.",
        "Are you an earthquake? Because every time you’re near, my standards drop lower.",
        "You must be a magician, because whenever you leave, my happiness disappears.",
        "Are you a bank loan? Because you have my interest, and I know I’ll never be able to afford you.",
        "I’d say ‘God bless you,’ but it looks like he already did… and forgot about me.",
        "Are you a parking ticket? Because you got ‘FINE’ written all over you, and I deserve this punishment.",
        "You could be 0% interested in me, and I'd still screenshot our conversation for memories.",
        "Are you a college acceptance letter? Because I’ve been waiting forever, and I know I’m getting rejected.",
        "I’d say I miss you, but you never even gave me a chance to be with you in the first place.",
        "Are you my phone battery? Because every time you leave, my life gets dimmer.",
        "Are you my therapist? Because I keep telling myself I don’t need you, but deep down, I know I do.",
        "I’m not a photographer, but I can picture us… with you looking great and me looking lucky.",
        "You remind me of my alarm clock… I keep wanting to ignore you, but I know you’re the only thing that can save me.",
        "Are you a star? Because I stay up all night thinking about you, knowing I'll never reach you.",
        "You must be a notification I’ve been waiting for, because every time I see my phone light up, I pray it’s you.",
        "Are you a mosquito? Because even though you suck, I still want you all over me.",
        "Are you my last two brain cells? Because every time I see you, I lose control of my thoughts.",
        "Are you an unpaid internship? Because no matter how much effort I put in, I’m getting absolutely nothing in return.",
        "You could run me over with your car, and I’d still apologize for being in the way.",
        "Are you my favorite hoodie? Because I’d wear you every day, and you’d eventually get sick of me and leave.",
        "Are you a toxic ex? Because I know you're bad for me, but I still want you so bad.",
        "Are you my sleep schedule? Because I keep losing you, and it’s ruining my life.",
        "You could leave me on read for a year, and I'd still reply within 0.2 seconds.",
        "Are you my therapist? Because I only see you once a week, and I cry when you leave.",
        "You could tell me you only like tall, rich, emotionally stable people, and I'd still shoot my shot.",
        "Are you my mom? Because I desperately seek your approval, and you never give it to me.",
        "If I had a dollar for every time I thought about you, I’d still be broke because you never cross my mind… SIKE, I’m down horrendous for you.",
        "You could hand me a restraining order, and I'd frame it like a love letter.",
        "Are you my Uber Eats driver? Because I watch your every move, wai4ting for you to arrive, and I cry when you’re gone.",
        "Are you a mirage? Because every time I think I have a chance with you, I realize it was just my thirst hallucinating.",
        "Heet je noor? want je ogen zijn net zo mooi als het noor-derlicht"
    )

    @SubscribeEvent
    fun onTick(event: ClientTickEvent) {
        if (event.phase != TickEvent.Phase.START) return
        if (mc.theWorld == null || mc.thePlayer == null) return
        if (System.currentTimeMillis() - lastMessage < 1000) return
        getPlayersInRenderDistance().sortedBy {mc.thePlayer.getDistanceToEntity(it)}.forEach { player ->
            if (System.currentTimeMillis() - lastMessage < 1000) return
            sendPickupLine(player)
        }

    }

    private fun sendPickupLine(player: EntityPlayer) {
        if (actuallySend) sendCommand("msg ${player.name} ${pickupLines.random()}")
        else modMessage("/msg ${player.name} ${pickupLines.random()}")
        sentList.add(player.name)
        ClientUtils.clientScheduleTask(200) { sentList.remove(player.name) }
        lastMessage = System.currentTimeMillis()
    }

    fun pickupLineByName(args: Array<out String>) {
        if (args.size < 2) {
            modMessage("u gotta put in a name")
            return
        }
        //sendCommand("msg ${args[1]} ${pickupLines.random()}")
        PacketUtils.sendPacket(C01PacketChatMessage("/msg ${args[1]} ${pickupLines.random()}"))
    }

    private fun getPlayersInRenderDistance(): List<EntityPlayer> {
        return mc.theWorld?.loadedEntityList?.filterIsInstance<EntityPlayer>()?.filter {player ->
                player != mc.thePlayer &&
                !player.isInvisible &&
                mc.thePlayer.getDistanceToEntity(player) < pickupRange &&
                !sentList.contains(player.name) &&
                        mc.theWorld?.loadedEntityList?.any { it is EntityArmorStand && it.posX == player.posX && it.posZ == player.posZ } == false //idk why its indented
                }.orEmpty()
    }

    @SubscribeEvent
    fun onLoad(event: WorldEvent.Load) {
        lastMessage = System.currentTimeMillis() + 1000
    }
}