package me.noobmodcore.events.impl

import net.minecraftforge.fml.common.eventhandler.Cancelable
import net.minecraftforge.fml.common.eventhandler.Event

/**
 * Chat packet without formatting.
 * @see me.noobmodcore.events.EventDispatcher
 */
@Cancelable
data class ChatPacketEvent(val message: String) : Event()

@Cancelable
data class MessageSentEvent(val message: String) : Event()