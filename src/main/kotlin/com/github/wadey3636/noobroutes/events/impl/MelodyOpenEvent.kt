package com.github.wadey3636.noobroutes.events.impl

import net.minecraft.network.play.server.S2DPacketOpenWindow
import net.minecraftforge.fml.common.eventhandler.Event

class MelodyOpenEvent(val packet: S2DPacketOpenWindow): Event()